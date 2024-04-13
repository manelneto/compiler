package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Duplicated extends AnalysisVisitor {

    private String currentMethod;
    private SymbolTable table;
    TypeUtils typeUtils = new TypeUtils("", table);

    @Override
    public void buildVisitor() {
        addVisit(Kind.CLASS_DECL, this::visitClassDecl);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        //addVisit(Kind.ARRAY_ACCESS, this::visitArrayAccess);
        //addVisit(Kind.ARRAY, this::visitArray);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        typeUtils.setCurrentMethod(currentMethod);
        List<String> duplicates = getDuplicatedParams(table);
        if (!duplicates.isEmpty()) {
            reportError(String.format("Duplicated params on method %s: ", currentMethod) + String.join(",", duplicates), method);
            return null;
        }
        duplicates = getDuplicatedLocals(table);
        if (!duplicates.isEmpty()) {
            reportError(String.format("Duplicated local variables on method %s: ", currentMethod) + String.join(",", duplicates), method);
            return null;
        }
        return null;
    }

    private Void visitClassDecl(JmmNode class_, SymbolTable table) {
        List<String> duplicates = getDuplicatedFields(table);
        if (!duplicates.isEmpty()) {
            reportError("Duplicated fields: " + String.join(",", duplicates), class_);
            return null;
        }
        duplicates = getDuplicatedImports(table);
        if (!duplicates.isEmpty()) {
            reportError("Duplicated imports: " + String.join(",", duplicates), class_);
            return null;
        }

        duplicates = getDuplicatedMethods(table);
        if(!duplicates.isEmpty()) {
            reportError("Duplicated methods: " + String.join(",", duplicates), class_);
            return null;
        }

        return null;
    }

    private List<String> getDuplicatedFields(SymbolTable symbolTable) {
        HashSet<String> set = new HashSet<>();
        HashSet<String> duplicates = new HashSet<>();

        List<String> fields = symbolTable.getFields().stream()
                .map(Symbol::getName)
                .toList();

        for (String field : fields) {
            if (set.contains(field)) {
                duplicates.add(field);
            } else {
                set.add(field);
            }
        }
        return new ArrayList<>(duplicates);
    }
    private List<String> getDuplicatedImports(SymbolTable symbolTable) {
        HashSet<String> set = new HashSet<>();
        HashSet<String> duplicates = new HashSet<>();

        List<String> imports = symbolTable.getImports().stream().toList();

        for (String import_ : imports) {
            if (set.contains(import_)) {
                duplicates.add(import_);
            } else {
                set.add(import_);
            }
        }
        return new ArrayList<>(duplicates);
    }

    private List<String> getDuplicatedMethods(SymbolTable symbolTable) {
        HashSet<String> set = new HashSet<>();
        HashSet<String> duplicates = new HashSet<>();

        List<String> methods = symbolTable.getMethods().stream().toList();

        for (String method : methods) {
            if (set.contains(method)) {
                duplicates.add(method);
            } else {
                set.add(method);
            }
        }
        return new ArrayList<>(duplicates);
    }

    private List<String> getDuplicatedParams(SymbolTable symbolTable) {
        HashSet<String> set = new HashSet<>();
        HashSet<String> duplicates = new HashSet<>();

        List<String> params = symbolTable.getParameters(currentMethod).stream()
                .map(Symbol::getName).toList();

        for (String param : params) {
            if (set.contains(param)) {
                duplicates.add(param);
            } else {
                set.add(param);
            }
        }
        return new ArrayList<>(duplicates);
    }

    private List<String> getDuplicatedLocals(SymbolTable symbolTable) {
        HashSet<String> set = new HashSet<>();
        HashSet<String> duplicates = new HashSet<>();

        List<String> locals = symbolTable.getLocalVariables(currentMethod).stream()
                .map(Symbol::getName).toList();

        for (String local : locals) {
            if (set.contains(local)) {
                duplicates.add(local);
            } else {
                set.add(local);
            }
        }
        return new ArrayList<>(duplicates);
    }




}