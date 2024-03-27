package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

public class IncompatibleAssignment extends AnalysisVisitor {
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(Kind.ARRAY_ASSIGN_STMT, this::visitAssignStmt);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        TypeUtils.setCurrentMethod(currentMethod); // ?
        return null;
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        JmmNode assignValue = assignStmt.getChild(assignStmt.getChildren().size() - 1);

        var varType = TypeUtils.getExprType(assignStmt, table);
        var assignValueType = TypeUtils.getExprType(assignValue, table);

        if (TypeUtils.areTypesAssignable(varType, assignValueType, table)) {
            return null;
        }

        // Create error report
        var message = "Invalid assignment.";
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(assignStmt),
                NodeUtils.getColumn(assignStmt),
                message,
                null)
        );

        return null;
    }
}
