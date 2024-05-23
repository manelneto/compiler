package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2024.ast.Kind;

public class OllirConstantFoldingVisitor extends AJmmVisitor<Void, Boolean> {
    @Override
    protected void buildVisitor() {
        addVisit(Kind.PAREN_EXPR, this::visitParenExpr);
        addVisit(Kind.UNARY_EXPR, this::visitUnaryExpr);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        setDefaultVisit(this::defaultVisit);
    }

    private boolean visitParenExpr(JmmNode parentExpr, Void unused) {
        JmmNode expr = parentExpr.getChild(0);

        if (expr.getKind().equals(Kind.INTEGER_LITERAL.toString()) || expr.getKind().equals(Kind.BOOLEAN_LITERAL.toString())) {
            parentExpr.replace(expr);
            return true;
        }

        return visit(expr);
    }

    private boolean visitUnaryExpr(JmmNode unaryExpr, Void unused) {
        JmmNode expr = unaryExpr.getChild(0);

        if (expr.getKind().equals(Kind.BOOLEAN_LITERAL.toString())) {
            String value = expr.get("value");
            JmmNode newNode = new JmmNodeImpl(Kind.BOOLEAN_LITERAL.toString());
            if (value.equals("true")) {
                newNode.putObject("value", "false");
            } else {
                newNode.putObject("value", "true");
            }
            unaryExpr.replace(newNode);
            return true;
        }

        return visit(expr);
    }

    private boolean visitBinaryExpr(JmmNode binaryExpr, Void unused) {
        JmmNode left = binaryExpr.getChild(0);
        JmmNode right = binaryExpr.getChild(1);

        if ((left.getKind().equals(Kind.INTEGER_LITERAL.toString()) || left.getKind().equals(Kind.BOOLEAN_LITERAL.toString())) && right.getKind().equals(left.getKind())) {
            int intResult;
            boolean boolResult;
            JmmNode newNode;
            switch (binaryExpr.get("op")) {
                case "*":
                    intResult = Integer.parseInt(left.get("value")) * Integer.parseInt(right.get("value"));
                    newNode = new JmmNodeImpl(Kind.INTEGER_LITERAL.toString());
                    newNode.putObject("value", intResult);
                    break;
                case "/":
                    intResult = Integer.parseInt(left.get("value")) / Integer.parseInt(right.get("value"));
                    newNode = new JmmNodeImpl(Kind.INTEGER_LITERAL.toString());
                    newNode.putObject("value", intResult);
                    break;
                case "+":
                    intResult = Integer.parseInt(left.get("value")) + Integer.parseInt(right.get("value"));
                    newNode = new JmmNodeImpl(Kind.INTEGER_LITERAL.toString());
                    newNode.putObject("value", intResult);
                    break;
                case "-":
                    intResult = Integer.parseInt(left.get("value")) - Integer.parseInt(right.get("value"));
                    newNode = new JmmNodeImpl(Kind.INTEGER_LITERAL.toString());
                    newNode.putObject("value", intResult);
                    break;
                case "<":
                    boolResult = Integer.parseInt(left.get("value")) < Integer.parseInt(right.get("value"));
                    newNode = new JmmNodeImpl(Kind.BOOLEAN_LITERAL.toString());
                    newNode.putObject("value", boolResult);
                    break;
                case "&&":
                    boolResult = Boolean.parseBoolean(left.get("value")) && Boolean.parseBoolean(right.get("value"));
                    newNode = new JmmNodeImpl(Kind.BOOLEAN_LITERAL.toString());
                    newNode.putObject("value", boolResult);
                    break;
                default:
                    throw new RuntimeException("Unhandled op: " + binaryExpr.get("op"));
            }
            binaryExpr.replace(newNode);
            return true;
        }

        boolean folded = false;

        for (JmmNode child : binaryExpr.getChildren()) {
            folded = visit(child) || folded;
        }

        return folded;
    }

    private boolean defaultVisit(JmmNode node, Void unused) {
        boolean folded = false;

        for (var child : node.getChildren()) {
            folded = visit(child) || folded;
        }

        return folded;
    }
}
