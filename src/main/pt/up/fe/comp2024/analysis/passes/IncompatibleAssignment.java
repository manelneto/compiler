package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;

public class IncompatibleAssignment extends AnalysisVisitor {
    private TypeUtils typeUtils;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(Kind.ARRAY_ASSIGN_STMT, this::visitArrayAssignStmt);  // TODO: Criar outra função
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        typeUtils = new TypeUtils(method.get("name"), table);
        return null;
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        JmmNode rhs = assignStmt.getChild(assignStmt.getChildren().size() - 1);

        Type lhsType = typeUtils.getExprType(assignStmt);
        Type rhsType = typeUtils.getExprType(rhs);

        if (typeUtils.areTypesAssignable(lhsType, rhsType)) {
            return null;
        }

        reportError("Invalid assignment", assignStmt);

        return null;
    }

    private Void visitArrayAssignStmt (JmmNode arrayAssignStmt, SymbolTable table) {
        JmmNode rhs = arrayAssignStmt.getChild(1);
        JmmNode index = arrayAssignStmt.getChild(0);


        Type rhsType = typeUtils.getExprType(rhs);
        Type indexType = typeUtils.getExprType(index);
        Type lhsType = typeUtils.getExprType(arrayAssignStmt);

        if (typeUtils.areTypesAssignable(lhsType, rhsType)) {
            return null;
        }

        if (!indexType.isArray() && indexType.getName().equals(typeUtils.getIntTypeName())){
            return null;
        }

        reportError("Invalid array assignment", arrayAssignStmt);


        return null;
    }
}
