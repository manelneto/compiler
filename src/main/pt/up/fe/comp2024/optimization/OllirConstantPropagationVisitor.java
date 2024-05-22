package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;

import java.util.ArrayList;
import java.util.HashMap;

public class OllirConstantPropagationVisitor extends AJmmVisitor<Void, Boolean> {
    private final HashMap<String, JmmNode> globalConstants = new HashMap<>();


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

    private boolean visitMethodDecl(JmmNode methodDecl, Void unused) {
        boolean propagated = false;

        for (JmmNode stmt : methodDecl.getChildren()) {
            propagated = visit(stmt) || propagated;
        }

        for (JmmNode constant : this.globalConstants.values()) {
            constant.removeParent();
        }

        return propagated;
    }

    private boolean visitStmtBlock(JmmNode stmtBlock, Void unused) {
        boolean propagated = false;
        ArrayList<String> localConstants = new ArrayList<>();

        for (JmmNode stmt : stmtBlock.getChildren()) {
            if (stmt.getKind().equals(Kind.ASSIGN_STMT.toString())) {
                JmmNode expr = stmt.getChild(0);

                if (expr.getKind().equals(Kind.INTEGER_LITERAL.toString()) || expr.getKind().equals(Kind.BOOLEAN_LITERAL.toString())) {
                    String name = stmt.get("name");
                    this.globalConstants.put(name, expr);
                    localConstants.add(name);
                    expr.removeParent();
                }
            }

            for (JmmNode child : stmt.getChildren()) {
                propagated = visit(child) || propagated;
            }
        }

        for (String constant : localConstants) {
            this.globalConstants.get(constant).removeParent();
            this.globalConstants.remove(constant);
        }

        return propagated;
    }

    private boolean visitIfElseStmt(JmmNode ifElseStmt, Void unused) {
        JmmNode expr = ifElseStmt.getChild(0);
        boolean propagated = visit(expr);

        for (JmmNode ifStmtChild : ifElseStmt.getChild(1).getChildren()) {
            propagated = visit(ifStmtChild) || propagated;

            if (ifStmtChild.getKind().equals(Kind.ASSIGN_STMT.toString())) {
                String name = ifStmtChild.get("name");
                this.globalConstants.remove(name);
            }
        }

        for (JmmNode elseStmtChild : ifElseStmt.getChild(2).getChildren()) {
            propagated = visit(elseStmtChild) || propagated;

            if (elseStmtChild.getKind().equals(Kind.ASSIGN_STMT.toString())) {
                String name = elseStmtChild.get("name");
                this.globalConstants.remove(name);
            }
        }

        return propagated;
    }

    private boolean visitWhileStmt(JmmNode whileStmt, Void unused) {
        boolean propagated = false;

        for (JmmNode whileStmtChild : whileStmt.getChild(1).getChildren()) {
            if (whileStmtChild.getKind().equals(Kind.ASSIGN_STMT.toString())) {
                String name = whileStmtChild.get("name");
                this.globalConstants.remove(name);
            }

            propagated = visit(whileStmtChild) || propagated;
        }

        JmmNode expr = whileStmt.getChild(0);
        return visit(expr) || propagated;
    }

    private boolean visitSimpleStmt(JmmNode simpleStmt, Void unused) {
        return visit(simpleStmt.getChild(0));
    }

    private boolean visitAssignStmt(JmmNode assignStmt, Void unused) {
        JmmNode expr = assignStmt.getChild(0);

        if (expr.getKind().equals(Kind.INTEGER_LITERAL.toString()) || expr.getKind().equals(Kind.BOOLEAN_LITERAL.toString())) {
            this.globalConstants.put(assignStmt.get("name"), expr);
        }

        return visit(expr);
    }

    private boolean visitVarRefExpr(JmmNode varRefExpr, Void unused) {
        String name = varRefExpr.get("name");

        if (this.globalConstants.containsKey(name)) {
            varRefExpr.replace(this.globalConstants.get(name));
            return true;
        }

        return false;
    }

    private boolean defaultVisit(JmmNode node, Void unused) {
        boolean propagated = false;

        for (JmmNode child : node.getChildren()) {
            propagated = visit(child) || propagated;
        }

        return propagated;
    }
/*
    private boolean visitMethodDecl(JmmNode methodDecl, Void unused) {
        this.forbidden = new HashSet<>();
        this.propagatableNodes = new ArrayList<>();

        boolean changes = false;
        for (JmmNode child : methodDecl.getChildren()) {
            changes = visit(child) || changes;
        }

        for (JmmNode node : this.propagatableNodes) {
            if (!this.forbidden.contains(node.get("name"))) {
                JmmNode parent = node.getParent();
                parent.removeChild(node);
            }
        }

        return changes;
    }

    private boolean visitAssignStmt(JmmNode assignStmt, Void unused) {
        JmmNode child = assignStmt.getChild(0);
        boolean changes = visit(child);
        if (child.getKind().equals(Kind.INTEGER_LITERAL.toString()) || child.getKind().equals(Kind.BOOLEAN_LITERAL.toString())) {
            this.propagatableNodes.add(assignStmt);
            this.forbidden.remove(assignStmt.get("name"));
        } else {
            this.forbidden.add(assignStmt.get("name"));
        }

        return changes;
    }

    private boolean visitVarRefExpr(JmmNode varRefExpr, Void unused) {
        boolean changes = false;
        if (this.forbidden.contains(varRefExpr.get("name")))
            return false;

        for (int i = this.propagatableNodes.size() - 1; i >= 0; i--) {
            JmmNode assignNode = this.propagatableNodes.get(i);
            if (assignNode.get("name").equals(varRefExpr.get("name"))) {
                JmmNode newNode = assignNode.getChild(0);

                varRefExpr.replace(newNode);
                changes = true;
                break;
            }
        }

        return changes;
    }

    private boolean visitWhileStmt(JmmNode whileStmt, Void unused) {
        for (JmmNode assign : whileStmt.getDescendants(Kind.ASSIGN_STMT)) {
            this.forbidden.add(assign.get("name"));
        }

        boolean changes = false;
        for (var child : whileStmt.getChildren()) {
            changes = visit(child) || changes;
        }

        for (JmmNode assign : whileStmt.getDescendants(Kind.ASSIGN_STMT)) {
            this.forbidden.add(assign.get("name"));
        }

        return changes;
    }

    private boolean visitIfElseStmt(JmmNode ifElseStmt, Void unused) {
        boolean changes = visit(ifElseStmt.getChild(0));

        for (int i = 1; i < ifElseStmt.getChildren().size(); i++) {
            JmmNode stmtBlock = ifElseStmt.getChild(i);

            changes = visit(stmtBlock) || changes;

            for (JmmNode assign : stmtBlock.getDescendants(Kind.ASSIGN_STMT)) {
                this.forbidden.add(assign.get("name"));
            }
        }

        return changes;
    }*/


    /**
     * Default visitor. Visits every child node and return an empty string.
     */
    /*private boolean defaultVisit(JmmNode node, Void unused) {
        boolean changes = false;
        for (var child : node.getChildren()) {
            changes = visit(child) || changes;
        }
        return changes;
    }*/
}
