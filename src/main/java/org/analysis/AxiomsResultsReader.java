package org.analysis;

import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxEditorParser;
import org.experiments.utility.SHA256Hash;
import org.semanticweb.owlapi.expression.ParserException;
import org.semanticweb.owlapi.model.OWLAxiom;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

public class AxiomsResultsReader implements BaseResultReader {

    private final String fileNameToAnalyze;
    private String axiom = "";

    public AxiomsResultsReader(String taskTypeUsed, String modelUsed, String ontologyUsed, String axiomUsed, String systemUsed) {
        fileNameToAnalyze = SHA256Hash.sha256(taskTypeUsed + modelUsed + ontologyUsed + axiomUsed + systemUsed);
    }

    @Override
    public void computeResults() {
        File file = new File("cache/" + fileNameToAnalyze + ".csv");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            String str = new String(data, StandardCharsets.UTF_8);
            String charToSplit = ",";
            var strArr = str.split(charToSplit);
            strArr[1] = strArr[1].toLowerCase();
            if ((strArr[1].contains("true") || strArr[1].contains("yes"))) {
                //NOTE: ManchesterOWLSyntaxEditorParser expect KeyWords with ":" at the end
                //but the Opposite Parser from OWLAxiom to String doesn't add the ":" at the end
                //so we need to add it manually
                axiom = strArr[0].replace("SubClassOf", "SubClassOf:")
                        .replace("DisjointWith", "DisjointWith:")
                        .replace("EquivalentTo", "EquivalentTo:");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getFileNameToAnalyze() {
        return fileNameToAnalyze;
    }

    public String getAxiom(){
        return axiom;
    }
}
