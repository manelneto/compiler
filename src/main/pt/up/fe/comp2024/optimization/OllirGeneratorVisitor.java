package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.Ollir;
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

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";


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
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(VAR_DECL, this::visitVarDecl);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(SIMPLE_STMT, this::visitSimpleStmt);
        addVisit(WHILE_STMT, this::visitWhileStmt);
        addVisit(IF_ELSE_STMT, this::visitIfElseStmt);
        addVisit(STMT_BLOCK, this::visitStmtBlock);
        addVisit(ARRAY_ASSIGN_STMT, this::visitArrayAssignStmt);


        setDefaultVisit(this::defaultVisit);
    }


    private String visitStmtBlock(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        for (JmmNode child : node.getChildren()) {
            code.append(visit(child));
        }
        return code.toString();
    }

    private String visitSimpleStmt(JmmNode node, Void unused) {
        JmmNode child = node.getChild(0);
        OllirExprResult result = exprVisitor.visit(child);
        return result.getComputation() + result.getCode() + END_STMT;
    }

    private String visitAssignStmt(JmmNode node, Void unused) {
        var lhs = node.get("name");
        var lhsType = OptUtils.toOllirType(typeUtils.getStmtType(node));
        var rhs = exprVisitor.visit(node.getJmmChild(0));

        StringBuilder code = new StringBuilder();

        if (typeUtils.isField(lhs)) {
            code.append("putfield(this, ");
            code.append(lhs);
            code.append(lhsType);
            code.append(", ");
            code.append(rhs.getCode());
            code.append(").V");
            code.append(END_STMT);
        } else {
            // code to compute the children
            code.append(rhs.getComputation());
            code.append(lhs);
            code.append(lhsType);

            // code to compute self
            // statement has type of lhs
            Type thisType = typeUtils.getStmtType(node);
            String typeString = OptUtils.toOllirType(thisType);

            code.append(SPACE);

            code.append(ASSIGN);
            code.append(typeString);
            code.append(SPACE);

            code.append(rhs.getCode());

            code.append(END_STMT);
        }
        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {
        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();

        var expr = OllirExprResult.EMPTY;

        if (node.getNumChildren() > 0) {
            expr = exprVisitor.visit(node.getJmmChild(0));
        }

        code.append(expr.getComputation());
        code.append("ret");
        code.append(OptUtils.toOllirType(retType));
        code.append(SPACE);

        code.append(expr.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitParam(JmmNode node, Void unused) {
        var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        var id = node.get("paramName");

        return id + typeCode;
    }


    private String visitMethodDecl(JmmNode node, Void unused) {
        typeUtils.setCurrentMethod(node.get("name"));
        exprVisitor.setCurrentMethod(node.get("name"));

        StringBuilder code = new StringBuilder(".method ");

        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");

        if (isPublic) {
            code.append("public ");
        }

        boolean isVoid = NodeUtils.getBooleanAttribute(node, "isVoid", "false");

        if (isVoid) {
            code.append("static ");
        }

        // name
        var name = node.get("name");
        code.append(name);

        // param
        if (isVoid) {
            code.append("(args.array.String)");
        } else {
            code.append("(");
            for (JmmNode param : node.getChildren(PARAM)) {
                var paramCode = visit(param);
                code.append(paramCode);
                code.append(", ");
            }
            if (!node.getChildren(PARAM).isEmpty()) {
                code.delete(code.length() - 2, code.length());
            }
            code.append(")");
        }

        // type
        if (isVoid) {
            code.append(".V");
        } else {
            var retType = OptUtils.toOllirType(node.getJmmChild(0));
            code.append(retType);
        }

        code.append(L_BRACKET);

        // rest of its children stmts
        int afterParam = node.getChildren(PARAM).size();
        if (!isVoid) {
            afterParam += 1;
        }
        for (int i = afterParam; i < node.getNumChildren(); i++) {
            var child = node.getJmmChild(i);
            var childCode = visit(child);
            code.append(childCode);
        }

        if (isVoid) {
            code.append("ret.V");
            code.append(END_STMT);
        }

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }


    private String visitClass(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        code.append(table.getClassName());

        boolean isSubclass = NodeUtils.getBooleanAttribute(node, "isSubclass", "false");

        code.append(" extends ");
        if (isSubclass) {
            code.append(table.getSuper());
        } else {
            code.append("Object");
        }

        code.append(L_BRACKET);

        code.append(NL);
        var needNl = true;

        for (var child : node.getChildren()) {
            var result = visit(child);

            if (METHOD_DECL.check(child) && needNl) {
                code.append(NL);
                needNl = false;
            }

            code.append(result);
        }

        code.append(buildConstructor());
        code.append(R_BRACKET);

        return code.toString();
    }

    private String buildConstructor() {
        return ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n";
    }


    private String visitProgram(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }

    private String visitImportDecl(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder("import ");

        List<String> tempImport = node.getObjectAsList("name", String.class);

        if (tempImport.size() > 1) {
            String fullName = tempImport.stream().reduce((a, e) -> a + "." + e).orElse("");
            code.append(fullName);
        } else {
            String name = tempImport.get(0);
            code.append(name);
        }

        code.append(END_STMT);

        return code.toString();
    }

    private String visitVarDecl(JmmNode node, Void unused) {
        if (node.getParent().getKind().equals(CLASS_DECL.toString())) {
            StringBuilder code = new StringBuilder(".field public ");

            var name = node.get("name");
            code.append(name);

            var type = OptUtils.toOllirType(node.getChild(0));
            code.append(type);

            code.append(END_STMT);

            return code.toString();
        }
        return "";
    }

    private String visitWhileStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        OllirExprResult exprResult = exprVisitor.visit(node.getChild(0));
        String stmtResult = visit(node.getChild(1));
        String whileNumber = OptUtils.getWhileNumber();

        code.append("goto while_cond_").append(whileNumber).append(END_STMT);
        code.append("while_body_").append(whileNumber).append(":").append("\n");
        code.append(stmtResult);

        code.append("while_cond_").append(whileNumber).append(":").append("\n");
        code.append(exprResult.getComputation());
        code.append("if (").append(exprResult.getCode()).append(") goto while_body_").append(whileNumber).append(END_STMT);

        return code.toString();
    }

    private String visitIfElseStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        OllirExprResult exprResult = exprVisitor.visit(node.getChild(0));
        String thenStmtResult = visit(node.getChild(1));
        String elseStmtResult = visit(node.getChild(2));
        String ifNumber = OptUtils.getIfNumber();

        code.append(exprResult.getComputation());
        code.append("if(").append(exprResult.getCode()).append(") goto if_then_").append(ifNumber).append(END_STMT);

        code.append(elseStmtResult);
        code.append("goto if_end_").append(ifNumber).append(END_STMT);

        code.append("if_then_").append(ifNumber).append(":\n");
        code.append(thenStmtResult);
        code.append("if_end_").append(ifNumber).append(":\n");

        return code.toString();
    }

    private String visitArrayAssignStmt (JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        String arrayName = node.get("name");

        OllirExprResult indexExprResult = exprVisitor.visit(node.getChild(0));
        OllirExprResult valueExprResult = exprVisitor.visit(node.getChild(1));

        code.append(indexExprResult.getComputation());
        code.append(valueExprResult.getComputation());
        code.append(arrayName).append("[").append(indexExprResult.getCode()).append("].i32 ") // TODO: o tipo está hardcoded para int
                .append(ASSIGN).append(".i32 ").append(valueExprResult.getCode()).append(END_STMT);

        return code.toString();
    }


    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {
        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}
