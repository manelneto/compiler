package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Map.entry;

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

    private final Map<String, Integer> stackUsage = Map.ofEntries(
            entry("aload", 1),
            entry("areturn", -1),
            entry("arraylength", 0),
            entry("astore", -1),
            entry("bipush", 1),
            entry("dup", 1),
            entry("getfield", 0),
            entry("goto", 0),
            entry("iadd", -1),
            entry("iaload", -1),
            entry("iand", -1),
            entry("iastore", -3),
            entry("iconst", 1),
            entry("idiv", -1),
            entry("if", -1),
            entry("iinc", 0),
            entry("iload", 1),
            entry("imul", -1),
            entry("ineg", 0),
            entry("invokespecial", 0),
            entry("invokestatic", 1),
            entry("invokevirtual", 0),
            entry("ior", -1),
            entry("ireturn", -1),
            entry("istore", -1),
            entry("isub", -1),
            entry("ixor", -1),
            entry("ldc", 1),
            entry("new", 1),
            entry("newarray", 0),
            entry("pop", -1),
            entry("putfield", -2),
            entry("return", 0),
            entry("sipush", 1)
    );

    private int currentStack = 0;
    private int maxStack = 0;

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
        generators.put(ArrayOperand.class, this::generateArrayOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(UnaryOpInstruction.class, this::generateUnaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(Field.class, this::generateField);
        generators.put(GetFieldInstruction.class, this::generateGetField);
        generators.put(PutFieldInstruction.class, this::generatePutField);
        generators.put(CallInstruction.class, this::generateCall);
        generators.put(GotoInstruction.class, this::generateGoto);
        generators.put(CondBranchInstruction.class, this::generateCondBranch);
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
                TAB + "aload_0\n" +
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

        StringBuilder instructionCode = new StringBuilder();
        for (Instruction instruction : method.getInstructions()) {
            for (String label : method.getLabels(instruction)) {
                instructionCode.append(label).append(":").append(NL);
            }

            instructionCode.append(StringLines.getLines(generators.apply(instruction)).stream().collect(Collectors.joining(NL + TAB, TAB, NL)));

            if (instruction instanceof CallInstruction callInstruction && !callInstruction.getReturnType().getTypeOfElement().equals(ElementType.VOID)) {
                instructionCode.append(TAB + "pop" + NL);
                this.updateStack("pop");
            }
        }

        code.append(TAB).append(".limit stack ").append(this.maxStack).append(NL);

        Descriptor maxRegister = method.getVarTable().values().stream().max(Comparator.comparingInt(Descriptor::getVirtualReg)).orElse(new Descriptor(0));
        int locals = maxRegister.getVirtualReg() + 1;
        code.append(TAB).append(".limit locals ").append(locals).append(NL);

        code.append(instructionCode);
        code.append(".end method").append(NL);

        this.currentMethod = null;
        this.maxStack = 0;

        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        StringBuilder code = new StringBuilder();
        // TODO: assign boolean b = 1 < 2;

        // store value in the stack in destination
        Element lhs = assign.getDest();

        if (!(lhs instanceof Operand operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        if (operand instanceof ArrayOperand arrayOperand) {
            code.append(generateOperand(arrayOperand));
            code.append(generators.apply(arrayOperand.getIndexOperands().get(0)));
            code.append(generators.apply(assign.getRhs()));
            code.append("iastore").append(NL);
            this.updateStack("iastore");
            return code.toString();
        }

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        // get register
        int register = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        String underscoreOrSpace = register >= 0 && register <= 3 ? "_" : " ";

        String operandCode = switch (operand.getType().getTypeOfElement()) {
            case INT32, BOOLEAN -> "istore";
            case OBJECTREF, CLASS, STRING, ARRAYREF -> "astore";
            case THIS, VOID -> null;
        };

        code.append(operandCode).append(underscoreOrSpace).append(register).append(NL);

        this.updateStack(operandCode);
        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        String string = literal.getLiteral();
        int n = Integer.parseInt(string);

        if (n == -1) {
            this.updateStack("iconst");
            return "iconst_m1" + NL;
        }
        if (n >= 0 && n <= 5) {
            this.updateStack("iconst");
            return "iconst_" + string + NL;
        }
        if (n >= -128 && n <= 127) {
            this.updateStack("bipush");
            return "bipush " + string + NL; // byte
        }
        if (n >= -32768 && n <= 32767) {
            this.updateStack("sipush");
            return "sipush " + string + NL; // short
        }
        this.updateStack("ldc");
        return "ldc " + string + NL;
    }

    private String generateOperand(Operand operand) {
        int register = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        String underscoreOrSpace = register >= 0 && register <= 3 ? "_" : " ";

        ElementType operandType = operand.getType().getTypeOfElement();

        if ((operandType.equals(ElementType.INT32) && !(operand instanceof ArrayOperand)) || operandType.equals(ElementType.BOOLEAN)) {
            this.updateStack("iload");
            return "iload" + underscoreOrSpace + register + NL;
        }

        this.updateStack("aload");
        return "aload" + underscoreOrSpace + register + NL;
    }

    private String generateArrayOperand(ArrayOperand arrayOperand) {
        StringBuilder code = new StringBuilder();

        code.append(generateOperand(arrayOperand));
        code.append(generators.apply(arrayOperand.getIndexOperands().get(0)));
        code.append("iaload").append(NL);

        this.updateStack("iaload");
        return code.toString();
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
            case SUB, LTH, GTH, LTE, GTE, EQ, NEQ -> "isub";
            case DIV -> "idiv";
            case AND, ANDB -> "iand";
            case OR, ORB -> "ior";
            case NOT, NOTB -> "ineg";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        this.updateStack(op);
        return code.toString();
    }

    private String generateUnaryOp(UnaryOpInstruction unaryOp) {
        StringBuilder code = new StringBuilder();

        code.append(generators.apply(unaryOp.getOperand()));
        code.append("iconst_1").append(NL);
        code.append("ixor").append(NL);

        this.updateStack("iconst");
        this.updateStack("ixor");
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
        this.updateStack(returnCode);
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
        this.updateStack("aload");

        code.append("getfield ");
        this.updateStack("getfield");
        code.append(currentMethod.getOllirClass().getClassName()).append("/").append(field.getName()).append(" ");
        code.append(toJasminType(field.getType())).append(NL);

        return code.toString();
    }

    private String generatePutField(PutFieldInstruction putFieldInstruction) {
        Operand field = putFieldInstruction.getField();
        int register = field.getParamId();

        StringBuilder code = new StringBuilder();
        code.append("aload ").append(register).append(NL);
        this.updateStack("aload");

        code.append(generators.apply(putFieldInstruction.getValue()));
        code.append("putfield ");
        this.updateStack("putfield");
        code.append(currentMethod.getOllirClass().getClassName()).append("/").append(field.getName()).append(" ");
        code.append(toJasminType(field.getType())).append(NL);

        return code.toString();
    }

    private String generateCall(CallInstruction callInstruction) {
        StringBuilder code = new StringBuilder();
        String invocationCode = "";

        Operand caller = (Operand) callInstruction.getCaller();
        String callerName = getFullName(caller.getName());

        if (callerName.equals("array")) {
            code.append(generators.apply(callInstruction.getArguments().get(0)));
            code.append("newarray int").append(NL);
            this.updateStack("newarray");
            return code.toString();
        }

        if (caller.getType() instanceof ArrayType arrayType) {
            code.append(generators.apply(caller));
            code.append("arraylength").append(NL);
            this.updateStack("arraylength");
            return code.toString();
        }

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

                for (Element argument : callInstruction.getArguments()) {
                    code.append(generators.apply(argument));
                }

                this.updateStack("invokevirtual", argumentsType.size());
                break;

            case invokespecial:
                invocationCode = getCall(invocationType.toString(), callerType, "<init>", argumentsType, "V");

                for (Element argument : callInstruction.getArguments()) {
                    code.append(generators.apply(argument));
                }

                this.updateStack("invokespecial", argumentsType.size());
                break;

            case invokestatic:
                String staticMethod = ((LiteralElement) callInstruction.getMethodName()).getLiteral();
                String staticMethodName = staticMethod.substring(1, staticMethod.length() - 1);

                invocationCode = getCall(invocationType.toString(), callerName, staticMethodName, argumentsType, returnType);

                for (Element argument : callInstruction.getArguments()) {
                    code.append(generators.apply(argument));
                }

                this.updateStack("invokestatic", argumentsType.size());
                break;

            case NEW:
                code.append("new ").append(callerType).append(NL);
                this.updateStack("new");
                code.append("dup");
                this.updateStack("dup");
                break;
        }

        code.append(invocationCode).append(NL);

        return code.toString();
    }

    private String generateGoto(GotoInstruction gotoInstruction) {
        this.updateStack("goto");
        return "goto " + gotoInstruction.getLabel();
    }

    private String generateCondBranch(CondBranchInstruction condBranchInstruction) {
        StringBuilder code = new StringBuilder();

        if (condBranchInstruction instanceof SingleOpCondInstruction singleOpCondInstruction) {
            code.append(generators.apply(singleOpCondInstruction.getCondition()));
            code.append("ifne ").append(condBranchInstruction.getLabel());
        } else {
            // TODO: and (&&) operation
            OpCondInstruction opCondInstruction = (OpCondInstruction) condBranchInstruction;
            OpInstruction condition = opCondInstruction.getCondition();
            code.append(generators.apply(condition.toInstruction()));

            String jump = switch (condition.getOperation().getOpType()) {
                case LTH -> "iflt ";
                case LTE -> "ifle ";
                case GTH -> "ifgt ";
                case GTE -> "ifge ";
                case EQ -> "ifeq ";
                case NEQ -> "ifne ";
                default -> throw new NotImplementedException(condition.getOperation().getOpType());
            };

            code.append(jump).append(condBranchInstruction.getLabel());
        }

        this.updateStack("if");
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
            case ARRAYREF -> "[" + toJasminType(((ArrayType) type).getElementType());
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

    private void updateStack(String instruction) {
        this.currentStack += this.stackUsage.get(instruction);

        if (this.currentStack > this.maxStack) {
            this.maxStack = this.currentStack;
        }
    }

    private void updateStack(String instruction, int argumentsNumber) {
        this.currentStack += this.stackUsage.get(instruction) - argumentsNumber;

        if (this.currentStack > this.maxStack) {
            this.maxStack = this.currentStack;
        }
    }
}
