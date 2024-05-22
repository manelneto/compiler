package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2024.ast.Kind;

import java.util.ArrayList;
import java.util.HashMap;

public class OllirRegisterAllocationVisitor extends AJmmVisitor<Void, Boolean> {
    private final OllirResult ollirResult;
    private final int size;
    private final ArrayList<String> name;
    private final HashMap<String, Integer> next;
    private final ArrayList<Boolean> free;
    private final ArrayList<Integer> stack;
    private int stackTop;

    public OllirRegisterAllocationVisitor(OllirResult ollirResult, int registers) {
        this.ollirResult = ollirResult;
        this.size = registers;
        this.name = new ArrayList<>();
        this.next = new HashMap<>();
        this.free = new ArrayList<>();
        this.stack = new ArrayList<>();
        this.stackTop = -1;
    }

    @Override
    protected void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
    }

    private boolean visitMethodDecl(JmmNode methodDecl, Void unused) {
        return false;
    }

    private void initialize() {
        for (int i = this.size - 1; i > 0; i--) {
            this.name.set(i, "");
            //ollirResult.getOllirClass()
            this.free.set(i, true);
            this.stack.add(i);
        }
        this.stackTop = this.size - 1;
    }
}
