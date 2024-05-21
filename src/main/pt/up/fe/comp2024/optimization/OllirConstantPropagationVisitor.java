package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;

public class OllirConstantPropagationVisitor extends AJmmVisitor<Void, Boolean> {
    @Override
    protected void buildVisitor() {
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        setDefaultVisit(this::defaultVisit);
    }

    private boolean visitAssignStmt(JmmNode assignStmt, Void unused) {
        JmmNode child = assignStmt.getChild(0);
        boolean changes = false;
        if (child.getKind().equals(Kind.INTEGER_LITERAL.toString()) || child.getKind().equals(Kind.BOOLEAN_LITERAL.toString())) {
            String name = assignStmt.get("name");
            JmmNode parent = assignStmt.getParent();
            parent.removeChild(assignStmt);

            for (JmmNode desc : parent.getDescendants(Kind.VAR_REF_EXPR)) {
                if (desc.get("name").equals(name)) {
                    desc.replace(child);
                    changes = true;
                }
            }
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
