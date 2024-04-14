package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;

import java.util.List;

public class Statements extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.STMT_BLOCK, this::visitStmtBlock);
        addVisit(Kind.IF_ELSE_STMT, this::visitIfElseStmt);
        addVisit(Kind.WHILE_STMT, this::visitWhileStmt);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        String currentMethod = method.get("name");

        if (method.getObject("isVoid", Boolean.class)) {
            return null;
        }

        List<JmmNode> children = method.getChildren();
        if (children.stream().filter(child -> child.getKind().equals(Kind.RETURN_STMT.toString())).count() != 1) {
            reportError(String.format("Method '%s' must contain exactly one return statement.", currentMethod), method);
            return null;
        }

        if (!children.get(children.size() - 1).getKind().equals(Kind.RETURN_STMT.toString())) {
            reportError(String.format("Method '%s' must end with a return statement.", currentMethod), method);
            return null;
        }

        return null;
    }

    private Void visitStmtBlock(JmmNode stmtBlock, SymbolTable table) {
        List<JmmNode> children = stmtBlock.getChildren();

        if (children.stream().noneMatch(child -> child.getKind().equals(Kind.RETURN_STMT.toString()))) {
            return null;
        }

        reportError("Statement block must not contain return statement.", stmtBlock);

        return null;
    }

    private Void visitIfElseStmt(JmmNode ifElseStmt, SymbolTable table) {
        List<JmmNode> children = ifElseStmt.getChildren();

        if (children.stream().noneMatch(child -> child.getKind().equals(Kind.RETURN_STMT.toString()))) {
            return null;
        }

        reportError("If-Else statement must not contain return statement.", ifElseStmt);

        return null;
    }

    private Void visitWhileStmt(JmmNode whileStmt, SymbolTable table) {
        List<JmmNode> children = whileStmt.getChildren();

        if (children.stream().noneMatch(child -> child.getKind().equals(Kind.RETURN_STMT.toString()))) {
            return null;
        }

        reportError("While statement must not contain return statement.", whileStmt);

        return null;
    }
}
