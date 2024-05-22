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
        boolean propagated = false;
        String name = assignStmt.get("name");
        JmmNode expr = assignStmt.getChild(0);

        if (expr.getKind().equals(Kind.INTEGER_LITERAL.toString()) || expr.getKind().equals(Kind.BOOLEAN_LITERAL.toString())) {
            this.globalConstants.put(name, expr);
        } else {
            propagated = visit(expr);
            this.globalConstants.remove(name);
        }

        return propagated;
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
}
