package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class TypeUtils {
    private final String INT_TYPE_NAME = "int";
    private final String BOOLEAN_TYPE_NAME = "boolean";
    private final String VOID_TYPE_NAME = "void";
    private String currentMethod;
    private SymbolTable table;

    public TypeUtils() {
        this.currentMethod = "";
        this.table = null;
    }

    public TypeUtils(String currentMethod, SymbolTable table) {
        this.currentMethod = currentMethod;
        this.table = table;
    }

    public void setCurrentMethod(String currentMethod) {
        this.currentMethod = currentMethod;
    }

    public void setTable(SymbolTable table) {
        this.table = table;
    }

    public String getIntTypeName() {
        return INT_TYPE_NAME;
    }

    public String getBooleanTypeName() {
        return BOOLEAN_TYPE_NAME;
    }

    public Type getVoidType() {
        return new Type(VOID_TYPE_NAME, false);
    }

    public Type getStringArrayType() {
        return new Type("String", true);
    }

    public Type getStmtType(JmmNode stmt) {
        assert Kind.fromString(stmt.getKind()).isStmt();

        if (stmt.hasAttribute("type") && stmt.hasAttribute("isArray")) {
            return new Type(stmt.get("type"), stmt.getObject("isArray", Boolean.class));
        }

        Kind kind = Kind.fromString(stmt.getKind());

        Type type = switch (kind) {
            case SIMPLE_STMT -> getExprType(stmt.getChild(0));
            case ASSIGN_STMT -> getAssignStmtType(stmt);
            case ARRAY_ASSIGN_STMT -> new Type(INT_TYPE_NAME, false);
            case RETURN_STMT -> table.getReturnType(currentMethod);
            default -> throw new RuntimeException("Can't compute type for statement: '" + kind + "'");
        };

        stmt.putObject("type", type.getName());
        stmt.putObject("isArray", type.isArray());

        return type;
    }

    private Type getAssignStmtType(JmmNode assignStmt) {
        assert assignStmt.getKind().equals(Kind.ASSIGN_STMT.toString());

        String varName = assignStmt.get("name");
        for (Symbol local : table.getLocalVariables(currentMethod)) {
            if (local.getName().equals(varName)) {
                return local.getType();
            }
        }

        for (Symbol param : table.getParameters(currentMethod)) {
            if (param.getName().equals(varName)) {
                return param.getType();
            }
        }

        for (Symbol field : table.getFields()) {
            if (field.getName().equals(varName)) {
                return field.getType();
            }
        }

        return null;
    }

    public Type getExprType(JmmNode expr) {
        assert Kind.fromString(expr.getKind()).isExpr();

        if (expr.hasAttribute("type") && expr.hasAttribute("isArray")) {
            return new Type(expr.get("type"), expr.getObject("isArray", Boolean.class));
        }

        Kind kind = Kind.fromString(expr.getKind());

        Type type = switch (kind) {
            case PAREN_EXPR -> getExprType(expr.getChild(0));
            case ARRAY_ACCESS -> new Type(INT_TYPE_NAME, false);
            case FUNCTION_CALL -> getFunctionCallType(expr);
            case LENGTH -> new Type(INT_TYPE_NAME, false);
            case UNARY_EXPR -> new Type(BOOLEAN_TYPE_NAME, false);
            case NEW_OBJECT -> getNewObjectType(expr);
            case NEW_ARRAY -> new Type(INT_TYPE_NAME, true);
            case BINARY_EXPR -> getBinaryExprType(expr);
            case ARRAY -> new Type(INT_TYPE_NAME, true);
            case INTEGER_LITERAL -> new Type(INT_TYPE_NAME, false);
            case BOOLEAN_LITERAL -> new Type(BOOLEAN_TYPE_NAME, false);
            case VAR_REF_EXPR -> getVarRefExprType(expr);
            case THIS -> getThisType(expr);
            default -> throw new RuntimeException("Can't compute type for expression: '" + kind + "'");
        };

        if (type != null) {
            expr.putObject("type", type.getName());
            expr.putObject("isArray", type.isArray());
        }

        return type;
    }

    private Type getFunctionCallType(JmmNode functionCall) {
        assert functionCall.getKind().equals(Kind.FUNCTION_CALL.toString());

        JmmNode expr = functionCall.getChild(0);
        String functionName = functionCall.get("name");

        if ((expr.getKind().equals(Kind.THIS.toString()) && table.getSuper().isEmpty()) || getExprType(expr).getName().equals(table.getClassName()) && table.getMethods().contains(functionName)) {
            return table.getReturnType(functionName);
        }

        JmmNode parent = functionCall.getParent();

        while (!Kind.fromString(parent.getKind()).isStmt() && !parent.getKind().equals(Kind.FUNCTION_CALL.toString())) {
            parent = parent.getParent();
        }

        if (parent.getKind().equals(Kind.SIMPLE_STMT.toString())) {
            return new Type(VOID_TYPE_NAME, false);
        }

        if (parent.getKind().equals(Kind.FUNCTION_CALL.toString())) {
            String method = parent.get("name");
            int pos = functionCall.getIndexOfSelf() - 1;
            return table.getParameters(method).get(pos).getType();
        }

        return getStmtType(parent);
    }

    private Type getNewObjectType(JmmNode newObject) {
        annotate(newObject, "isInstance", true);
        return new Type(newObject.get("name"), false);
    }

    private Type getBinaryExprType(JmmNode binaryExpr) {
        assert binaryExpr.getKind().equals(Kind.BINARY_EXPR.toString());

        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "*", "/", "-" -> new Type(INT_TYPE_NAME, false);
            case "<", "&&" -> new Type(BOOLEAN_TYPE_NAME, false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }

    private Type getVarRefExprType(JmmNode varRefExpr) {
        assert varRefExpr.getKind().equals(Kind.VAR_REF_EXPR.toString());

        String varName = varRefExpr.get("name");
        for (Symbol local : table.getLocalVariables(currentMethod)) {
            if (local.getName().equals(varName)) {
                annotate(varRefExpr, "isInstance", true);
                return local.getType();
            }
        }

        for (Symbol param : table.getParameters(currentMethod)) {
            if (param.getName().equals(varName)) {
                annotate(varRefExpr, "isInstance", true);
                return param.getType();
            }
        }

        for (Symbol field : table.getFields()) {
            if (field.getName().equals(varName)) {
                annotate(varRefExpr, "isInstance", true);
                return field.getType();
            }
        }

        for (String i : table.getImports()) {
            if (i.equals(varName)) {
                annotate(varRefExpr, "isInstance", false);
                return new Type(i, false);
            }
        }

        return null;
    }

    private Type getThisType(JmmNode this_) {
        assert this_.getKind().equals(Kind.THIS.toString());

        annotate(this_, "isInstance", true);

        JmmNode parent = this_.getParent();
        String name = parent.get("name");

        if (table.getMethods().contains(name)) {
            return new Type(table.getClassName(), false);
        }

        return new Type(table.getSuper(), false);
    }

    private static void annotate(JmmNode node, String name, boolean value) {
        JmmNode parent = node.getParent();
        if (parent.getKind().equals(Kind.PAREN_EXPR.toString())) {
            parent.putObject(name, value);
        }
        node.putObject(name, value);
    }

    public boolean isIndexable(Type indexType) {
        // TODO
        return indexType.equals(getVoidType()) || indexType.equals(new Type(INT_TYPE_NAME, false));
    }

    public boolean areTypesAssignable(Type lhsType, Type rhsType) {
        if (table.getImports().stream().anyMatch(i -> i.equals(lhsType.getName())) && table.getImports().stream().anyMatch(i -> i.equals(rhsType.getName()))) {
            return true;
        }

        if (lhsType.getName().equals(rhsType.getName()) && lhsType.isArray() == rhsType.isArray()) {
            return true;
        }

        if (table.getSuper().equals(lhsType.getName()) && table.getClassName().equals(rhsType.getName())) {
            return true;
        }

        return table.getClassName().equals(lhsType.getName()) || table.getSuper().equals(lhsType.getName());
    }

    public boolean isLocal(String varName) {
        return table.getLocalVariables(currentMethod).stream().anyMatch(local -> local.getName().equals(varName));
    }

    public boolean isParameter(String varName) {
        return table.getParameters(currentMethod).stream().anyMatch(parameter -> parameter.getName().equals(varName));
    }

    public boolean isField(String varName) {
        return !isLocal(varName) && !isParameter(varName) && table.getFields().stream().anyMatch(field -> field.getName().equals(varName));
    }

    public boolean isImport(String varName) {
        return table.getImports().stream().anyMatch(i -> i.equals(varName));
    }
}
