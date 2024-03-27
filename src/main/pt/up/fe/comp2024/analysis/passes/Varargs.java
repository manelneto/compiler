package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

public class Varargs extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.PARAM, this::visitParam);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
    }

    private Void visitParam(JmmNode param, SymbolTable table) {

        var paramType = param.getChild(0);

        if (!paramType.getObject("isElypsis", Boolean.class)) {
            return null;
        }

        var method = param.getParent().get("name");

        if (table.getParameters(method).get(table.getParameters(method).size() - 1).getName().equals(param.get("paramName"))) {
            return null;
        }

        var message = "Vararg must be the last parameter of the method";
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(param),
                NodeUtils.getColumn(param),
                message,
                null)
        );

        return null;
    }

    private Void visitVarDecl(JmmNode varDecl, SymbolTable table) {

        var varDeclType = varDecl.getChild(0);

        if (!varDeclType.getObject("isElypsis", Boolean.class)) {
            return null;
        }

        var message = "Variable declaration cannot be a vararg";
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(varDecl),
                NodeUtils.getColumn(varDecl),
                message,
                null)
        );

        return null;
    }

    private Void visitMethodDecl(JmmNode methodDecl, SymbolTable table) {

        if (methodDecl.getObject("isVoid", Boolean.class)) {
            return null;
        }

        var returnType = methodDecl.getChild(0);

        if (!returnType.getObject("isElypsis", Boolean.class)) {
            return null;
        }

        var message = "Method return cannot be a vararg";
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(methodDecl),
                NodeUtils.getColumn(methodDecl),
                message,
                null)
        );

        return null;
    }
}
