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

    String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        var leftChild = binaryExpr.getChild(0);
        var rightChild = binaryExpr.getChild(1);

        TypeUtils.setCurrentMethod(currentMethod); // ?

        var leftType = TypeUtils.getExprType(leftChild, table);
        var rightType = TypeUtils.getExprType(rightChild, table);

        if (!leftType.isArray() && !rightType.isArray() && leftType.equals(rightType)) {
            return null;
        }

        // Create error report
        var message = "Invalid binary operation.";
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(binaryExpr),
                NodeUtils.getColumn(binaryExpr),
                message,
                null)
        );

        return null;
    }
}
