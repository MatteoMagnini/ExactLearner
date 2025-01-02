package org.analysis.exp2;
import org.analysis.common.Metrics;
import org.configurations.Configuration;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ResultAnalyzer {

    public static void main(String[] args) {
        Configuration config = new YAMLConfigLoader().getConfig(args[0], Configuration.class);

        config.getOntologies().forEach(ontology -> config.getModels().forEach(model -> {
            try {
                new ResultAnalyzer(model.replace(":", "-"), ontology.substring(0, ontology.length() - 4)).run();
            } catch (OWLOntologyCreationException e) {
                throw new RuntimeException(e);
            }
        }));
    }
    private final OWLOntology nlpBaseOntology;
    private final OWLOntology manchesterBaseOntology;
    private final OWLOntology expectedOntology;
    private final OWLOntology manchesterAdvancedOntology;
    private final OWLOntology nlpAdvancedOntology;
    private final List<String> allPossibleAxioms;

    private final List<Boolean> inferredAxiomsByExpectedOntology = new ArrayList<>();

    private final String model;
    private final String ontology;

    public ResultAnalyzer(String model, String ontology) throws OWLOntologyCreationException {
        this.model = model;
        // Keep only the name of the ontology without the path
        this.ontology = ontology;
        this.expectedOntology = loadOntology();
        String shortName = Path.of(ontology).getFileName().toString();
        String sep = FileSystems.getDefault().getSeparator();
        String ontologyResultPath = "results" + sep + "ontologies";
        String manchesterBaseOntologyPath = "results" + sep + "ontologies" + sep + shortName + "_" + model + "_" + "manchester_base.owl";
        this.manchesterBaseOntology = loadOntology(manchesterBaseOntologyPath);
        String nlpBaseOntologyPath = ontologyResultPath + sep + shortName + "_" + model + "_" + "nlp_base.owl";
        this.nlpBaseOntology = loadOntology(nlpBaseOntologyPath);
        String manchesterAdvancedOntologyPath = ontologyResultPath + sep + shortName + "_" + model + "_" + "manchester_advanced.owl";
        this.manchesterAdvancedOntology = loadOntology(manchesterAdvancedOntologyPath);
        String nlpAdvancedOntologyPath = ontologyResultPath + sep + shortName + "_" + model + "_" + "nlp_advanced.owl";
        this.nlpAdvancedOntology = loadOntology(nlpAdvancedOntologyPath);
        this.allPossibleAxioms = OntologyManipulator.getAllPossibleAxiomsCombinations(expectedOntology).stream().sorted().toList();
    }

    public void run() {
        try {
            compareOntologies();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void compareOntologies() throws InterruptedException {
        System.out.println("Evaluation using Ontology " + ontology + " and Model " + model + "...");
        long startingTime = System.currentTimeMillis();

        // Inizializzazione delle matrici di confusione
        int[][] confusionMatrix = new int[3][3];
        int[][] nlpConfusionMatrix = new int[3][3];
        int[][] enrichedConfusionMatrix = new int[3][3];
        int[][] enrichedNlpConfusionMatrix = new int[3][3];

        // Creazione e gestione dei Reasoner con controllo null
        Reasoner expectedReasoner = initializeReasonerOrFillMatrix(expectedOntology, null);
        Reasoner predictedReasoner = initializeReasonerOrFillMatrix(manchesterBaseOntology, confusionMatrix);
        Reasoner nlpPredictedReasoner = initializeReasonerOrFillMatrix(nlpBaseOntology, nlpConfusionMatrix);
        Reasoner enrichedPredictedReasoner = initializeReasonerOrFillMatrix(manchesterAdvancedOntology, enrichedConfusionMatrix);
        Reasoner enrichedNlpPredictedReasoner = initializeReasonerOrFillMatrix(nlpAdvancedOntology, enrichedNlpConfusionMatrix);

        if (expectedReasoner != null) {
            allPossibleAxioms.forEach(ax -> {
                var axiom = OntologyManipulator.createAxiomFromString(ax, expectedOntology);
                inferredAxiomsByExpectedOntology.add(expectedReasoner.isEntailed(axiom));
            });
        }

        if (predictedReasoner != null) updateConfusionMatrix(predictedReasoner, confusionMatrix);
        if (enrichedPredictedReasoner != null) updateConfusionMatrix(enrichedPredictedReasoner, enrichedConfusionMatrix);
        if (nlpPredictedReasoner != null) updateConfusionMatrix(nlpPredictedReasoner, nlpConfusionMatrix);
        if (enrichedNlpPredictedReasoner != null) updateConfusionMatrix(enrichedNlpPredictedReasoner, enrichedNlpConfusionMatrix);

        printResults(confusionMatrix, nlpConfusionMatrix, enrichedConfusionMatrix, enrichedNlpConfusionMatrix);
        System.out.println("Evaluation completed in " + (System.currentTimeMillis() - startingTime) / 1000 + " seconds.");
    }

    private Reasoner initializeReasonerOrFillMatrix(OWLOntology ontology, int[][] confusionMatrix) {
        if (ontology == null) {
            if (confusionMatrix != null) {
                for (int[] matrix : confusionMatrix) {
                    Arrays.fill(matrix, 0);
                }
            }
            System.out.println("Warning: Ontology is null, confusion matrix filled with zeros.");
            return null;
        }
        return new Reasoner(ontology);
    }


    private void updateConfusionMatrix(Reasoner predictedReasoner, int[][] confusionMatrix) {
        List<Boolean> inferredAxiomsByPredictedOntology = new ArrayList<>();
        allPossibleAxioms.forEach(ax -> {
            var axiom = OntologyManipulator.createAxiomFromString(ax, expectedOntology);
            inferredAxiomsByPredictedOntology.add(predictedReasoner.isEntailed(axiom));
        });
        // Update confusion matrix
        for (int i = 0; i < inferredAxiomsByExpectedOntology.size(); i++) {
            int row = inferredAxiomsByExpectedOntology.get(i) ? 0 : 1;
            int col = inferredAxiomsByPredictedOntology.get(i) ? 0 : 1;
            confusionMatrix[row][col]++;
        }
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
        var path = getOntologyPath(type, enginePrefix, model);
        if (!path.toFile().exists()) {
            System.err.println("Ontology file not found: " + path);
            return null;
        }
        return loadOntology(getOntologyPath(type, enginePrefix, model).toString());
    }

    private OWLOntology loadOntology() {
        String ontologyName = Path.of(ontology).getFileName().toString().replaceAll("\\(.*\\)", "");
        String path = String.format("results%1$sontologies%1$s%2$s_%3$s",
                FileSystems.getDefault().getSeparator(), "target", ontologyName);
        return loadOntology(path + ".owl");
    }

    private OWLOntology loadOntology(String path) {
        if (!new File(path).exists()) {
            System.out.println("Ontology file not found: " + path + "\nSkipping...");
            return null;
        }
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        try {
            return manager.loadOntologyFromOntologyDocument(new File(path));
        } catch (OWLOntologyCreationException e) {
            System.err.println("Error loading ontology: " + e.getMessage());
            System.exit(1);
            return null;
        }
    }
}