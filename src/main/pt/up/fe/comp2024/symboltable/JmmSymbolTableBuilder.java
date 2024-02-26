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


    public static JmmSymbolTable build(JmmNode root) {

        var children = root.getChildren();
        List<JmmNode> importDecls = children.size() > 1 ? children.subList(0, children.size() - 1) : new ArrayList<>();
        var classDecl = children.get(children.size() - 1);

        var imports = buildImports(importDecls);

        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("name");

        String superclassName; // TODO
        try {
            superclassName = classDecl.get("parentClassName");
        } catch (NullPointerException e) {
            superclassName = "";
        }

        var fields = buildFields(classDecl);
        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);

        return new JmmSymbolTable(imports, className, superclassName, fields, methods, returnTypes, params, locals);
    }

    private static List<String> buildImports(List<JmmNode> importDecls) {
        List<String> imports = new ArrayList<>();
        for (JmmNode i : importDecls) {
            imports.add(i.get("name"));
        }
        return imports;
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, Type> map = new HashMap<>();

        for (JmmNode method : classDecl.getChildren(METHOD_DECL)) {
            if (method.getObject("isVoid", Boolean.class)) {
                map.put("main", TypeUtils.getVoidType());
            } else {
                var nodeType = method.getChild(0);
                Type type = new Type(nodeType.get("name"), nodeType.getObject("isArray", Boolean.class));
                map.put(method.get("name"), type);
            }
        }

        return map;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();

        for (JmmNode method : classDecl.getChildren(METHOD_DECL)) {
            if (method.getObject("isVoid", Boolean.class)) {
                map.put("main", List.of(new Symbol(TypeUtils.getVoidType(), method.get("name"))));
            } else {
                List<Symbol> params = new ArrayList<>();
                for (JmmNode param : method.getChildren(PARAM)) {
                    var nodeType = param.getChild(0);
                    Type type = new Type(nodeType.get("name"), nodeType.getObject("isArray", Boolean.class));
                    params.add(new Symbol(type, param.get("name")));
                }
                map.put(method.get("name"), params);
            }
        }

        return map;
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();


        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));

        return map;
    }

    private static List<Symbol> buildFields(JmmNode classDecl) {
        return classDecl.getChildren(VAR_DECL).stream()
                .map(method -> new Symbol(new Type(method.getChild(0).get("name"), method.getChild(0).getChildren().size() == 3), method.get("name")))
                .toList();
    }

    private static List<String> buildMethods(JmmNode classDecl) {
        List<String> methods = new ArrayList<>();
        for (JmmNode method : classDecl.getChildren(METHOD_DECL)) {
            if (method.getObject("isVoid", Boolean.class)) {
                methods.add("main");
            } else {
                methods.add(method.get("name"));
            }
        }

        return methods;
        /*
        return classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();*/
    }


    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        // TODO: Simple implementation that needs to be expanded

        var intType = new Type(TypeUtils.getIntTypeName(), false);

        return methodDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol(intType, varDecl.get("name")))
                .toList();
    }

}
