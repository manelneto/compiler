package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";
    private static final String STRING_TYPE_NAME = "String";
    private static final String VOID_TYPE_NAME = "void";
    private static final String BOOLEAN_TYPE_NAME = "boolean";
    private static final String THIS_TYPE_NAME = "this";

    private static String currentMethod = "";

    public static String getIntTypeName() {
        return INT_TYPE_NAME;
    }

    public static String getBooleanTypeName() {
        return BOOLEAN_TYPE_NAME;
    }

    public static void setCurrentMethod(String currentMethod) {
        TypeUtils.currentMethod = currentMethod;
    }

    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @param table
     * @return
     */
    public static Type getExprType(JmmNode expr, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded

        var kind = Kind.fromString(expr.getKind());

        Type type = switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr);
            case VAR_REF_EXPR, ASSIGN_STMT, ARRAY_ASSIGN_STMT -> getVarExprType(expr, table);
            case INTEGER_LITERAL -> new Type(INT_TYPE_NAME, false);
            case BOOLEAN_LITERAL -> new Type(BOOLEAN_TYPE_NAME, false);
            case THIS -> new Type(THIS_TYPE_NAME, false);
            case NEW_OBJECT -> new Type(expr.get("name"), false);
            case FUNCTION_CALL -> table.getReturnType(expr.get("name"));
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
    }

    private static Type getBinExprType(JmmNode binaryExpr) {
        // TODO: Simple implementation that needs to be expanded

        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "*", "/", "-" -> new Type(INT_TYPE_NAME, false);
            case "<", "&&" -> new Type(BOOLEAN_TYPE_NAME, false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }


    private static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded
        var varName = varRefExpr.get("name");
        for (var local : table.getLocalVariables(currentMethod)) {
            if (local.getName().equals(varName)) {
                return local.getType();
            }
        }

        for (var param : table.getParameters(currentMethod)) {
            if (param.getName().equals(varName)) {
                return param.getType();
            }
        }

        for (var field : table.getFields()) {
            if (field.getName().equals(varName)) {
                return field.getType();
            }
        }

        return null;
    }

    public static Type getVoidType() {
        return new Type(VOID_TYPE_NAME, false);
    }

    public static Type getThisType() {
        return new Type(THIS_TYPE_NAME, false);
    }

    public static Type getStringArrayType() {
        return new Type(STRING_TYPE_NAME, true);
    }

    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded
        if (table.getImports().stream().anyMatch(i -> i.equals(sourceType.getName())) && table.getImports().stream().anyMatch(i -> i.equals(destinationType.getName()))) {
            return true;
        }

        if (sourceType.getName().equals(destinationType.getName()) && sourceType.isArray() == destinationType.isArray()) {
            return true;
        }

        if (table.getSuper().equals(sourceType.getName()) && table.getClassName().equals(destinationType.getName())) {
            return true;
        }

        return destinationType.getName().equals(THIS_TYPE_NAME) && (table.getClassName().equals(sourceType.getName()) || table.getSuper().equals(sourceType.getName()));
    }
}
