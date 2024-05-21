package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;

public class OllirOptimizationVisitor extends AJmmVisitor<Void, String> {

    private final SymbolTable symbolTable;

    public OllirOptimizationVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    @Override
    protected void buildVisitor() {
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        setDefaultVisit(this::defaultVisit);
    }

    private String visitAssignStmt(JmmNode assignStmt, Void unused) {
        JmmNode child = assignStmt.getChild(0);
        if (child.getKind().equals(Kind.INTEGER_LITERAL.toString()) || child.getKind().equals(Kind.BOOLEAN_LITERAL.toString())) {
            String name = assignStmt.get("name");
            JmmNode parent = assignStmt.getParent();
            parent.removeChild(assignStmt);

            for (JmmNode desc : parent.getDescendants(Kind.VAR_REF_EXPR)) {
                if (desc.get("name").equals(name)) {
                    desc.replace(child);
                }
            }
        }

        return "";
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     */
    private String defaultVisit(JmmNode node, Void unused) {
        for (var child : node.getChildren()) {
            visit(child);
        }
        return "";
    }
}
