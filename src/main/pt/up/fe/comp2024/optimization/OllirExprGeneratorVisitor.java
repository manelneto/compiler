package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;

    TypeUtils typeUtils;


    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
        this.typeUtils = new TypeUtils("", table);
    }

    public void setCurrentMethod(String currentMethod) {
        typeUtils.setCurrentMethod(currentMethod);
    }

    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(FUNCTION_CALL, this::visitFunctionCall);
        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitFunctionCall(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder("invokestatic(");

        var className = typeUtils.getExprType(node.getChild(0)).getName();
        code.append(className);
        code.append(", \"");

        var functionName = node.get("name");
        code.append(functionName);
        code.append("\", ");

        for (int i = 1; i < node.getNumChildren(); i++) {
            code.append(visit(node.getJmmChild(i)).getCode());
        }

        code.append(")");

        if (node.getParent().getKind().equals(ASSIGN_STMT.toString())) {
            var retType = typeUtils.getExprType(node.getParent());
            code.append(OptUtils.toOllirType(retType));
        } else {
            code.append(".V");
        }

        return new OllirExprResult(code.toString());
    }

    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(typeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        Type resType = typeUtils.getExprType(node);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        Type type = typeUtils.getExprType(node);
        computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        var id = node.get("name");
        Type type = typeUtils.getExprType(node);
        String ollirType = OptUtils.toOllirType(type);

        String code = id + ollirType;

        return new OllirExprResult(code);
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

}
