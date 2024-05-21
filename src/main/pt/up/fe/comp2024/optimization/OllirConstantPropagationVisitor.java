package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;

import java.util.ArrayList;

public class OllirConstantPropagationVisitor extends AJmmVisitor<Void, Boolean> {
    private ArrayList<String> forbidden;
    private ArrayList<JmmNode> propagatableNodes;
    private ArrayList<Boolean> changed;

    @Override
    protected void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(Kind.IF_ELSE_STMT, this::visitIfElseStmt);
        addVisit(Kind.WHILE_STMT, this::visitIfElseStmt);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
        setDefaultVisit(this::defaultVisit);
    }

    private boolean visitMethodDecl(JmmNode methodDecl, Void unused) {
        this.forbidden = new ArrayList<>();
        this.propagatableNodes = new ArrayList<>();
        this.changed = new ArrayList<>();
        boolean changes = false;
        for (JmmNode child : methodDecl.getChildren()) {
            changes = visit(child) || changes;
        }

        for (int i = 0; i < this.propagatableNodes.size(); i++) {
            boolean propagated = this.changed.get(i);
            JmmNode node = this.propagatableNodes.get(i);
            if (propagated && !this.forbidden.contains(node.get("name"))) {
                JmmNode parent = node.getParent();
                parent.removeChild(node);
            }
        }

        return changes;
    }

    private boolean visitAssignStmt(JmmNode assignStmt, Void unused) {
        boolean changes = false;

        JmmNode child = assignStmt.getChild(0);
        if (child.getKind().equals(Kind.INTEGER_LITERAL.toString()) || child.getKind().equals(Kind.BOOLEAN_LITERAL.toString())) {
            //String name = assignStmt.get("name");
            //JmmNode parent = assignStmt.getParent();
            //parent.removeChild(assignStmt);
            this.propagatableNodes.add(assignStmt);
            this.changed.add(false);
        }

        changes = visit(child);

        return changes;
    }

    private boolean visitVarRefExpr(JmmNode varRefExpr, Void unused) {
        boolean changes = false;
        if (this.forbidden.contains(varRefExpr.get("name"))) return false;
        for (int i = 0; i < this.propagatableNodes.size(); i++) {
            JmmNode assignNode = this.propagatableNodes.get(i);
            if (assignNode.get("name").equals(varRefExpr.get("name"))) {
                JmmNode newNode = assignNode.getChild(0);
                JmmNode varRefParent = varRefExpr.getParent();

                varRefParent.add(newNode);
                varRefParent.removeChild(varRefExpr);
                this.changed.set(i, true);
                changes = true;
                break;
            }
        }

        return changes;
    }

    private boolean visitIfElseStmt(JmmNode ifElseStmt, Void unused) {
        for (JmmNode assign : ifElseStmt.getDescendants(Kind.ASSIGN_STMT)) {
            this.forbidden.add(assign.get("name"));
        }

        boolean changes = false;
        for (var child : ifElseStmt.getChildren()) {
            changes = visit(child) || changes;
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
