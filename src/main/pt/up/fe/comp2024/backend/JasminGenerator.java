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
        generators.put(GetFieldInstruction.class, this::generateGetField);
        generators.put(PutFieldInstruction.class, this::generatePutField);
        generators.put(CallInstruction.class, this::generateCall);
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

        String superClassName = ollirResult.getOllirClass().getSuperClass();
        if (superClassName == null || superClassName.equals("Object"))
            superClassName = "java/lang/Object";
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
            case ADD -> "iadd ";
            case MUL -> "imul ";
            case SUB -> "isub ";
            case DIV -> "idiv ";
            case AND, ANDB -> "iand ";
            case OR, ORB -> "ior ";
            case EQ, NEQ, LTE, GTE -> "icmp ";
            case NOT, NOTB -> "ineg ";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        StringBuilder code = new StringBuilder();

        if (returnInst.hasReturnValue()) {
            code.append(generators.apply(returnInst.getOperand()));
        }

        String returnCode = switch (returnInst.getReturnType().getTypeOfElement()) {
            case INT32, BOOLEAN -> "ireturn ";
            case ARRAYREF, OBJECTREF, CLASS, THIS, STRING -> "areturn ";
            case VOID -> "return ";
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
        currentMethod.getVarTable().get(field.getName()).setVirtualReg(register);

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
        var code = new StringBuilder();
        var instType = callInstruction.getInvocationType();
        //var className = ollirResult.getOllirClass().getClassName(); // TODO
        ArrayList<String> argsType;
        String returnType;
        String invocationCode;

        Operand caller = (Operand) (callInstruction.getCaller());
        String callerName = caller.getName();
        String callerType = ((ClassType) caller.getType()).getName();

        if (callInstruction.hasChildren())
            switch (instType) {
                case invokevirtual:
                    String virtualMethod = ((LiteralElement) callInstruction.getMethodName()).getLiteral();
                    String virtualMethodName = virtualMethod.substring(1, virtualMethod.length() - 1);

                    argsType = getArgsCallInstruction(callInstruction);
                    returnType = toJasminType(callInstruction.getReturnType());

                    invocationCode = getCall("invokevirtual", callerType, virtualMethodName, argsType, returnType);
                    code.append(generators.apply(callInstruction.getCaller()));
                    code.append(getArgsComputationCallInstruction(callInstruction));
                    code.append(invocationCode);
                    break;
                case invokestatic:
                    String staticMethod = ((LiteralElement) callInstruction.getMethodName()).getLiteral();
                    String staticMethodName = staticMethod.substring(1, staticMethod.length() - 1);

                    argsType = getArgsCallInstruction(callInstruction);
                    returnType = toJasminType(callInstruction.getReturnType());

                    invocationCode = getCall("invokestatic", callerName, staticMethodName, argsType, returnType);
                    code.append(getArgsComputationCallInstruction(callInstruction));
                    code.append(invocationCode);
                    break;
                case NEW:
                    code.append("new ");
                    code.append(callerType);
                    code.append(NL);
                    code.append("dup");
                    code.append(NL);
                    code.append(getArgsComputationCallInstruction(callInstruction));
                    invocationCode = getCall("invokespecial", callerType, "<init>", getArgsCallInstruction(callInstruction), "V");
                    code.append(invocationCode);
                    break;
            }

        code.append(NL);

        return code.toString();
    }

    private ArrayList<String> getArgsCallInstruction(CallInstruction callInstruction) {
        var argsType = new ArrayList<String>();
        for (var arg : callInstruction.getArguments()) {
            argsType.add(toJasminType(arg.getType()));
        }
        return argsType;
    }

    private String getArgsComputationCallInstruction(CallInstruction callInstruction) {
        var code = new StringBuilder();
        for (var arg : callInstruction.getArguments()) {
            code.append(generators.apply(arg));
        }
        return code.toString();
    }


    private String toJasminType(Type type) {
        return switch (type.getTypeOfElement()) {
            case INT32 -> "I";
            case BOOLEAN -> "Z";
            case ARRAYREF -> "[Ljava/lang/String;";
            case OBJECTREF, CLASS -> getClassName((ClassType) type);
            case THIS -> null;
            case STRING -> "Ljava/lang/String;";
            case VOID -> "V";
        };
    }

    private String getCall(String call, String className, String methodName, List<String> argsType, String returnType) {
        StringBuilder code = new StringBuilder();
        code.append(call).append(" ");
        code.append(className).append(".");
        code.append(methodName).append("(");
        for (String argType : argsType) {
            code.append(argType);
        }
        code.append(")").append(returnType);
        return code.toString();
    }

    private String getClassName(ClassType type) {
        String shortName = type.getName();

        StringBuilder fullName = new StringBuilder("L");

        for (String importName : ollirResult.getOllirClass().getImports()) {
            if (importName.endsWith(shortName)) {
                String path = importName.replace(".", "/");
                fullName.append(path);
            }
        }

        fullName.append(";");

        return fullName.toString();
    }
}
