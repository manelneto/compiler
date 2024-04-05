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
            return new Type(expr.get("type"), Boolean.parseBoolean(expr.get("isArray")));
        }

        var kind = Kind.fromString(expr.getKind());

        Type type = switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr);
            case VAR_REF_EXPR, ASSIGN_STMT, ARRAY_ASSIGN_STMT -> getVarExprType(expr);
            case INTEGER_LITERAL, ARRAY_ACCESS -> new Type(INT_TYPE_NAME, false);
            case BOOLEAN_LITERAL -> new Type(BOOLEAN_TYPE_NAME, false);
            case THIS -> new Type(THIS_TYPE_NAME, false);
            case NEW_OBJECT -> new Type(expr.get("name"), false);
            case FUNCTION_CALL -> table.getReturnType(expr.get("name"));
            case ARRAY -> new Type(INT_TYPE_NAME, true);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        expr.put("type", type.getName());
        expr.put("isArray", Boolean.toString(type.isArray()));

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

    public Type getVoidType() {
        return new Type(VOID_TYPE_NAME, false);
    }

    public Type getThisType() {
        return new Type(THIS_TYPE_NAME, false);
    }

    public Type getStringArrayType() {
        return new Type(STRING_TYPE_NAME, true);
    }

    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public boolean areTypesAssignable(Type sourceType, Type destinationType) {

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
