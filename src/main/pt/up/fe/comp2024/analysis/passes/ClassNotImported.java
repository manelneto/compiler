package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

public class ClassNotImported extends AnalysisVisitor {

    private String currentClass;

    @Override
    public void buildVisitor() {
        addVisit(Kind.CLASS_DECL, this::visitClassDecl);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
    }

    private Void visitClassDecl(JmmNode class_, SymbolTable table) {
        currentClass = class_.get("name");

        if (!class_.getObject("isSubclass", Boolean.class)) {
            return null;
        }

        var parentClassName = class_.get("parentClassName");

        if (table.getImports().stream().anyMatch(import_ -> import_.equals(parentClassName))) {
            return null;
        }

        var message = String.format("Class '%s' not imported.", parentClassName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(class_),
                NodeUtils.getColumn(class_),
                message,
                null)
        );

        return null;
    }

    private Void visitVarDecl(JmmNode var_, SymbolTable table) {
        var nodeType = var_.getChild(0);
        var nodeName = nodeType.get("name");
        if (nodeType.getObject("isArray", Boolean.class) || nodeName.equals("boolean")
                || nodeName.equals("int") || nodeName.equals(currentClass)) {
            return null;
        }

        if (table.getImports().stream().anyMatch(import_ -> import_.equals(nodeName))) {
            return null;
        }

        var message = String.format("Class '%s' not imported.", nodeName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(var_),
                NodeUtils.getColumn(var_),
                message,
                null)
        );

        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        if (method.getObject("isVoid", Boolean.class)) {
            return null;
        }

        var nodeType = method.getChild(0);
        var nodeName = nodeType.get("name");
        if (nodeType.getObject("isArray", Boolean.class) || nodeName.equals("boolean")
                || nodeName.equals("int") || nodeName.equals(currentClass)) {
            return null;
        }

        if (table.getImports().stream().anyMatch(import_ -> import_.equals(nodeName))) {
            return null;
        }

        var message = String.format("Class '%s' not imported.", nodeName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(method),
                NodeUtils.getColumn(method),
                message,
                null)
        );

        return null;
    }
}
