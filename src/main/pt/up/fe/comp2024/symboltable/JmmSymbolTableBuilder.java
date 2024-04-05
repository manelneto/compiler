package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pt.up.fe.comp2024.ast.Kind.*;

public class JmmSymbolTableBuilder {

    TypeUtils typeUtils;

    public JmmSymbolTableBuilder() {
        this.typeUtils = new TypeUtils();
    }

    public JmmSymbolTable build(JmmNode root) {
        var children = root.getChildren();
        List<JmmNode> importDecls = children.size() > 1 ? children.subList(0, children.size() - 1) : new ArrayList<>();

        var imports = buildImports(importDecls);

        var classDecl = children.get(children.size() - 1);

        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);

        String className = classDecl.get("name");
        String superclassName = classDecl.getObject("isSubclass", Boolean.class) ? classDecl.get("parentClassName") : "";

        var fields = buildFields(classDecl);
        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);

        return new JmmSymbolTable(imports, className, superclassName, fields, methods, returnTypes, params, locals);
    }

    private List<String> buildImports(List<JmmNode> importDecls) {
        List<String> imports = new ArrayList<>();
        for (JmmNode i : importDecls) {
            List<String> tempImport = i.getObjectAsList("name", String.class);
            imports.add(tempImport.stream().reduce((a, e) -> a + "." + e).orElse(""));
        }
        return imports;
    }

    private Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> map = new HashMap<>();

        for (JmmNode method : classDecl.getChildren(METHOD_DECL)) {
            if (method.getObject("isVoid", Boolean.class)) {
                map.put("main", typeUtils.getVoidType());
            } else {
                var nodeType = method.getChild(0);
                Type type = new Type(nodeType.get("name"), nodeType.getObject("isArray", Boolean.class));
                map.put(method.get("name"), type);
            }
        }

        return map;
    }

    private Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        for (JmmNode method : classDecl.getChildren(METHOD_DECL)) {
            if (method.getObject("isVoid", Boolean.class)) {
                map.put("main", List.of(new Symbol(typeUtils.getStringArrayType(), method.get("paramName"))));
            } else {
                List<Symbol> params = new ArrayList<>();
                for (JmmNode param : method.getChildren(PARAM)) {
                    var nodeType = param.getChild(0);
                    Type type = new Type(nodeType.get("name"), nodeType.getObject("isArray", Boolean.class));
                    type.putObject("isEllipsis", nodeType.getObject("isEllipsis", Boolean.class));
                    params.add(new Symbol(type, param.get("paramName")));
                }
                map.put(method.get("name"), params);
            }
        }

        return map;
    }

    private Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();
        classDecl.getChildren(METHOD_DECL).forEach(method -> map.put(method.get("name"), getLocalsList(method)));
        return map;
    }

    private List<Symbol> buildFields(JmmNode classDecl) {
        List<Symbol> fields = new ArrayList<>();

        for (JmmNode field : classDecl.getChildren(VAR_DECL)) {
            var nodeType = field.getChild(0);
            Type type = new Type(nodeType.get("name"), nodeType.getObject("isArray", Boolean.class));
            String name = field.get("name");
            fields.add(new Symbol(type, name));
        }

        return fields;
    }

    private List<String> buildMethods(JmmNode classDecl) {
        return classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();
    }


    private List<Symbol> getLocalsList(JmmNode methodDecl) {
        List<Symbol> localsList = new ArrayList<>();

        for (JmmNode local : methodDecl.getChildren(VAR_DECL)) {
            var nodeType = local.getChild(0);
            Type type = new Type(nodeType.get("name"), nodeType.getObject("isArray", Boolean.class));
            String name = local.get("name");
            localsList.add(new Symbol(type, name));
        }

        return localsList;
    }

}
