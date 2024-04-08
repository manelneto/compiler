package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;

public class Varargs extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.PARAM, this::visitParam);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
    }

    private Void visitParam(JmmNode param, SymbolTable table) {

        var paramType = param.getChild(0);

        if (!paramType.getObject("isEllipsis", Boolean.class)) {
            return null;
        }

        var method = param.getParent().get("name");

        if (table.getParameters(method).get(table.getParameters(method).size() - 1).getName().equals(param.get("paramName"))) {
            return null;
        }

        reportError("Vararg must be the last parameter of the method", param);

        return null;
    }

    private Void visitVarDecl(JmmNode varDecl, SymbolTable table) {

        var varDeclType = varDecl.getChild(0);

        if (!varDeclType.getObject("isEllipsis", Boolean.class)) {
            return null;
        }

        reportError("Variable declaration cannot be a vararg", varDecl);

        return null;
    }

    private Void visitMethodDecl(JmmNode methodDecl, SymbolTable table) {

        if (methodDecl.getObject("isVoid", Boolean.class)) {
            return null;
        }

        var returnType = methodDecl.getChild(0);

        if (!returnType.getObject("isEllipsis", Boolean.class)) {
            return null;
        }

        reportError("Method return cannot be a vararg", methodDecl);

        return null;
    }
}
