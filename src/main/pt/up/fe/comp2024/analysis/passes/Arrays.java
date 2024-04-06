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

    private String currentMethod;
    private SymbolTable table;
    TypeUtils typeUtils = new TypeUtils("", table);

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ARRAY_ACCESS, this::visitArrayAccess);
        addVisit(Kind.ARRAY, this::visitArray);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        typeUtils.setCurrentMethod(currentMethod);
        return null;
    }

    private Void visitArrayAccess(JmmNode arrayAccess, SymbolTable table) {

        typeUtils.setTable(table);

        var array = arrayAccess.getChild(0);
        var index = arrayAccess.getChild(1);

        var arrayType = typeUtils.getExprType(array);
        var indexType = typeUtils.getExprType(index);

        if (arrayType.isArray() && indexType.getName().equals(typeUtils.getIntTypeName()) && !indexType.isArray()) {
            return null;
        }

        reportError("Invalid array access", array);

        return null;
    }

    private Void visitArray(JmmNode array, SymbolTable table) {

        typeUtils.setTable(table);

        for (var elem : array.getChildren()) {
            if (!typeUtils.getExprType(elem).equals(new Type(typeUtils.getIntTypeName(), false))) {
                reportError("Invalid array elements", array);
            }
        }

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
