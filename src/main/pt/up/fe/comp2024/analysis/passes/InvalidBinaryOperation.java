package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;

public class InvalidBinaryOperation extends AnalysisVisitor {
    private TypeUtils typeUtils;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        typeUtils = new TypeUtils(method.get("name"), table);
        return null;
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        var leftChild = binaryExpr.getChild(0);
        var rightChild = binaryExpr.getChild(1);

        var leftType = typeUtils.getExprType(leftChild);
        var rightType = typeUtils.getExprType(rightChild);

        if (!leftType.isArray() && !rightType.isArray() && leftType.equals(rightType)) {
            return null;
        }

        reportError("Invalid binary operation", binaryExpr);

        return null;
    }
}
