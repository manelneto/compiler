package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import javax.lang.model.element.TypeElement;
import javax.naming.AuthenticationNotSupportedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;

    Method currentMethod;

    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(Field.class, this::generateField);
        generators.put(PutFieldInstruction.class, this::generatePutFieldInst);
        generators.put(GetFieldInstruction.class, this::generateGetFieldInst);
        generators.put(CallInstruction.class, this::generateCallInst);
    }


    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }

        return code;
    }


    private String generateClassUnit(ClassUnit classUnit) {

        var code = new StringBuilder();

        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class public ").append(className).append(NL).append(NL);
        var superClassName = ollirResult.getOllirClass().getSuperClass();
        if (superClassName == null || superClassName.equals("Object"))                 // pode ser null ou ""
            superClassName = "java/lang/Object";
        code.append(".super ").append(superClassName).append(NL);

        // generate a single constructor method
        String defaultConstructor = ";default constructor\n" +
                ".method public <init>()V\n" +
                TAB + "aload 0\n" +
                TAB + getSuperClassConstructorCall() +
                TAB + "return\n" +
                ".end method\n";

        for(var field : ollirResult.getOllirClass().getFields()) {
            code.append(generators.apply(field));
        }
        code.append(NL);
        code.append(defaultConstructor);

        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
                continue;
            }

            code.append(generators.apply(method));
        }

        return code.toString();
    }

    private String getSuperClassConstructorCall() {
        var superClassName = ollirResult.getOllirClass().getSuperClass();
        if (superClassName == null || superClassName.equals("Object"))
            superClassName = "java/lang/Object";
        return "invokespecial " + superClassName + ".<init>()V\n";
    }

    private String getConstructorCall(String className, List<String> argsType) {
        var code = new StringBuilder();
        code.append("invokespecial ");
        code.append(className);
        code.append(".<init>(");
        for (var argType : argsType) {
            code.append(argType);
        }
        code.append(")V");
        return code.toString();
    }

    private String getVirtualCall(String className, String methodName, List<String> argsType, String returnType) {
        var code = new StringBuilder();
        code.append("invokevirtual ");
        code.append(className);
        code.append("." + methodName + "(");
        for (var argType : argsType) {
            code.append(argType);
        }
        code.append(")" + returnType);
        return code.toString();
    }

    private String getStaticCall(String className, String methodName, List<String> argsType, String returnType) { // TODO: Repetição do código do getVirtualCall()
        var code = new StringBuilder();
        code.append("invokestatic ");
        code.append(className);
        code.append("." + methodName + "(");
        for (var argType : argsType) {
            code.append(argType);
        }
        code.append(")" + returnType);
        return code.toString();
    }




    private String generateMethod(Method method) {

        // set method
        currentMethod = method;

        var code = new StringBuilder();
        // TODO: Static for main method
        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + (method.isStaticMethod() ? " static" : "") + " " :
                "";

        var methodName = method.getMethodName();

        // TODO: Hardcoded param types and return type, needs to be expanded
        code.append("\n.method ").append(modifier).append(methodName).append("(");

        if (currentMethod.getMethodName().equals("main"))
            code.append("[Ljava/lang/String;");
        else {
            for (var i = 0; i < method.getParams().size(); i++) {
                var param = method.getParam(i);
                var paramType = param.getType().getTypeOfElement();
                code.append(toJasminType(paramType));
            }
        }
        code.append(")");
        code.append(toJasminType(method.getReturnType().getTypeOfElement()));
        code.append(NL);

        // Add limits
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

        for (var inst : method.getInstructions()) {
            /*var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));*/
            for (var i = 0; i < StringLines.getLines(generators.apply(inst)).size(); i++) {
                var result = StringLines.getLines(generators.apply(inst)).get(i);
                if (result.isEmpty())
                    continue;
                code.append(NL + TAB);
                code.append(result);
                if (i != StringLines.getLines(generators.apply(inst)).size())
                    code.append(NL);
            }
            //code.append(instCode);
        }

        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        // TODO: Hardcoded for int type, needs to be expanded

        var operandCode = switch (operand.getType().getTypeOfElement()) {
            case INT32, BOOLEAN -> "istore ";
            case OBJECTREF, CLASS, STRING, ARRAYREF -> "astore "; //TODO: Class pode não estar certo
            case THIS, VOID -> null;
        };
        code.append(operandCode);
        code.append(reg).append(NL);
        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        String instruction;
        var operandType = operand.getType().getTypeOfElement();
        if (operandType.equals(ElementType.INT32) || operandType.equals(ElementType.BOOLEAN)) {
            instruction = "iload";
        }
        else {
            instruction = "aload";
        }
        return instruction + " " + reg + NL;
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation
        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "iadd ";
            case MUL -> "imul ";
            case SUB -> "isub ";
            case DIV -> "idiv ";
            case AND, ANDB -> "iand ";
            case OR,ORB -> "ior ";
            case EQ, NEQ, LTE, GTE -> "icmp ";
            case NOT, NOTB -> "ineg ";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        // TODO: Hardcoded to int return type, needs to be expanded
        var returnCode = "return ";
        if (returnInst.hasReturnValue()) {
            code.append(generators.apply(returnInst.getOperand()));

            returnCode = switch (returnInst.getReturnType().getTypeOfElement()) {
                case INT32, BOOLEAN -> "ireturn ";
                case ARRAYREF, OBJECTREF, CLASS, THIS, STRING-> "areturn ";
                case VOID -> "return ";
            };
        }

        code.append(returnCode).append(NL);
        return code.toString();
    }



    private String generateGetFieldInst(GetFieldInstruction getFieldInstruction) {
        var code = new StringBuilder();
        code.append("aload ");
        code.append(getFieldInstruction.getObject().getParamId());
        code.append(NL);
        code.append("getfield ");
        code.append(currentMethod.getOllirClass().getClassName());
        code.append("/");
        code.append(getFieldInstruction.getField().getName());
        code.append(" ");
        code.append(toJasminType(getFieldInstruction.getField().getType().getTypeOfElement()));
        code.append(NL);

        return code.toString();
    }

    private String generatePutFieldInst(PutFieldInstruction putFieldInstruction) {
        var code = new StringBuilder();
        code.append("aload ");
        code.append(putFieldInstruction.getObject().getParamId());
        code.append(NL);
        code.append(generators.apply(putFieldInstruction.getValue()));
        code.append("putfield ");
        code.append(currentMethod.getOllirClass().getClassName());
        code.append("/");
        code.append(putFieldInstruction.getField().getName());
        code.append(" ");
        code.append(toJasminType(putFieldInstruction.getField().getType().getTypeOfElement()));
        code.append(NL);

        return code.toString();
    }

    private String generateField(Field field) {
        var code = new StringBuilder();
        var modifier = field.getFieldAccessModifier() != AccessModifier.DEFAULT ?
                field.getFieldAccessModifier().name().toLowerCase() + " " :
                "private ";

        var fieldName = field.getFieldName();
        code.append("\n.field ").append(modifier).append(fieldName).append(" ");
        code.append(toJasminType(field.getFieldType().getTypeOfElement())).append(NL);

        return code.toString();
    }

    private String generateCallInst(CallInstruction callInstruction) {
        var code = new StringBuilder();
        var instType = callInstruction.getInvocationType();
        //var className = ollirResult.getOllirClass().getClassName(); // TODO
        String callerString;
        String callerName;
        String callerType;
        String methodName;
        ArrayList<String> argsType;
        String returnType;
        String invocationCode;

        switch (instType) {
            case invokevirtual :
                callerString = callInstruction.getCaller().toString();

                callerType = getCallerType(callerString);
                methodName = getInvokeMethod(callInstruction.getMethodName().toString());
                argsType = getArgsCallInstruction(callInstruction);
                returnType = toJasminType(callInstruction.getReturnType().getTypeOfElement());

                invocationCode = getVirtualCall(callerType, methodName, argsType, returnType);
                code.append(generators.apply(callInstruction.getCaller()));
                code.append(getArgsComputationCallInstruction(callInstruction));
                code.append(invocationCode);

                code.append(NL);
                break;
            case invokeinterface :
                break;
            case invokespecial :
                break;
            case invokestatic :
                callerString = callInstruction.getCaller().toString();

                callerType = getCallerType(callerString); // TODO: estamos a passar o callerName porque o tipo está errado
                callerName = getCallerName(callerString);
                methodName = getInvokeMethod(callInstruction.getMethodName().toString());
                argsType = getArgsCallInstruction(callInstruction);
                returnType = toJasminType(callInstruction.getReturnType().getTypeOfElement());

                invocationCode = getStaticCall(callerName, methodName, argsType, returnType);
                code.append(getArgsComputationCallInstruction(callInstruction));
                code.append(invocationCode);

                code.append(NL);
                break;
            case NEW :

                callerType = getCallerType(callInstruction.getCaller().toString());
                code.append("new ");
                code.append(callerType);
                code.append(NL);
                code.append("dup");
                code.append(NL);
                code.append(getArgsComputationCallInstruction(callInstruction));
                invocationCode = getConstructorCall(callerType, getArgsCallInstruction(callInstruction));
                code.append(invocationCode);
                code.append(NL);
                break;

            case arraylength :
                break;
            case ldc :
                break;
        }
        return code.toString();
    }

    private ArrayList<String> getArgsCallInstruction(CallInstruction callInstruction) {
        var argsType = new ArrayList<String>();
        for (var arg : callInstruction.getArguments()) {
            argsType.add(toJasminType(arg.getType().getTypeOfElement()));
        }
        return argsType;
    }

    private String getArgsComputationCallInstruction (CallInstruction callInstruction) {
        var code = new StringBuilder();
        for (var arg : callInstruction.getArguments()) {
            code.append(generators.apply(arg));
        }
        return code.toString();
    }


    private String toJasminType(ElementType type) {
        return switch(type) {
            case INT32 -> "I";
            case BOOLEAN -> "Z";
            case STRING -> "[Ljava/lang/String;";
            case ARRAYREF -> "[";
            case OBJECTREF -> "[";
            case CLASS,THIS -> null;
            case VOID -> "V";
        };
    }


    private String getCallerName(String caller) {
        String callerName;

        // Define the pattern to match "Operand: name(Type)"
        String pattern = "Operand: (\\w+)\\.";

        // Compile the pattern
        Pattern p = Pattern.compile(pattern);

        // Match the pattern against the input string
        Matcher m = p.matcher(caller);

        if (m.find()) {
            callerName = m.group(1);
        } else {
            callerName = "Not Found";
        }
        return callerName;

    }

    private String getCallerType(String caller) {
        String callerType;

        String pattern = "Operand: \\w+\\.\\w+\\((\\w+)\\)";
        Pattern p = Pattern.compile(pattern);

        Matcher m = p.matcher(caller);

        if (m.find()) {
            callerType = m.group(1);
        } else {
            callerType = "Not Found";
        }
        return callerType;
    }

    private String getInvokeMethod(String method) {
        String methodName;

        // Define the pattern to match "Operand: name(Type)"
        String pattern = "\"(\\w+)\"";

        // Compile the pattern
        Pattern p = Pattern.compile(pattern);

        // Match the pattern against the input string
        Matcher m = p.matcher(method);

        if (m.find()) {
            methodName = m.group(1);
        } else {
            methodName = "Not Found";
        }
        return methodName;
    }
}
