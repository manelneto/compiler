package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

public class Arrays extends AnalysisVisitor {

    String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ARRAY_ACCESS, this::visitArrayAccess);
        addVisit(Kind.ARRAY, this::visitArray);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        TypeUtils.setCurrentMethod(currentMethod); // ?
        return null;
    }

    private Void visitArrayAccess(JmmNode arrayAccess, SymbolTable table) {
        var array = arrayAccess.getChild(0);
        var index = arrayAccess.getChild(1);

        var arrayType = TypeUtils.getExprType(array, table);
        var indexType = TypeUtils.getExprType(index, table);

        if (arrayType.isArray() && indexType.getName().equals(TypeUtils.getIntTypeName()) && !indexType.isArray()) {
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

    private Void visitArray(JmmNode array, SymbolTable table) {

        for (var elem : array.getChildren()) {
            if (!TypeUtils.getExprType(elem, table).equals(new Type(TypeUtils.getIntTypeName(), false))) {
                // Create error report
                var message = "Invalid array elements.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(array),
                        NodeUtils.getColumn(array),
                        message,
                        null)
                );
            }
        }

        return null;
    }
}
