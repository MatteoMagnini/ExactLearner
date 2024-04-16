package org.analysis;

import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxEditorParser;
import org.experiments.Configuration;
import org.experiments.Environment;
import org.experiments.logger.SmartLogger;
import org.experiments.task.ExperimentTask;
import org.experiments.task.Task;
import org.experiments.utility.SHA256Hash;
import org.experiments.utility.YAMLConfigLoader;
import org.experiments.workload.OllamaWorkload;
import org.jetbrains.annotations.NotNull;
import org.junit.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ParserException;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.yaml.snakeyaml.Yaml;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Set;

public class AxiomsResultsReaderTest{

    private String type;
    private int maxTokens;
    private String model;
    private String ontology;
    private String system;
    private AxiomsResultsReader axiomsResultsReader;
    @Before
    public void setUp() {
        var config = new YAMLConfigLoader().getConfig("src/main/java/org/experiments/axiomsQueryingConf.yml", Configuration.class);

        type = config.getType();
        model = config.getModels().get(0);
        ontology = config.getOntologies().get(0);
        system = config.getSystem();
        maxTokens = config.getMaxTokens();

        runSomeTask();
    }

    private void runSomeTask() {
        String axiom = "Bird SubClassOf (has_part some Leg) and (has_part some Wing)";//TRUE
        var work = new OllamaWorkload(model, system, axiom, maxTokens);
        Task task = new ExperimentTask(type, model, ontology, axiom, system, work);
        Environment.run(task);

        axiom = "Reptile SubClassOf Carnivore and (lays some Egg)";//FALSE
        work = new OllamaWorkload(model, system, axiom, maxTokens);
        task = new ExperimentTask(type, model, ontology, axiom, system, work);
        Environment.run(task);
    }
    @Test
    public void testComputeResultsTrue() {
        String axiom = "Bird SubClassOf (has_part some Leg) and (has_part some Wing)";//TRUE or YES
        axiomsResultsReader = new AxiomsResultsReader(type, model, ontology, axiom, system);
        checkFileExists();
        axiomsResultsReader.computeResults();
        String axiomResult = axiomsResultsReader.getAxiom();
        assert (axiomResult.equals(axiom.replace("SubClassOf", "SubClassOf:")));
        assert (axiomsResultsReader.getFileNameToAnalyze().equals(SHA256Hash.sha256(type + model + ontology + axiom + system)));
        try {
            OWLOntologyManager manager= OWLManager.createOWLOntologyManager();
            OWLOntology rootOntology=manager.loadOntologyFromOntologyDocument(new File(ontology));
            ManchesterOWLSyntaxEditorParser parser = getManchesterOWLSyntaxEditorParser(rootOntology, manager, axiomResult);
            OWLAxiom owlAxiom = parser.parseAxiom();
            System.out.println(owlAxiom.toString());
        } catch (ParserException e) {
            e.printStackTrace();
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }
    }

    private ManchesterOWLSyntaxEditorParser getManchesterOWLSyntaxEditorParser(OWLOntology rootOntology, OWLOntologyManager manager, String axiomResult) {
        Set<OWLOntology> importsClosure = rootOntology.getImportsClosure();
        OWLEntityChecker entityChecker = new ShortFormEntityChecker(
                new BidirectionalShortFormProviderAdapter(manager, importsClosure,
                        new SimpleShortFormProvider()));

        ManchesterOWLSyntaxEditorParser parser = new ManchesterOWLSyntaxEditorParser(new OWLDataFactoryImpl(), axiomResult);
        parser.setDefaultOntology(rootOntology);
        parser.setOWLEntityChecker(entityChecker);
        return parser;
    }

    @Test
    public void testComputeResultsFalse() {
        String axiom = "Reptile SubClassOf Carnivore and (lays some Egg)";//FALSE
        axiomsResultsReader = new AxiomsResultsReader(type, model, ontology, axiom, system);
        checkFileExists();
        axiomsResultsReader.computeResults();
        assert (axiomsResultsReader.getAxiom().isEmpty());
        assert (axiomsResultsReader.getFileNameToAnalyze().equals(SHA256Hash.sha256(type + model + ontology + axiom + system)));
    }

    private void checkFileExists() {
        if (SmartLogger.isFileInCache(axiomsResultsReader.getFileNameToAnalyze())) {
            System.out.println("File is in cache");
        } else {
            throw new RuntimeException("File is not in cache");
        }
    }
}