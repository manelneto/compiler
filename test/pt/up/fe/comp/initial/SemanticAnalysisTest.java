package pt.up.fe.comp.initial;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.specs.util.SpecsIo;

import static org.junit.Assert.assertEquals;


public class SemanticAnalysisTest {

    @Test
    public void undeclaredVariable() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/initial/semanticanalysis/UndeclaredVariable.jmm"));
        TestUtils.mustFail(result);
        assertEquals(1, result.getReports(ReportType.ERROR).size());
        // TODO
        //  este teste falha porque um dos reports na lista é a exceção que resulta do tipo da variável b (não declarada) ser null
        //  a solução deve passar por parar a verificação semântica depois de correr UndeclaredVariable, se houver algum erro
        //  tentar mudar o retorno de getVarRefType de null para b resulta noutro report "Invalid binary operation", pelo que o problema mantém-se
        System.out.println(result.getReports());
    }


}
