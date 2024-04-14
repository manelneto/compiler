package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;

public class Conditions extends AnalysisVisitor {
    private TypeUtils typeUtils;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.IF_ELSE_STMT, this::visitCondition);
        addVisit(Kind.WHILE_STMT, this::visitCondition);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        typeUtils = new TypeUtils(method.get("name"), table);
        return null;
    }

    private Void visitCondition(JmmNode stmt, SymbolTable table) {
        var conditionNode = stmt.getChild(0);
        var conditionType = typeUtils.getExprType(conditionNode);

        if (conditionType.getName().equals(typeUtils.getBooleanTypeName()) && !conditionType.isArray()) {
            return null;
        }

        reportError("Invalid condition", stmt);

        return null;
    }
}
