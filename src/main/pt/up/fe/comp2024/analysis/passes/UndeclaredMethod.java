package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;

public class UndeclaredMethod extends AnalysisVisitor {
    private TypeUtils typeUtils;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.FUNCTION_CALL, this::visitFunctionCall);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        typeUtils = new TypeUtils(method.get("name"), table);
        return null;
    }

    private Void visitFunctionCall(JmmNode functionCall, SymbolTable table) {
        String functionName = functionCall.get("name");

        if (table.getMethods().stream().anyMatch(method -> method.equals(functionName))) {
            return null;
        }

        JmmNode expr = functionCall.getChild(0);
        Type type = typeUtils.getExprType(expr);

        if (table.getImports().stream().anyMatch(i -> i.equals(type.getName()))) {
            return null;
        }

        if (type.getName().equals(table.getClassName()) && !table.getSuper().isEmpty()) {
            return null;
        }

        reportError(String.format("Method '%s' does not exist.", functionName), functionCall);

        return null;
    }
}
