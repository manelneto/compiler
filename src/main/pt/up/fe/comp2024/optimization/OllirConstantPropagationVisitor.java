package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;

import java.util.ArrayList;
import java.util.HashSet;

public class OllirConstantPropagationVisitor extends AJmmVisitor<Void, Boolean> {
    private HashSet<String> forbidden;
    private ArrayList<JmmNode> propagatableNodes;

    @Override
    protected void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(Kind.IF_ELSE_STMT, this::visitConditionalStmt);
        addVisit(Kind.WHILE_STMT, this::visitConditionalStmt);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
        setDefaultVisit(this::defaultVisit);
    }

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

    private boolean visitConditionalStmt(JmmNode conditionalStmt, Void unused) {
        for (JmmNode assign : conditionalStmt.getDescendants(Kind.ASSIGN_STMT)) {
            this.forbidden.add(assign.get("name"));
        }

        boolean changes = false;
        for (var child : conditionalStmt.getChildren()) {
            changes = visit(child) || changes;
        }

        for (JmmNode assign : conditionalStmt.getDescendants(Kind.ASSIGN_STMT)) {
            this.forbidden.add(assign.get("name"));
        }

        return changes;
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     */
    private boolean defaultVisit(JmmNode node, Void unused) {
        boolean changes = false;
        for (var child : node.getChildren()) {
            changes = visit(child) || changes;
        }
        return changes;
    }
}
