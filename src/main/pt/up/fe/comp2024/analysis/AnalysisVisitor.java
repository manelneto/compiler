package pt.up.fe.comp2024.analysis;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public abstract class AnalysisVisitor extends PreorderJmmVisitor<SymbolTable, Void> implements AnalysisPass {

    private List<Report> reports;

    public AnalysisVisitor() {
        reports = new ArrayList<>();
        setDefaultValue(() -> null);
    }

    protected void addReport(Report report) {
        reports.add(report);
    }

    protected List<Report> getReports() {
        return reports;
    }

    protected void reportError(String message, JmmNode node) {
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(node),
                NodeUtils.getColumn(node),
                message,
                null)
        );
    }

    protected boolean isValidAccess(String name, SymbolTable table, String currentMethod) {
        if (!currentMethod.equals("main")) {
            return true;
        }

        if (table.getLocalVariables(currentMethod).stream().anyMatch(var -> var.getName().equals(name))) {
            return true;
        }

        if (table.getParameters(currentMethod).stream().anyMatch(param -> param.getName().equals(name))) {
            return true;
        }

        return table.getFields().stream().noneMatch(field -> field.getName().equals(name));
    }

    @Override
    public List<Report> analyze(JmmNode root, SymbolTable table) {
        // Visit the node
        visit(root, table);

        // Return reports
        return getReports();
    }
}
