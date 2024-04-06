package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

public class InvalidBinaryOperation extends AnalysisVisitor {

    private String currentMethod;
    private SymbolTable table;
    TypeUtils typeUtils = new TypeUtils("", table);

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        typeUtils.setCurrentMethod(currentMethod);
        return null;
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {

        typeUtils.setTable(table);

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

    private void reportError(String message, JmmNode node) {

        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(node),
                NodeUtils.getColumn(node),
                message,
                null)
        );
    }
}
