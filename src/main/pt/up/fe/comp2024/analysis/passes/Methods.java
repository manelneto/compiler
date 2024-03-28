package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

public class Methods extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.FUNCTION_CALL, this::visitFunctionCall);
    }

    private Void visitFunctionCall(JmmNode functionCall, SymbolTable table) {

        var functionName = functionCall.get("name");
        var child = functionCall.getChild(0);
        var childType = TypeUtils.getExprType(child, table);

        if (table.getMethods().stream().noneMatch(method -> method.equals(functionName))) {
            if (childType.getName().equals(table.getClassName()) && !table.getSuper().isEmpty()) {
                return null;
            }

            if (table.getImports().stream().anyMatch(i -> i.equals(childType.getName()))) {
                return null;
            }
        }

        var args = table.getParameters(functionName);
        var lastParam = args.get(args.size() - 1);
        var children = functionCall.getChildren();

        if (lastParam.getType().getObject("isEllipsis", Boolean.class)) { // com varargs
            if (children.get(children.size() - 1).getObject("isArray", Boolean.class)) {
                if (args.size() == children.size() - 1) {
                    for (int i = 1; i < children.size() - 1; i++) {
                        if (!TypeUtils.getExprType(children.get(i), table).equals(args.get(i - 1).getType())) {
                            // Create error report
                            var message = "Wrong arguments types.";
                            addReport(Report.newError(
                                    Stage.SEMANTIC,
                                    NodeUtils.getLine(functionCall),
                                    NodeUtils.getColumn(functionCall),
                                    message,
                                    null)
                            );

                            return null;
                        }
                    }
                }
            } else { //ultimo não é array

            }
        } else { // sem varargs

            if (args.size() == children.size() - 1) {
                for (int i = 1; i < children.size(); i++) {
                    if (!TypeUtils.getExprType(children.get(i), table).equals(args.get(i - 1).getType())) {
                        // Create error report
                        var message = "Wrong arguments.";
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(functionCall),
                                NodeUtils.getColumn(functionCall),
                                message,
                                null)
                        );

                        return null;
                    }
                }
            }
        }

        // Create error report
        var message = "Wrong function call.";
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(functionCall),
                NodeUtils.getColumn(functionCall),
                message,
                null)
        );

        return null;
    }
}
