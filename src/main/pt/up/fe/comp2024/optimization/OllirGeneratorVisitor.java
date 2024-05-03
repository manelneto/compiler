package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.List;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String ASSIGN = ":=";
    private static final String END_LABEL = ":\n";
    private static final String END_STMT = ";\n";
    private static final String NL = "\n";
    private static final String SPACE = " ";
    private static final String L_BRACKET = " {\n";
    private static final String R_BRACKET = "}\n";

    private final SymbolTable table;
    private final TypeUtils typeUtils;

    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        this.typeUtils = new TypeUtils("", table);
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {
        addVisit(PROGRAM, this::visitProgram);
        addVisit(IMPORT_DECL, this::visitImportDecl);
        addVisit(CLASS_DECL, this::visitClassDecl);
        addVisit(VAR_DECL, this::visitVarDecl);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(STMT_BLOCK, this::visitStmtBlock);
        addVisit(IF_ELSE_STMT, this::visitIfElseStmt);
        addVisit(WHILE_STMT, this::visitWhileStmt);
        addVisit(SIMPLE_STMT, this::visitSimpleStmt);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(ARRAY_ASSIGN_STMT, this::visitArrayAssignStmt);
        addVisit(RETURN_STMT, this::visitReturnStmt);
        setDefaultVisit(this::defaultVisit);
    }

    private String visitProgram(JmmNode program, Void unused) {
        StringBuilder code = new StringBuilder();

        program.getChildren().stream().map(this::visit).forEach(code::append);

        return code.toString();
    }

    private String visitImportDecl(JmmNode importDecl, Void unused) {
        StringBuilder code = new StringBuilder("import ");

        List<String> imports = importDecl.getObjectAsList("name", String.class);

        if (imports.size() > 1) {
            String fullName = imports.stream().reduce((a, e) -> a + "." + e).orElse("");
            code.append(fullName);
        } else {
            String name = imports.get(0);
            code.append(name);
        }

        code.append(END_STMT);

        return code.toString();
    }

    private String visitClassDecl(JmmNode classDecl, Void unused) {
        StringBuilder code = new StringBuilder();

        code.append(table.getClassName()).append(" extends ");

        if (NodeUtils.getBooleanAttribute(classDecl, "isSubclass", "false")) {
            code.append(table.getSuper());
        } else {
            code.append("Object");
        }

        code.append(L_BRACKET).append(NL);

        boolean needNl = true;
        for (JmmNode child : classDecl.getChildren()) {
            String result = visit(child);

            if (METHOD_DECL.check(child) && needNl) {
                code.append(NL);
                needNl = false;
            }

            code.append(result);
        }

        code.append(buildConstructor()).append(R_BRACKET).append(NL);

        return code.toString();
    }

    private String visitVarDecl(JmmNode varDecl, Void unused) {
        if (varDecl.getParent().getKind().equals(CLASS_DECL.toString())) {
            String name = varDecl.get("name");
            String type = OptUtils.toOllirType(varDecl.getChild(0));
            return ".field public " + name + type + END_STMT;
        }
        return "";
    }

    private String visitMethodDecl(JmmNode methodDecl, Void unused) {
        typeUtils.setCurrentMethod(methodDecl.get("name"));
        exprVisitor.setCurrentMethod(methodDecl.get("name"));

        StringBuilder code = new StringBuilder(".method ");

        if (NodeUtils.getBooleanAttribute(methodDecl, "isPublic", "false")) {
            code.append("public ");
        }

        boolean isVoid = NodeUtils.getBooleanAttribute(methodDecl, "isVoid", "false");

        if (isVoid) {
            code.append("static ");
        }

        List<JmmNode> params = methodDecl.getChildren(PARAM);
        int paramsNumber = params.size();
        if (paramsNumber > 0 && params.get(paramsNumber - 1).getChild(0).getObject("isEllipsis", Boolean.class)) {
            code.append("varargs ");
        }

        String name = methodDecl.get("name");
        code.append(name);

        if (isVoid) {
            String paramName = methodDecl.get("paramName");
            code.append("(").append(paramName).append(".array.String)");
        } else {
            code.append("(");
            for (JmmNode param : methodDecl.getChildren(PARAM)) {
                String paramCode = visit(param);
                code.append(paramCode).append(", ");
            }
            if (!methodDecl.getChildren(PARAM).isEmpty()) {
                code.delete(code.length() - 2, code.length());
            }
            code.append(")");
        }

        if (isVoid) {
            code.append(".V");
        } else {
            String retType = OptUtils.toOllirType(methodDecl.getJmmChild(0));
            code.append(retType);
        }

        code.append(L_BRACKET);

        int afterParam = methodDecl.getChildren(PARAM).size();
        if (!isVoid) {
            afterParam += 1;
        }
        for (int i = afterParam; i < methodDecl.getNumChildren(); i++) {
            JmmNode child = methodDecl.getJmmChild(i);
            String childCode = visit(child);
            code.append(childCode);
        }

        if (isVoid) {
            code.append("ret.V").append(END_STMT);
        }

        code.append(R_BRACKET).append(NL);

        return code.toString();
    }

    private String visitParam(JmmNode param, Void unused) {
        String type = OptUtils.toOllirType(param.getJmmChild(0));
        String id = param.get("paramName");

        return id + type;
    }

    private String visitStmtBlock(JmmNode stmtBlock, Void unused) {
        StringBuilder code = new StringBuilder();
        for (JmmNode child : stmtBlock.getChildren()) {
            code.append(visit(child));
        }
        return code.toString();
    }

    private String visitIfElseStmt(JmmNode ifElseStmt, Void unused) {
        StringBuilder code = new StringBuilder();
        OllirExprResult exprResult = exprVisitor.visit(ifElseStmt.getChild(0));
        String thenCode = visit(ifElseStmt.getChild(1));
        String elseCode = visit(ifElseStmt.getChild(2));
        String ifNumber = OptUtils.getIfNumber();

        code.append(exprResult.getComputation());
        code.append("if (").append(exprResult.getCode()).append(") goto if_then_").append(ifNumber).append(END_STMT);

        code.append(elseCode);
        code.append("goto if_end_").append(ifNumber).append(END_STMT);

        code.append("if_then_").append(ifNumber).append(END_LABEL);
        code.append(thenCode);
        code.append("if_end_").append(ifNumber).append(END_LABEL);

        return code.toString();
    }

    private String visitWhileStmt(JmmNode whileStmt, Void unused) {
        StringBuilder code = new StringBuilder();
        OllirExprResult exprResult = exprVisitor.visit(whileStmt.getChild(0));
        String stmtCode = visit(whileStmt.getChild(1));
        String whileNumber = OptUtils.getWhileNumber();

        code.append("goto while_cond_").append(whileNumber).append(END_STMT);
        code.append("while_body_").append(whileNumber).append(END_LABEL);
        code.append(stmtCode);

        code.append("while_cond_").append(whileNumber).append(END_LABEL);
        code.append(exprResult.getComputation());
        code.append("if (").append(exprResult.getCode()).append(") goto while_body_").append(whileNumber).append(END_STMT);

        return code.toString();
    }

    private String visitSimpleStmt(JmmNode simpleStmt, Void unused) {
        JmmNode child = simpleStmt.getChild(0);
        OllirExprResult result = exprVisitor.visit(child);
        return result.getComputation() + result.getCode() + END_STMT;
    }

    private String visitAssignStmt(JmmNode assignStmt, Void unused) {
        String lhs = assignStmt.get("name");
        String lhsType = OptUtils.toOllirType(typeUtils.getStmtType(assignStmt));
        OllirExprResult rhs = exprVisitor.visit(assignStmt.getJmmChild(0));

        StringBuilder code = new StringBuilder();

        if (typeUtils.isField(lhs)) {
            code.append("putfield(this, ");
            code.append(lhs).append(lhsType).append(", ");
            code.append(rhs.getCode()).append(").V").append(END_STMT);
        } else {
            // code to compute the children
            code.append(rhs.getComputation());
            code.append(lhs).append(lhsType);

            // code to compute self
            // statement has type of lhs
            Type thisType = typeUtils.getStmtType(assignStmt);
            String typeString = OptUtils.toOllirType(thisType);

            code.append(SPACE).append(ASSIGN).append(typeString).append(SPACE);

            code.append(rhs.getCode()).append(END_STMT);
        }
        return code.toString();
    }

    private String visitArrayAssignStmt(JmmNode arrayAssignStmt, Void unused) {
        StringBuilder code = new StringBuilder();
        String arrayName = arrayAssignStmt.get("name");

        OllirExprResult indexExprResult = exprVisitor.visit(arrayAssignStmt.getChild(0));
        OllirExprResult valueExprResult = exprVisitor.visit(arrayAssignStmt.getChild(1));

        code.append(indexExprResult.getComputation());
        code.append(valueExprResult.getComputation());
        code.append(arrayName).append("[").append(indexExprResult.getCode()).append("].i32 ");
        code.append(ASSIGN).append(".i32 ").append(valueExprResult.getCode()).append(END_STMT);

        return code.toString();
    }

    private String visitReturnStmt(JmmNode returnStmt, Void unused) {
        String methodName = returnStmt.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();

        OllirExprResult expr = OllirExprResult.EMPTY;
        if (returnStmt.getNumChildren() > 0) {
            expr = exprVisitor.visit(returnStmt.getJmmChild(0));
        }

        code.append(expr.getComputation());
        code.append("ret").append(OptUtils.toOllirType(retType)).append(SPACE);
        code.append(expr.getCode()).append(END_STMT);

        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     */
    private String defaultVisit(JmmNode node, Void unused) {
        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }

    private String buildConstructor() {
        return ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n";
    }
}
