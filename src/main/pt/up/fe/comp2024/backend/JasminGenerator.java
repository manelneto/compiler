package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.List;
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

        ollirResult.getOllirClass().getImports().add("java/lang/Object");

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
        generators.put(UnaryOpInstruction.class, this::generateUnaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(Field.class, this::generateField);
        generators.put(GetFieldInstruction.class, this::generateGetField);
        generators.put(PutFieldInstruction.class, this::generatePutField);
        generators.put(CallInstruction.class, this::generateCall);
        //generators.put(GotoInstruction.class, this::generateGoto);
        //generators.put(CondBranchInstruction.class, this::generateCondBranch);
        //generators.put(SingleOpCondInstruction.class, this::generateSingleOpCond);
        //generators.put(OpCondInstruction.class, this::generateOpCond);
        //generators.put(ArrayOperand.class, this::generateArrayOp);
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
        StringBuilder code = new StringBuilder();

        String className = ollirResult.getOllirClass().getClassName();
        code.append(".class public ").append(className).append(NL).append(NL);

        String superClass = ollirResult.getOllirClass().getSuperClass();
        if (superClass == null)
            superClass = "Object";
        String superClassName = getFullName(superClass);
        code.append(".super ").append(superClassName).append(NL);

        String defaultConstructor = ";default constructor\n" +
                ".method public <init>()V\n" +
                TAB + "aload 0\n" +
                TAB + "invokespecial " + superClassName + ".<init>()V\n" +
                TAB + "return\n" +
                ".end method\n";

        for (Field field : ollirResult.getOllirClass().getFields()) {
            code.append(generators.apply(field));
        }
        code.append(NL);
        code.append(defaultConstructor);

        for (Method method : ollirResult.getOllirClass().getMethods()) {
            if (method.isConstructMethod()) {
                // Ignore constructor, since there is always one constructor that receives no arguments, and has been already added
                continue;
            }
            code.append(generators.apply(method));
        }

        return code.toString();
    }

    private String generateMethod(Method method) {
        currentMethod = method;

        StringBuilder code = new StringBuilder();

        String modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + (method.isStaticMethod() ? " static" : "") + " " :
                "";

        String methodName = method.getMethodName();

        code.append("\n.method ").append(modifier).append(methodName).append("(");

        for (Element param : method.getParams()) {
            Type type = param.getType();
            code.append(toJasminType(type));
        }

        String returnType = toJasminType(method.getReturnType());
        code.append(")").append(returnType).append(NL);

        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

        for (Instruction inst : method.getInstructions()) {
            String instCode = StringLines.getLines(generators.apply(inst)).stream().collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);

            if (inst instanceof CallInstruction callInstruction && !callInstruction.getReturnType().getTypeOfElement().equals(ElementType.VOID)) {
                code.append(TAB + "pop" + NL);
            }
        }

        code.append(".end method").append(NL);

        currentMethod = null;

        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        StringBuilder code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        // store value in the stack in destination
        Element lhs = assign.getDest();

        if (!(lhs instanceof Operand operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        // get register
        int register = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        String operandCode = switch (operand.getType().getTypeOfElement()) {
            case INT32, BOOLEAN -> "istore ";
            case OBJECTREF, CLASS, STRING, ARRAYREF -> "astore ";
            case THIS, VOID -> null;
        };

        code.append(operandCode).append(register).append(NL);

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
        int register = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        ElementType operandType = operand.getType().getTypeOfElement();
        if (operandType.equals(ElementType.INT32) || operandType.equals(ElementType.BOOLEAN)) {
            return "iload " + register + NL;
        }

        return "aload " + register + NL;
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        StringBuilder code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation
        String op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "iadd";
            case MUL -> "imul";
            case SUB -> "isub";
            case DIV -> "idiv";
            case AND, ANDB -> "iand";
            case OR, ORB -> "ior";
            case EQ, NEQ, LTE, GTE -> "icmp";
            case NOT, NOTB -> "ineg";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateUnaryOp(UnaryOpInstruction unaryOp) {
        StringBuilder code = new StringBuilder();

        code.append(generators.apply(unaryOp.getOperand()));
        code.append("iconst_1").append(NL);
        code.append("ixor").append(NL);                 // TODO: confirmar se xor com 1 é a negação

        return code.toString();
    }
    private String generateReturn(ReturnInstruction returnInst) {
        StringBuilder code = new StringBuilder();

        if (returnInst.hasReturnValue()) {
            code.append(generators.apply(returnInst.getOperand()));
        }

        String returnCode = switch (returnInst.getReturnType().getTypeOfElement()) {
            case INT32, BOOLEAN -> "ireturn";
            case ARRAYREF, OBJECTREF, CLASS, THIS, STRING -> "areturn";
            case VOID -> "return";
        };

        code.append(returnCode).append(NL);
        return code.toString();
    }

    private String generateField(Field field) {
        StringBuilder code = new StringBuilder();
        String modifier = field.getFieldAccessModifier() != AccessModifier.DEFAULT ?
                field.getFieldAccessModifier().name().toLowerCase() + " " :
                "private ";

        String fieldName = field.getFieldName();
        code.append("\n.field ").append(modifier).append(fieldName).append(" ");
        code.append(toJasminType(field.getFieldType())).append(NL);

        return code.toString();
    }

    private String generateGetField(GetFieldInstruction getFieldInstruction) {
        Operand field = getFieldInstruction.getField();
        int register = field.getParamId();

        StringBuilder code = new StringBuilder();
        code.append("aload ").append(register).append(NL);

        code.append("getfield ");
        code.append(currentMethod.getOllirClass().getClassName()).append("/").append(field.getName()).append(" ");
        code.append(toJasminType(field.getType())).append(NL);

        return code.toString();
    }

    private String generatePutField(PutFieldInstruction putFieldInstruction) {
        Operand field = putFieldInstruction.getField();
        int register = field.getParamId();

        StringBuilder code = new StringBuilder();
        code.append("aload ").append(register).append(NL);
        code.append(generators.apply(putFieldInstruction.getValue()));
        code.append("putfield ");
        code.append(currentMethod.getOllirClass().getClassName()).append("/").append(field.getName()).append(" ");
        code.append(toJasminType(field.getType())).append(NL);

        return code.toString();
    }

    private String generateCall(CallInstruction callInstruction) {
        StringBuilder code = new StringBuilder();
        String invocationCode = "";

        Operand caller = (Operand) callInstruction.getCaller();
        String callerName = getFullName(caller.getName());

        ClassType callerClass = (ClassType) caller.getType();
        String callerType = getFullName(callerClass.getName());

        CallType invocationType = callInstruction.getInvocationType();
        ArrayList<String> argumentsType = new ArrayList<>();
        for (Element argument : callInstruction.getArguments()) {
            argumentsType.add(toJasminType(argument.getType()));
        }

        String returnType = toJasminType(callInstruction.getReturnType());

        switch (invocationType) {
            case invokevirtual:
                String virtualMethod = ((LiteralElement) callInstruction.getMethodName()).getLiteral();
                String virtualMethodName = virtualMethod.substring(1, virtualMethod.length() - 1);

                invocationCode = getCall(invocationType.toString(), callerType, virtualMethodName, argumentsType, returnType);
                code.append(generators.apply(callInstruction.getCaller()));
                break;

            case invokespecial:
                invocationCode = getCall(invocationType.toString(), callerType, "<init>", argumentsType, "V");
                break;

            case invokestatic:
                String staticMethod = ((LiteralElement) callInstruction.getMethodName()).getLiteral();
                String staticMethodName = staticMethod.substring(1, staticMethod.length() - 1);

                invocationCode = getCall(invocationType.toString(), callerName, staticMethodName, argumentsType, returnType);
                break;

            case NEW:
                code.append("new ").append(callerType).append(NL);
                code.append("dup");
                break;
        }

        for (Element argument : callInstruction.getArguments()) {
            code.append(generators.apply(argument));
        }
        code.append(invocationCode).append(NL);

        return code.toString();
    }

    private String getCall(String invocationType, String className, String methodName, List<String> argumentsType, String returnType) {
        StringBuilder code = new StringBuilder();
        code.append(invocationType).append(" ");
        code.append(className).append(".");
        code.append(methodName).append("(");
        for (String argumentType : argumentsType) {
            code.append(argumentType);
        }
        code.append(")").append(returnType);
        return code.toString();
    }

    private String toJasminType(Type type) {
        return switch (type.getTypeOfElement()) {
            case INT32 -> "I";
            case BOOLEAN -> "Z";
            case ARRAYREF -> "[Ljava/lang/String;"; // TODO: CP3
            case OBJECTREF, CLASS -> "L" + getFullName(((ClassType) type).getName()) + ";";
            case THIS -> null;
            case STRING -> "Ljava/lang/String;";
            case VOID -> "V";
        };
    }

    private String getFullName(String shortName) {
        for (String importName : ollirResult.getOllirClass().getImports()) {
            if (importName.endsWith(shortName)) {
                return importName.replace(".", "/");
            }
        }

        return shortName;
    }
}
