package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.ClassUnit;
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
            var constantPropagationVisitor = new OllirConstantPropagationVisitor();
            var constantFoldingVisitor = new OllirConstantFoldingVisitor();

            while (true) {
                boolean constantPropagated = constantPropagationVisitor.visit(semanticsResult.getRootNode());
                boolean constantFolded = constantFoldingVisitor.visit(semanticsResult.getRootNode());

                if (!constantPropagated && !constantFolded) {
                    break;
                }
            }
        }

        return semanticsResult;
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        ClassUnit c = ollirResult.getOllirClass();
        c.buildCFGs();
        c.buildVarTables();

        if (ollirResult.getConfig().containsKey("registerAllocation")) {
            int registers = Integer.parseInt(ollirResult.getConfig().get("registerAllocation"));

            OllirRegisterAllocationVisitor ollirRegisterAllocationVisitor = new OllirRegisterAllocationVisitor(ollirResult, registers);

            //ollirRegisterAllocationVisitor.visit(ol);
        }

        return ollirResult;
    }
}
