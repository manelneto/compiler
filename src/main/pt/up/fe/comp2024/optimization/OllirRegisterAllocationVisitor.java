package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.HashSet;

public class OllirRegisterAllocationVisitor {

    private void livenessAnalysis(OllirResult ollirResult) {
        for (Method method : ollirResult.getOllirClass().getMethods()) {
            for (Instruction instruction : method.getInstructions()) {
                HashSet<String> def = this.calculateDef(instruction);
                //HashSet<String> use = this.calculateUse(instruction);
                //HashSet<String> in = this.calculateIn(instruction);
                //HashSet<String> out = this.calculateOut(instruction);
            }
        }
    }

    private HashSet<String> calculateDef(Instruction instruction) {
        HashSet<String> def = new HashSet<>();

        if (instruction instanceof AssignInstruction assignInstruction) {
            Operand dest = (Operand) assignInstruction.getDest();
            def.add(dest.getName());
        } else if (instruction instanceof PutFieldInstruction putFieldInstruction) {
            Operand field = putFieldInstruction.getField();
            def.add(field.getName());
        }

        return def;
    }

    private HashSet<String> calculateRead(Instruction instruction) {
        HashSet<String> read = new HashSet<>();

        //if (instruction instanceof )

        return read;
    }
}
