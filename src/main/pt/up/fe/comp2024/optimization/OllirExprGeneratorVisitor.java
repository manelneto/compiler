package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.TypeUtils;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends AJmmVisitor<Void, OllirExprResult> {

    private static final String ARRAY_INT_TYPE = ".array.i32";
    private static final String ASSIGN = ":=";
    private static final String END_STMT = ";\n";
    private static final String INT_TYPE = ".i32";
    private static final String SPACE = " ";
    private final SymbolTable table;
    private final TypeUtils typeUtils;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
        this.typeUtils = new TypeUtils("", table);
    }

    public void setCurrentMethod(String currentMethod) {
        typeUtils.setCurrentMethod(currentMethod);
    }

    @Override
    protected void buildVisitor() {
        addVisit(PAREN_EXPR, this::visitParenExpr);
        addVisit(ARRAY_ACCESS, this::visitArrayAccess);
        addVisit(FUNCTION_CALL, this::visitFunctionCall);
        addVisit(LENGTH, this::visitLength);
        addVisit(UNARY_EXPR, this::visitUnaryExpr);
        addVisit(NEW_OBJECT, this::visitNewObject);
        addVisit(NEW_ARRAY, this::visitNewArray);
        addVisit(BINARY_EXPR, this::visitBinaryExpr);
        addVisit(ARRAY, this::visitArray);
        addVisit(INTEGER_LITERAL, this::visitIntegerLiteral);
        addVisit(BOOLEAN_LITERAL, this::visitBooleanLiteral);
        addVisit(VAR_REF_EXPR, this::visitVarRefExpr);
        addVisit(THIS, this::visitThis);
        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitParenExpr(JmmNode parenExpr, Void unused) {
        JmmNode expr = parenExpr.getChild(0);
        return visit(expr);
    }

    private OllirExprResult visitArrayAccess(JmmNode arrayAccess, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        JmmNode array = arrayAccess.getChild(0); // TODO: ID or FunctionCall?
        JmmNode index = arrayAccess.getChild(1);

        OllirExprResult result = visit(index);

        code.append(OptUtils.getTemp()).append(INT_TYPE);

        computation.append(result.getComputation());
        computation.append(code).append(SPACE).append(ASSIGN).append(INT_TYPE).append(SPACE);
        computation.append(array.get("name")).append(ARRAY_INT_TYPE);
        computation.append("[").append(result.getCode()).append("]").append(INT_TYPE).append(END_STMT);

        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitFunctionCall(JmmNode functionCall, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        JmmNode child = functionCall.getChild(0);
        String type = OptUtils.toOllirType(typeUtils.getExprType(functionCall));

        OllirExprResult exprResult = visit(child);
        computation.append(exprResult.getComputation());

        StringBuilder argumentsCode = new StringBuilder();

        int argumentsNumber = functionCall.getNumChildren() - 1;
        Type lastArgumentType = typeUtils.getExprType(functionCall.getChildren().get(argumentsNumber));

        if (!functionCall.hasAttribute("hasVarargs") || !functionCall.getObject("hasVarargs", Boolean.class) || lastArgumentType.isArray()) {
            for (int i = 1; i < functionCall.getNumChildren(); i++) {
                OllirExprResult result = visit(functionCall.getJmmChild(i));
                computation.append(result.getComputation());
                argumentsCode.append(", ").append(result.getCode());
            }
        } else {
            int paramsNumber = table.getParameters(functionCall.get("name")).size();
            int arraySize = argumentsNumber - paramsNumber + 1;
            String arrayTemp = OptUtils.getTemp();

            computation.append(arrayTemp).append(ARRAY_INT_TYPE).append(SPACE);
            computation.append(ASSIGN).append(ARRAY_INT_TYPE).append(SPACE);
            computation.append("new(array, ").append(arraySize).append(INT_TYPE).append(")").append(ARRAY_INT_TYPE).append(END_STMT);

            for (int i = 1; i < paramsNumber; i++) {
                OllirExprResult result = visit(functionCall.getJmmChild(i));
                computation.append(result.getComputation());
                argumentsCode.append(", ").append(result.getCode());
            }

            for (int i = 0; i < arraySize; i++) {
                JmmNode arrayElement = functionCall.getChild(1 + i + argumentsNumber - arraySize);
                OllirExprResult arrayElementResult = visit(arrayElement);
                computation.append(arrayElementResult.getComputation());
                computation.append(arrayTemp).append("[").append(i).append(INT_TYPE).append("]").append(INT_TYPE).append(SPACE);
                computation.append(ASSIGN).append(INT_TYPE).append(SPACE).append(arrayElementResult.getCode()).append(END_STMT);
            }

            argumentsCode.append(", ").append(arrayTemp).append(ARRAY_INT_TYPE);
        }

        if (!functionCall.getParent().getKind().equals(SIMPLE_STMT.toString())) {
            code.append(OptUtils.getTemp()).append(type);
            computation.append(code).append(SPACE).append(ASSIGN).append(type).append(SPACE);
        }

        if (child.hasAttribute("isInstance") && child.getObject("isInstance", Boolean.class)) {
            computation.append("invokevirtual(");
        } else {
            computation.append("invokestatic(");
        }

        computation.append(exprResult.getCode()).append(", ");
        computation.append("\"").append(functionCall.get("name")).append("\"");
        computation.append(argumentsCode).append(")");
        computation.append(OptUtils.toOllirType(typeUtils.getExprType(functionCall)));

        if (!functionCall.getParent().getKind().equals(SIMPLE_STMT.toString())) {
            computation.append(END_STMT);
        }

        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitNewObject(JmmNode newObject, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        String temp = OptUtils.getTemp();
        Type type = typeUtils.getExprType(newObject);
        String ollirType = OptUtils.toOllirType(type);

        code.append(temp).append(ollirType);

        computation.append(temp).append(ollirType).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE);
        computation.append("new(").append(type.getName()).append(")").append(ollirType).append(END_STMT);
        computation.append("invokespecial(").append(temp).append(ollirType).append(", \"<init>\").V").append(END_STMT);

        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitNewArray(JmmNode newArray, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        JmmNode expr = newArray.getChild(0);
        OllirExprResult result = visit(expr);

        code.append(OptUtils.getTemp()).append(ARRAY_INT_TYPE);

        computation.append(result.getComputation());
        computation.append(code).append(SPACE).append(ASSIGN).append(ARRAY_INT_TYPE).append(SPACE);
        computation.append("new(array, ").append(result.getCode()).append(")").append(ARRAY_INT_TYPE).append(END_STMT);

        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitArray(JmmNode array, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        String arrayTemp = OptUtils.getTemp();
        int arraySize = array.getNumChildren();

        code.append(arrayTemp).append(ARRAY_INT_TYPE);

        computation.append(code).append(SPACE).append(ASSIGN).append(ARRAY_INT_TYPE).append(SPACE);
        computation.append("new(array, ").append(arraySize).append(INT_TYPE).append(")").append(ARRAY_INT_TYPE).append(END_STMT);

        for (int i = 0; i < array.getChildren().size(); i++) {
            JmmNode elem = array.getChild(i);
            OllirExprResult result = visit(elem);

            computation.append(result.getComputation());
            computation.append(arrayTemp).append("[").append(i).append(INT_TYPE).append("]").append(INT_TYPE).append(SPACE);
            computation.append(ASSIGN).append(INT_TYPE).append(SPACE);
            computation.append(result.getCode()).append(END_STMT);
        }

        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitLength(JmmNode length, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        JmmNode expr = length.getChild(0);
        OllirExprResult result = visit(expr);

        code.append(OptUtils.getTemp()).append(INT_TYPE);

        computation.append(result.getComputation());
        computation.append(code).append(SPACE).append(ASSIGN).append(INT_TYPE).append(SPACE);
        computation.append("arraylength(").append(result.getCode()).append(")").append(INT_TYPE).append(END_STMT);

        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitUnaryExpr(JmmNode unaryExpr, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        String booleanType = OptUtils.toOllirType(typeUtils.getBooleanType());
        JmmNode expr = unaryExpr.getChild(0);
        OllirExprResult result = visit(expr);

        code.append(OptUtils.getTemp()).append(booleanType);

        computation.append(result.getComputation());
        computation.append(code).append(SPACE).append(ASSIGN).append(booleanType).append(SPACE);
        computation.append("!").append(booleanType).append(SPACE).append(result.getCode()).append(END_STMT);

        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitBinaryExpr(JmmNode binaryExpr, Void unused) {
        OllirExprResult lhs = visit(binaryExpr.getJmmChild(0));
        OllirExprResult rhs = visit(binaryExpr.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        Type resType = typeUtils.getExprType(binaryExpr);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        String exprType = binaryExpr.get("type");


        if (binaryExpr.get("op").equals("&&")) {
            String booleanType = OptUtils.toOllirType(typeUtils.getBooleanType());
            String ifNumber = OptUtils.getIfNumber();

//            String negCode = OptUtils.getTemp() + booleanType;
//            String negComputation = negCode + SPACE + ASSIGN + booleanType +
//                    " !" + booleanType + SPACE + code + END_STMT;

            //computation.append(negComputation);


              String lhsNegCode= OptUtils.getTemp() + booleanType;
              String lhsNeg = lhsNegCode + SPACE + ASSIGN + booleanType +
                    " !" + booleanType + SPACE + lhs.getCode() + END_STMT;

              computation.append(lhsNeg);

              String rhsNegCode= OptUtils.getTemp() + booleanType;
              String rhsNeg = rhsNegCode + SPACE + ASSIGN + booleanType +
                    " !" + booleanType + SPACE + rhs.getCode() + END_STMT;


            computation.append("if (").append(lhsNegCode).append(") goto if_then_").append(ifNumber).append(END_STMT); // TODO: change code to negCode to remove short circuit
            computation.append(rhsNeg);
            computation.append("if (").append(rhsNegCode).append(") goto if_then_").append(ifNumber).append(END_STMT);
            computation.append(code).append(SPACE).append(ASSIGN).append(booleanType).append(SPACE)
                    .append("1").append(booleanType).append(END_STMT)
                    .append("goto if_end_").append(ifNumber).append(END_STMT);
            computation.append("if_then_").append(ifNumber).append(": ").append(code).append(SPACE).append(ASSIGN).append(booleanType).append(SPACE)
                    .append("0").append(booleanType).append(END_STMT);
            computation.append("if_end_").append(ifNumber).append(": ");

        }
        else {

            computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE);
            computation.append(lhs.getCode()).append(SPACE);

            Type type = typeUtils.getExprType(binaryExpr);
            computation.append(binaryExpr.get("op")).append(OptUtils.toOllirType(type)).append(SPACE);
            computation.append(rhs.getCode()).append(END_STMT);
        }


        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitIntegerLiteral(JmmNode integerLiteral, Void unused) {
        Type intType = typeUtils.getIntType();
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = integerLiteral.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitBooleanLiteral(JmmNode booleanLiteral, Void unused) {
        Type booleanType = typeUtils.getBooleanType();
        String ollirBooleanType = OptUtils.toOllirType(booleanType);
        String value = booleanLiteral.get("value");
        if (value.equals("true")) {
            return new OllirExprResult(1 + ollirBooleanType);
        }
        return new OllirExprResult(0 + ollirBooleanType);
    }

    private OllirExprResult visitVarRefExpr(JmmNode varRefExpr, Void unused) {
        String name = varRefExpr.get("name");
        Type type = typeUtils.getExprType(varRefExpr);
        String ollirType = OptUtils.toOllirType(type);

        if (typeUtils.isImport(name)) {
            return new OllirExprResult(name);
        }

        if (typeUtils.isLocal(name) || typeUtils.isParameter(name)) {
            String code = name + ollirType;
            return new OllirExprResult(code);
        }

        assert typeUtils.isField(name);

        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        code.append(OptUtils.getTemp()).append(ollirType);

        computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE);
        computation.append("getfield(this, ").append(name).append(ollirType).append(")").append(ollirType).append(END_STMT);

        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitThis(JmmNode this_, Void unused) {
        String name = this_.get("name"); // this
        return new OllirExprResult(name);
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {
        for (var child : node.getChildren()) {
            visit(child);
        }
        return OllirExprResult.EMPTY;
    }
}
