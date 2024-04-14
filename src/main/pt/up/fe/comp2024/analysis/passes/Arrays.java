package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;

public class Arrays extends AnalysisVisitor {
    private String currentMethod;
    private TypeUtils typeUtils;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ARRAY_ACCESS, this::visitArrayAccess);
        addVisit(Kind.ARRAY, this::visitArray);
        addVisit(Kind.LENGTH, this::visitLength);
        addVisit(Kind.ARRAY_ASSIGN_STMT, this::visitArrayAssignStmt);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        typeUtils = new TypeUtils(currentMethod, table);
        return null;
    }

    private Void visitArrayAccess(JmmNode arrayAccess, SymbolTable table) {
        JmmNode array = arrayAccess.getChild(0);
        JmmNode index = arrayAccess.getChild(1);

        Type arrayType = typeUtils.getExprType(array);
        Type indexType = typeUtils.getExprType(index);

        if (arrayType.isArray() && typeUtils.isIndexable(indexType)) {
            return null;
        }

        reportError("Invalid array access", array);

        return null;
    }

    private Void visitArray(JmmNode array, SymbolTable table) {
        for (JmmNode elem : array.getChildren()) {
            Type elemType = typeUtils.getExprType(elem);
            if (!elemType.getName().equals(typeUtils.getIntTypeName()) || elemType.isArray()) {
                reportError("Invalid array elements", array);
            }
        }
        return null;
    }

    private Void visitLength(JmmNode length, SymbolTable table) {
        JmmNode expr = length.getChild(0);
        String id = length.get("name");

        if (typeUtils.getExprType(expr).isArray() && id.equals(Kind.LENGTH.toString().toLowerCase())) {
            return null;
        }

        reportError("Invalid field access", length);

        return null;
    }

    private Void visitArrayAssignStmt(JmmNode arrayAssignStmt, SymbolTable table) {
        String name = arrayAssignStmt.get("name");

        if (isValidAccess(name, table, currentMethod)) {
            return null;
        }

        reportError("Cannot assign class field inside static function", arrayAssignStmt);

        return null;
    }
}
