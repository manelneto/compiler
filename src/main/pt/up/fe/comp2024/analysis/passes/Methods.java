package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;

public class Methods extends AnalysisVisitor {
    private String currentMethod;
    private TypeUtils typeUtils;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.FUNCTION_CALL, this::visitFunctionCall);
        addVisit(Kind.RETURN_STMT, this::visitReturnStmt);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        typeUtils = new TypeUtils(currentMethod, table);
        return null;
    }

    private Void visitFunctionCall(JmmNode functionCall, SymbolTable table) {
        var functionName = functionCall.get("name");
        var child = functionCall.getChild(0);
        var childType = typeUtils.getExprType(child);

        if (table.getMethods().stream().noneMatch(method -> method.equals(functionName))) {
            if (childType.getName().equals(table.getClassName()) && !table.getSuper().isEmpty()) {
                return null;
            }

            if (table.getImports().stream().anyMatch(i -> i.equals(childType.getName()))) {
                return null;
            }
        }

        var args = table.getParameters(functionName);
        if (args.isEmpty()) {
            return null;
        }

        var lastParam = args.get(args.size() - 1);
        var children = functionCall.getChildren();

        if (lastParam.getType().getObject("isEllipsis", Boolean.class)) { // com varargs
            var lastChild = children.get(children.size() - 1);
            if (lastChild.getKind().equals(Kind.ARRAY.toString()) || (lastChild.hasAttribute("isArray") && lastChild.getObject("isArray", Boolean.class))) {
                if (args.size() == children.size() - 1) {
                    for (int i = 1; i < children.size() - 1; i++) {
                        if (!typeUtils.getExprType(children.get(i)).equals(args.get(i - 1).getType())) {
                            reportError("Wrong arguments types (with array)", functionCall);

                            return null;
                        }
                    }
                    return null;
                }
            } else { //ultimo não é array
                int j = 0;
                for (int i = 1; i < children.size(); i++) {
                    var param = children.get(i);
                    if ((param.hasAttribute("isArray") && param.getObject("isArray", Boolean.class)) || !typeUtils.getExprType(param).getName().equals(args.get(j).getType().getName())) {
                        reportError("Wrong arguments types (without array)", functionCall);

                        return null;
                    }
                    if (j != args.size() - 1) {
                        j++;
                    }
                }
                return null;
            }
        } else { // sem varargs

            if (args.size() == children.size() - 1) {
                for (int i = 1; i < children.size(); i++) {
                    if (!typeUtils.getExprType(children.get(i)).equals(args.get(i - 1).getType())) {
                        reportError("Wrong arguments", functionCall);

                        return null;
                    }
                }
                return null;
            }
        }

        reportError("Wrong function call", functionCall);

        return null;
    }

    private Void visitReturnStmt(JmmNode returnStmt, SymbolTable table) {
        var child = returnStmt.getChild(0);
        if (child.getKind().equals(Kind.FUNCTION_CALL.toString())) {
            var childType = typeUtils.getExprType(child.getChild(0));

            if (table.getImports().stream().anyMatch(i -> i.equals(childType.getName()))) {
                return null;
            }
        }

        var returnType = typeUtils.getExprType(child);
        if (returnType.equals(table.getReturnType(currentMethod))) {
            return null;
        }

        reportError("Wrong return type", returnStmt);

        return null;
    }

    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        if (!currentMethod.equals("main")) {
            return null;
        }

        String name = varRefExpr.get("name");

        if (table.getFields().stream().noneMatch(field -> field.getName().equals(name))) {
            return null;
        }

        reportError("Cannot access class field inside static function", varRefExpr);

        return null;
    }
}
