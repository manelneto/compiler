package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;

import java.util.HashMap;
import java.util.Optional;

public class OllirConstantPropagationVisitor extends AJmmVisitor<HashMap<String, JmmNode>, Boolean> {
    @Override
    protected void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.STMT_BLOCK, this::visitStmtBlock);
        addVisit(Kind.IF_ELSE_STMT, this::visitIfElseStmt);
        addVisit(Kind.WHILE_STMT, this::visitWhileStmt);
        addVisit(Kind.SIMPLE_STMT, this::visitSimpleStmt);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
        setDefaultVisit(this::defaultVisit);
    }

    private boolean visitMethodDecl(JmmNode methodDecl, HashMap<String, JmmNode> unused) {
        boolean propagated = false;

        HashMap<String, JmmNode> constants = new HashMap<>();

        for (JmmNode stmt : methodDecl.getChildren()) {
            if (stmt.getKind().equals(Kind.ASSIGN_STMT.toString())) {
                JmmNode expr = stmt.getChild(0);
                if (expr.getKind().equals(Kind.INTEGER_LITERAL.toString()) || expr.getKind().equals(Kind.BOOLEAN_LITERAL.toString())) {
                    constants.put(stmt.get("name"), expr);
                }
            }

            propagated = visit(stmt, constants) || propagated;
        }

        for (JmmNode constant : constants.values()) {
            Optional<JmmNode> optional = constant.getAncestor(Kind.ASSIGN_STMT);
            if (optional.isPresent()) {
                JmmNode assign = optional.get();
                assign.getParent().removeChild(assign.getIndexOfSelf());
            }
        }

        return propagated;
    }

    private boolean visitStmtBlock(JmmNode stmtBlock, HashMap<String, JmmNode> constants) {
        boolean propagated = false;
        HashMap<String, JmmNode> localConstants = new HashMap<>();

        for (JmmNode stmt : stmtBlock.getChildren()) {
            if (stmt.getKind().equals(Kind.ASSIGN_STMT.toString())) {
                String name = stmt.get("name");
                constants.remove(name);

                JmmNode expr = stmt.getChild(0);
                if (expr.getKind().equals(Kind.INTEGER_LITERAL.toString()) || expr.getKind().equals(Kind.BOOLEAN_LITERAL.toString())) {
                    constants.put(name, expr);
                    localConstants.put(name, expr);
                }
            }

            propagated = visit(stmt, constants) || propagated;
        }

        for (String localConstant : localConstants.keySet()) {
            constants.remove(localConstant);
        }

        return propagated;
    }

    private boolean visitIfElseStmt(JmmNode ifElseStmt, HashMap<String, JmmNode> constants) {
        HashMap<String, JmmNode> ifConstants = new HashMap<>(constants);
        HashMap<String, JmmNode> elseConstants = new HashMap<>(constants);

        boolean propagated = visit(ifElseStmt.getChild(0), constants);
        propagated = visit(ifElseStmt.getChild(1), ifConstants) || propagated;
        propagated = visit(ifElseStmt.getChild(2), elseConstants) || propagated;

        constants.clear();

        for (String constant : ifConstants.keySet()) {
            if (elseConstants.containsKey(constant)) {
                constants.put(constant, ifConstants.get(constant));
            }
        }

        return propagated;
    }

    private boolean visitWhileStmt(JmmNode whileStmt, HashMap<String, JmmNode> constants) {
        boolean propagated = false;
        HashMap<String, JmmNode> localConstants = new HashMap<>();

        for (JmmNode assignStmt : whileStmt.getChild(1).getDescendants(Kind.ASSIGN_STMT)) {
            constants.remove(assignStmt.get("name"));
        }

        for (JmmNode whileStmtChild : whileStmt.getChild(1).getChildren()) {
            if (whileStmtChild.getKind().equals(Kind.ASSIGN_STMT.toString())) {
                String name = whileStmtChild.get("name");

                JmmNode expr = whileStmtChild.getChild(0);
                if (expr.getKind().equals(Kind.INTEGER_LITERAL.toString()) || expr.getKind().equals(Kind.BOOLEAN_LITERAL.toString())) {
                    constants.put(name, expr);
                    localConstants.put(name, expr);
                }
            }

            propagated = visit(whileStmtChild, constants) || propagated;
        }

        for (String localConstant : localConstants.keySet()) {
            constants.remove(localConstant);
        }

        JmmNode expr = whileStmt.getChild(0);
        return visit(expr, constants) || propagated;
    }

    private boolean visitSimpleStmt(JmmNode simpleStmt, HashMap<String, JmmNode> constants) {
        return visit(simpleStmt.getChild(0), constants);
    }

    private boolean visitAssignStmt(JmmNode assignStmt, HashMap<String, JmmNode> constants) {
        return visit(assignStmt.getChild(0), constants);
    }

    private boolean visitVarRefExpr(JmmNode varRefExpr, HashMap<String, JmmNode> constants) {
        String name = varRefExpr.get("name");

        if (constants.containsKey(name)) {
            varRefExpr.replace(constants.get(name));
            return true;
        }

        return false;
    }

    private boolean defaultVisit(JmmNode node, HashMap<String, JmmNode> constants) {
        boolean propagated = false;

        for (JmmNode child : node.getChildren()) {
            propagated = visit(child, constants) || propagated;
        }

        return propagated;
    }
}
