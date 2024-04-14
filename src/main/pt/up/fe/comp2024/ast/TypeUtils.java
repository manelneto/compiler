package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class TypeUtils {

    private final String INT_TYPE_NAME = "int";
    private final String STRING_TYPE_NAME = "String";
    private final String VOID_TYPE_NAME = "void";
    private final String BOOLEAN_TYPE_NAME = "boolean";
    private final String THIS_TYPE_NAME = "this";
    private String currentMethod;
    private SymbolTable table;

    public TypeUtils(String currentMethod, SymbolTable table) {
        this.currentMethod = currentMethod;
        this.table = table;
    }

    public TypeUtils() {
        this.currentMethod = "";
        this.table = null;
    }

    public String getIntTypeName() {
        return INT_TYPE_NAME;
    }

    public String getBooleanTypeName() {
        return BOOLEAN_TYPE_NAME;
    }

    public void setCurrentMethod(String currentMethod1) {
        this.currentMethod = currentMethod1;
    }

    public void setTable(SymbolTable table1) {
        this.table = table1;
    }

    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @return
     */
    public Type getExprType(JmmNode expr) {

        if (expr.hasAttribute("type") && expr.hasAttribute("isArray")) {
            return new Type(expr.get("type"), expr.getObject("isArray", Boolean.class));
        }

        var kind = Kind.fromString(expr.getKind());

        Type type = switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr);
            case VAR_REF_EXPR, ASSIGN_STMT, ARRAY_ASSIGN_STMT, THIS -> getVarExprType(expr);
            case INTEGER_LITERAL, ARRAY_ACCESS -> new Type(INT_TYPE_NAME, false);
            case BOOLEAN_LITERAL -> new Type(BOOLEAN_TYPE_NAME, false);
            case NEW_OBJECT -> new Type(expr.get("name"), false);
            case FUNCTION_CALL -> table.getReturnType(expr.get("name"));
            case ARRAY, NEW_ARRAY -> new Type(INT_TYPE_NAME, true);
            case PAREN_EXPR -> getExprType(expr.getChild(0));
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        assert type != null;

        expr.putObject("type", type.getName());
        expr.putObject("isArray", type.isArray());

        return type;
    }

    private Type getBinExprType(JmmNode binaryExpr) {

        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "*", "/", "-" -> new Type(INT_TYPE_NAME, false);
            case "<", "&&" -> new Type(BOOLEAN_TYPE_NAME, false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }


    private Type getVarExprType(JmmNode varRefExpr) {
        if (varRefExpr.getKind().equals(Kind.THIS.toString())) {
            varRefExpr.putObject("isInstance", true);
            return new Type(table.getClassName(), false);
        }

        var varName = varRefExpr.get("name");
        for (var local : table.getLocalVariables(currentMethod)) {
            if (local.getName().equals(varName)) {
                varRefExpr.putObject("isInstance", true);
                return local.getType();
            }
        }

        for (var param : table.getParameters(currentMethod)) {
            if (param.getName().equals(varName)) {
                varRefExpr.putObject("isInstance", true);
                return param.getType();
            }
        }

        for (var field : table.getFields()) {
            if (field.getName().equals(varName)) {
                varRefExpr.putObject("isInstance", true);
                return field.getType();
            }
        }

        for (var i : table.getImports()) {
            if (i.equals(varName)) {
                varRefExpr.putObject("isInstance", false);
                return new Type(i, false);
            }
        }

        return null;
    }

    public Type getVoidType() {
        return new Type(VOID_TYPE_NAME, false);
    }

    public Type getThisType() {
        return new Type(THIS_TYPE_NAME, false);
    }

    public Type getStringArrayType() {
        return new Type(STRING_TYPE_NAME, true);
    }

    public boolean isIndexable(Type indexType) {
        return indexType.equals(getVoidType()) || indexType.equals(new Type(INT_TYPE_NAME, false));
    }

    public boolean areTypesAssignable(Type lhsType, Type rhsType) {
        if (rhsType.getName().equals(VOID_TYPE_NAME)) {
            return true;
        }

        if (table.getImports().stream().anyMatch(i -> i.equals(lhsType.getName())) && table.getImports().stream().anyMatch(i -> i.equals(rhsType.getName()))) {
            return true;
        }

        if (lhsType.getName().equals(rhsType.getName()) && lhsType.isArray() == rhsType.isArray()) {
            return true;
        }

        if (table.getSuper().equals(lhsType.getName()) && table.getClassName().equals(rhsType.getName())) {
            return true;
        }

        return rhsType.getName().equals(THIS_TYPE_NAME) && (table.getClassName().equals(lhsType.getName()) || table.getSuper().equals(lhsType.getName()));
    }
}
