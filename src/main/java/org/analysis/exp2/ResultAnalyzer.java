package org.analysis.exp2;
import org.analysis.common.Metrics;
import org.configurations.Configuration;
import org.exactlearner.parser.OWLParserImpl;
import org.pac.Pac;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.utility.OntologyManipulator;
import org.utility.YAMLConfigLoader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class ResultAnalyzer {

    public static void main(String[] args) {
        Configuration config = new YAMLConfigLoader().getConfig(args[0], Configuration.class);
        config.getOntologies().forEach(ontology -> config.getModels().forEach(model -> {
            try {
                new ResultAnalyzer(model.replace(":", "-"), ontology).run();
            } catch (OWLOntologyCreationException e) {
                throw new RuntimeException(e);
            }
        }));
    }
    private final OWLOntology nlpPredictedOntology;
    private final OWLOntology predictedOntology;
    private final OWLOntology expectedOntology;
    private final OWLOntology enrichedPredictedOntology;
    private final OWLOntology enrichedNlpPredictedOntology;
    private final double allPossibleAxioms;

    private final String model;
    private final String ontology;
    private static final String TF_TYPE = "true_false";
    private static final String RICH_TYPE = "rich_prompt";

    public ResultAnalyzer(String model, String ontology) throws OWLOntologyCreationException {
        this.model = model;
        this.ontology = ontology;
        this.expectedOntology = OntologyManipulator.getInferredOntology(ontology);
        this.predictedOntology = OntologyManipulator.getInferredOntology(getOntologyPathString(TF_TYPE, "manchester_", model));
        this.nlpPredictedOntology = OntologyManipulator.getInferredOntology(getOntologyPathString(TF_TYPE, "nlp_", model));
        this.enrichedPredictedOntology = OntologyManipulator.getInferredOntology(getOntologyPathString(RICH_TYPE, "manchester_", model));
        this.enrichedNlpPredictedOntology = OntologyManipulator.getInferredOntology(getOntologyPathString(RICH_TYPE, "nlp_", model));
        var parser = new OWLParserImpl(expectedOntology);
        this.allPossibleAxioms = new Pac(parser.getClassesNamesAsString(), parser.getObjectPropertiesAsString(), 0.2, 0.1, OntologyManipulator.computeOntologySize(ontology), 0).computeInstanceSpaceSize();
    }

    public void run() {
        compareOntologies();
    }

    private void compareOntologies() {
        int[][] confusionMatrix = new int[3][3];
        int[][] nlpConfusionMatrix = new int[3][3];
        int[][] enrichedConfusionMatrix = new int[3][3];
        int[][] enrichedNlpConfusionMatrix = new int[3][3];

        Reasoner expectedReasoner = new Reasoner(expectedOntology);
        Reasoner predictedReasoner = new Reasoner(predictedOntology);
        Reasoner nlpPredictedReasoner = new Reasoner(nlpPredictedOntology);
        Reasoner enrichedPredictedReasoner = new Reasoner(enrichedPredictedOntology);
        Reasoner enrichedNlpPredictedReasoner = new Reasoner(enrichedNlpPredictedOntology);

        updateConfusionMatrix(predictedReasoner, expectedReasoner, this.predictedOntology, confusionMatrix);
        updateConfusionMatrix(enrichedPredictedReasoner, expectedReasoner, this.enrichedPredictedOntology, enrichedConfusionMatrix);
        updateConfusionMatrix(nlpPredictedReasoner, expectedReasoner, this.nlpPredictedOntology, nlpConfusionMatrix);
        updateConfusionMatrix(enrichedNlpPredictedReasoner, expectedReasoner, this.enrichedNlpPredictedOntology, enrichedNlpConfusionMatrix);

        printResults(confusionMatrix, nlpConfusionMatrix, enrichedConfusionMatrix, enrichedNlpConfusionMatrix);
    }

    private void updateConfusionMatrix(Reasoner predictedReasoner, Reasoner expectedReasoner, OWLOntology predictedOntology, int[][] confusionMatrix) {

        expectedOntology.getAxioms().stream().map(OntologyManipulator::axiomToString)
                .filter(OntologyManipulator::isAxiomInTheRightFormat)
                .forEach(ax -> {
            System.out.println("CHECKING "+ ax.toLowerCase());
            var axiom = OntologyManipulator.createAxiomFromString(ax, expectedOntology);
            if (predictedReasoner.isEntailed(axiom)) {
                confusionMatrix[0][0]++; //TP
            } else {
                confusionMatrix[1][0]++; //FN
            }
        });
        predictedOntology.getAxioms().stream().filter(ax -> !expectedOntology.getAxioms().contains(ax))
                .map(OntologyManipulator::axiomToString)
                .filter(OntologyManipulator::isAxiomInTheRightFormat)
                .forEach(ax -> {
            var axiom = OntologyManipulator.createAxiomFromString(ax, predictedOntology);
            if (!expectedReasoner.isEntailed(axiom) && predictedReasoner.isEntailed(axiom)) {
                confusionMatrix[0][2]++; //FP
            }
        });
        confusionMatrix[1][2] = (int) allPossibleAxioms - confusionMatrix[0][0] - confusionMatrix[1][0] - confusionMatrix[0][2];
    }

    private void printResults(int[][] confusionMatrix, int[][] nlpConfusionMatrix, int[][] enrichedConfusionMatrix, int[][] enrichedNlpConfusionMatrix) {
        String ontologyName = Path.of(ontology).getFileName().toString().replaceAll("\\(.*\\)", "");

        System.out.printf("Ontology: %s%n", ontologyName);
        System.out.printf("Model: %s%n", model);
        printMetrics("M.Syntax", confusionMatrix);
        printMetrics("NLP", nlpConfusionMatrix);
        printMetrics("Enriched prompt M.Syntax", enrichedConfusionMatrix);
        printMetrics("Enriched prompt NLP", enrichedNlpConfusionMatrix);
        System.out.println("##############################################################");
        generateSummaryFilesForLatexTable(ontologyName, model, confusionMatrix, nlpConfusionMatrix, enrichedConfusionMatrix, enrichedNlpConfusionMatrix);
    }

    private void generateSummaryFilesForLatexTable(String ontologyName, String model, int[][] confusionMatrix, int[][] nlpConfusionMatrix, int[][] enrichedConfusionMatrix, int[][] enrichedNlpConfusionMatrix) {
        FileWriter fw;
        var s = FileSystems.getDefault().getSeparator();
        try {
            File f = new File("analysis" + s + ontologyName + "-" + model + ".txt");
            f.createNewFile();
            fw = new FileWriter(f.getPath());
            String result = calculateMetrics(confusionMatrix);
            result = result.concat(" " + calculateMetrics(nlpConfusionMatrix));
            result = result.concat(" " + calculateMetrics(enrichedConfusionMatrix));
            result = result.concat(" " + calculateMetrics(enrichedNlpConfusionMatrix));
            fw.write(result);
            fw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private String calculateMetrics(int[][] confusionMatrix) {
        return Metrics.calculateAccuracy(confusionMatrix) + " " + Metrics.calculateRecall(confusionMatrix) + " " + Metrics.calculatePrecision(confusionMatrix) + " " + Metrics.calculateF1Score(confusionMatrix);
    }

    private void printMetrics(String label, int[][] confusionMatrix) {
        System.out.printf("%s RECALL: %.2f%n", label, Metrics.calculateRecall(confusionMatrix));
        System.out.printf("%s PRECISION: %.2f%n", label, Metrics.calculatePrecision(confusionMatrix));
        System.out.printf("%s F1-Score: %.2f%n", label, Metrics.calculateF1Score(confusionMatrix));
        System.out.printf("%s Accuracy: %.2f%n", label, Metrics.calculateAccuracy(confusionMatrix));
    }

    private String getOntologyStringName() {
        return Path.of(ontology).getFileName().toString().replaceAll("\\(.*\\)", "");
    }

    private Path getOntologyPath(String type, String enginePrefix, String model) {
        return Path.of(String.format("results%1$sontologies%1$s%2$s%1$s%3$slearned_%4$s_%5$s",
                FileSystems.getDefault().getSeparator(), type, enginePrefix, model.replace(":", "-"), getOntologyStringName()));
    }

    private String getOntologyPathString(String type, String enginePrefix, String model) {
        return getOntologyPath(type, enginePrefix, model).toString();
    }

    private OWLOntology loadOntology(String type, String enginePrefix, String model) {
        return loadOntologyFromFile(getOntologyPath(type, enginePrefix, model).toString());
    }

    private OWLOntology loadOntology() {
        String ontologyName = Path.of(ontology).getFileName().toString().replaceAll("\\(.*\\)", "");
        String path = String.format("results%1$sontologies%1$s%2$s_%3$s",
                FileSystems.getDefault().getSeparator(), "target", ontologyName);
        return loadOntologyFromFile(path);
    }

    private OWLOntology loadOntologyFromFile(String path) {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        try {
            return manager.loadOntologyFromOntologyDocument(new File(path));
        } catch (OWLOntologyCreationException e) {
            System.err.println("ERROR IN READING OWL FILE; CHECK IF LLM LEARNER THROWS SOME ERRORS!!!");
            throw new RuntimeException(e);
        }
    }
}