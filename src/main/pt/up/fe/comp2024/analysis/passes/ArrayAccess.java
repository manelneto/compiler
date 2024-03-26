package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

public class ArrayAccess extends AnalysisVisitor {

    String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ARRAY_ACCESS, this::visitArrayAccess);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        TypeUtils.setCurrentMethod(currentMethod); // ?
        return null;
    }

    private Void visitArrayAccess(JmmNode array, SymbolTable table) {
        var child = array.getChild(0);
        var type = TypeUtils.getExprType(child, table);

        if (type.isArray()) {
            return null;
        }

        // Create error report
        var message = "Invalid array access.";
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(array),
                NodeUtils.getColumn(array),
                message,
                null)
        );

        return null;
    }

}
