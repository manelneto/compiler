package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.TypeUtils;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends AJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final TypeUtils typeUtils;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.typeUtils = new TypeUtils("", table);
    }

    public void setCurrentMethod(String currentMethod) {
        typeUtils.setCurrentMethod(currentMethod);
    }

    @Override
    protected void buildVisitor() {
        addVisit(PAREN_EXPR, this::visitParenExpr);
        //addVisit(ARRAY_ACCESS, this::visitArrayAccess);
        addVisit(FUNCTION_CALL, this::visitFunctionCall);
        //addVisit(LENGTH, this::visitLength);
        //addVisit(UNARY_EXPR, this::visitUnaryExpr);
        addVisit(NEW_OBJECT, this::visitNewObject);
        //addVisit(NEW_ARRAY, this::visitNewArray);
        addVisit(BINARY_EXPR, this::visitBinaryExpr);
        //addVisit(ARRAY, this::visitArray);
        addVisit(INTEGER_LITERAL, this::visitIntegerLiteral);
        addVisit(BOOLEAN_LITERAL, this::visitBooleanLiteral);
        addVisit(VAR_REF_EXPR, this::visitVarRefExpr);
        addVisit(THIS, this::visitThis);
        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitParenExpr(JmmNode parenExpr, Void unused) {
        JmmNode expr = parenExpr.getChild(0);
        return visit(expr);
    }

    private OllirExprResult visitFunctionCall(JmmNode functionCall, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        JmmNode child = functionCall.getChild(0);
        String type = OptUtils.toOllirType(typeUtils.getExprType(functionCall));

        OllirExprResult exprResult = visit(child);
        computation.append(exprResult.getComputation());

        StringBuilder argumentsCode = new StringBuilder();

        for (int i = 1; i < functionCall.getNumChildren(); i++) {
            OllirExprResult result = visit(functionCall.getJmmChild(i));
            computation.append(result.getComputation());
            argumentsCode.append(", " + result.getCode());
        }

        if (!functionCall.getParent().getKind().equals(SIMPLE_STMT.toString())) {
            code.append(OptUtils.getTemp());
            code.append(type);
            computation.append(code);
            computation.append(" " + ASSIGN);
            computation.append(type + " ");
        }

        if (child.getObject("isInstance", Boolean.class)) {
            computation.append("invokevirtual(");
        } else {
            computation.append("invokestatic(");
        }

        computation.append(exprResult.getCode());
        computation.append(", \"");
        computation.append(functionCall.get("name"));
        computation.append("\"");
        computation.append(argumentsCode);

        computation.append(")");
        computation.append(OptUtils.toOllirType(typeUtils.getExprType(functionCall)));

        if (!functionCall.getParent().getKind().equals(SIMPLE_STMT.toString())) {
            computation.append(END_STMT);
        }

        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitNewObject(JmmNode newObject, Void unused) {
        StringBuilder computation = new StringBuilder();
        String temp = OptUtils.getTemp();
        computation.append(temp);

        Type type = typeUtils.getExprType(newObject);
        String ollirType = OptUtils.toOllirType(type);
        computation.append(ollirType);

        computation.append(" :=");
        computation.append(ollirType);

        computation.append(" new(");
        computation.append(type.getName());
        computation.append(")");
        computation.append(ollirType);

        computation.append(END_STMT);
        computation.append("invokespecial(");
        computation.append(temp);
        computation.append(ollirType);
        computation.append(", \"<init>\").V");
        computation.append(END_STMT);

        StringBuilder code = new StringBuilder(temp);
        code.append(ollirType);

        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitBinaryExpr(JmmNode binaryExpr, Void unused) {
        var lhs = visit(binaryExpr.getJmmChild(0));
        var rhs = visit(binaryExpr.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        Type resType = typeUtils.getExprType(binaryExpr);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        Type type = typeUtils.getExprType(binaryExpr);
        computation.append(binaryExpr.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitIntegerLiteral(JmmNode integerLiteral, Void unused) {
        Type intType = new Type(typeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = integerLiteral.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitBooleanLiteral(JmmNode booleanLiteral, Void unused) {
        Type boolType = new Type(typeUtils.getBooleanTypeName(), false);
        String ollirBoolType = OptUtils.toOllirType(boolType);
        String value = booleanLiteral.get("value");
        if (value.equals("true")) {
            return new OllirExprResult(1 + ollirBoolType);
        }
        return new OllirExprResult(0 + ollirBoolType);
    }

    private OllirExprResult visitVarRefExpr(JmmNode varRefExpr, Void unused) {
        String name = varRefExpr.get("name");
        Type type = typeUtils.getExprType(varRefExpr);
        String ollirType = OptUtils.toOllirType(type);

        if (typeUtils.isImport(name)) {
            return new OllirExprResult(name);
        }

        if (typeUtils.isLocal(name) || typeUtils.isParameter(name)) {
            String code = name + ollirType;
            return new OllirExprResult(code);
        }

        assert typeUtils.isField(name);

        StringBuilder code = new StringBuilder();
        code.append(OptUtils.getTemp());
        code.append(ollirType);

        StringBuilder computation = new StringBuilder();
        computation.append(code);
        computation.append(" " + ASSIGN);
        computation.append(ollirType);
        computation.append(" getfield(this, ");
        computation.append(name);
        computation.append(ollirType);
        computation.append(")");
        computation.append(ollirType);
        computation.append(END_STMT);

        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitThis(JmmNode this_, Void unused) {
        String name = this_.get("name"); // this
        return new OllirExprResult(name);
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

}
