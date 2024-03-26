package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

public class This extends AnalysisVisitor {

    private JmmNode currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.THIS, this::visitThis);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method;
        return null;
    }

    private Void visitThis(JmmNode this_, SymbolTable table) {
        if (!currentMethod.getObject("isVoid", Boolean.class)) {
            return null;
        }

        var message = "'This' cannot be used in a static function";
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(this_),
                NodeUtils.getColumn(this_),
                message,
                null)
        );

        return null;

    }
}
