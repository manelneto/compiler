package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

public class Conditions extends AnalysisVisitor {
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.IF_ELSE_STMT, this::visitCondition);
        addVisit(Kind.WHILE_STMT, this::visitCondition);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        TypeUtils.setCurrentMethod(currentMethod); // ?
        return null;
    }

    private Void visitCondition(JmmNode stmt, SymbolTable table) {
        var conditionNode = stmt.getChild(0);
        var conditionType = TypeUtils.getExprType(conditionNode, table);

        if (conditionType.getName().equals(TypeUtils.getBooleanTypeName()) && !conditionType.isArray()) {
            return null;
        }
        
        var message = "Invalid condition.";
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(stmt),
                NodeUtils.getColumn(stmt),
                message,
                null)
        );

        return null;
    }

}
