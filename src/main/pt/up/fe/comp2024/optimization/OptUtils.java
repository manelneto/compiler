package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import static pt.up.fe.comp2024.ast.Kind.TYPE;

public class OptUtils {
    private static int tempNumber = -1;
    private static int whileNumber = -1;
    private static int ifNumber = -1;

    public static String getWhileNumber() {
        whileNumber += 1;
        return String.valueOf(whileNumber);
    }
    public static String getIfNumber() {
        ifNumber += 1;
        return String.valueOf(ifNumber);
    }


    public static String getTemp() {

        return getTemp("tmp");
    }

    public static String getTemp(String prefix) {

        return prefix + getNextTempNum();
    }

    public static int getNextTempNum() {

        tempNumber += 1;
        return tempNumber;
    }

    public static String toOllirType(JmmNode typeNode) {
        TYPE.checkOrThrow(typeNode);

        String typeName = typeNode.get("name");
        boolean isArray = typeNode.getObject("isArray", Boolean.class);

        return toOllirType(typeName, isArray);
    }

    public static String toOllirType(Type type) {
        return toOllirType(type.getName(), type.isArray());
    }

    private static String toOllirType(String typeName, boolean isArray) {
        String ollirType = isArray ? ".array" : "";

        return ollirType + "." + switch (typeName) {
            case "boolean" -> "bool";
            case "int" -> "i32";
            case "void" -> "V";
            case "String" -> "String";
            default -> typeName;
        };
    }
}
