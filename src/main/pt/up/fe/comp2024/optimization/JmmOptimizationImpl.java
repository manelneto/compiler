package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.Collections;

public class JmmOptimizationImpl implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        var visitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());
        var ollirCode = visitor.visit(semanticsResult.getRootNode());

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        if (Boolean.parseBoolean(semanticsResult.getConfig().get("optimize"))) {
            var visitor = new OllirOptimizationVisitor(semanticsResult.getSymbolTable());
            visitor.visit(semanticsResult.getRootNode());
        }
        
        return semanticsResult;
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        return ollirResult;
    }
}
