package org.utility;

import org.apache.jena.atlas.lib.Pair;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxEditorParser;
import org.exactlearner.parser.OWLParserImpl;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ParserException;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;

import java.io.File;
import java.nio.file.FileSystems;
import java.util.Set;
import java.util.stream.Collectors;

public class OntologyManipulator {

    public static int computeOntologySize(String ontologyFileName) {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = null;
        try {
            ontology = manager.loadOntologyFromOntologyDocument(new File(ontologyFileName));
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }
        return OntologyManipulator.filterUnusedAxioms(ontology.getAxioms()).size();
    }

    public static OWLAxiom createAxiomFromString(String query, OWLOntology ontology) {
        //query = query.replace("SubClassOf", "SubClassOf:");
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        ManchesterOWLSyntaxEditorParser parser = getManchesterOWLSyntaxEditorParser(ontology, manager, query);
        OWLAxiom axiom = null;
        try {
            axiom = parser.parseAxiom();
        } catch (ParserException e) {
            System.err.println("Error parsing axiom: " + e.getMessage());
        }
        return axiom;
    }

    public static Set<String> parseAxioms(Set<OWLAxiom> axioms) {
        return axioms.stream().map(new ManchesterOWLSyntaxOWLObjectRendererImpl()::render).collect(Collectors.toSet());
    }

    public static ManchesterOWLSyntaxEditorParser getManchesterOWLSyntaxEditorParser(OWLOntology rootOntology, OWLOntologyManager manager, String axiomResult) {
        Set<OWLOntology> importsClosure = rootOntology.getImportsClosure();
        OWLEntityChecker entityChecker = new ShortFormEntityChecker(
                new BidirectionalShortFormProviderAdapter(manager, importsClosure,
                        new SimpleShortFormProvider()));

        ManchesterOWLSyntaxEditorParser parser = new ManchesterOWLSyntaxEditorParser(new OWLDataFactoryImpl(), axiomResult);
        parser.setDefaultOntology(rootOntology);
        parser.setOWLEntityChecker(entityChecker);
        return parser;
    }

    private static Set<String> checkClosure(Set<String> trueAnswers, Set<String> notTrueAnswers) {
        Set<Pair<String, String>> truePairs = getPair(trueAnswers);
        Set<Pair<String, String>> notTruePairs = getPair(notTrueAnswers);

        var result = notTruePairs.stream().filter(pair ->
                        truePairs.stream()
                                .anyMatch(first -> first.getLeft().equals(pair.getLeft()) &&
                                        truePairs.stream()
                                                .anyMatch(second -> second.getRight().equals(pair.getRight()) &&
                                                        first.getRight().equals(second.getLeft()))))
                .map(pair -> pair.getLeft() + " SubClassOf " + pair.getRight())
                .collect(Collectors.toSet());
        System.out.println(result);
        return result;
    }

    private static Set<Pair<String, String>> getPair(Set<String> answers) {
        try {
            return answers.stream().map(a -> {
                String[] split = a.split(" SubClassOf ");
                return new Pair<>(split[0].trim(), split[1].trim());
            }).collect(Collectors.toSet());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Set<OWLAxiom> filterUnusedAxioms(Set<OWLAxiom> axioms) {
        return axioms.stream().filter(axiom -> axiom.isOfType(AxiomType.SUBCLASS_OF)
                        || axiom.isOfType(AxiomType.EQUIVALENT_CLASSES)
                        || axiom.isOfType(AxiomType.SUB_OBJECT_PROPERTY)
                        || axiom.isOfType(AxiomType.EQUIVALENT_OBJECT_PROPERTIES)
                        || axiom.isOfType(AxiomType.OBJECT_PROPERTY_DOMAIN)
                        || axiom.isOfType(AxiomType.DISJOINT_CLASSES)
                        || axiom.isOfType(AxiomType.getAxiomType("ObjectOneOf")))
                .collect(Collectors.toSet());
    }

    public static String getOntologyShortName(String model, String ontology) {
        String separator = FileSystems.getDefault().getSeparator();
        // Check if the results directory exists
        if (!new File("results").exists()) {
            new File("results").mkdir();
        }
        if (!new File("results" + separator + "axiomsQuerying").exists()) {
            new File("results" + separator + "axiomsQuerying").mkdir();
        }
        String shortOntology = ontology.substring(ontology.lastIndexOf(separator) + 1);
        shortOntology = shortOntology.substring(0, shortOntology.lastIndexOf('.'));
        return "results" + separator + "axiomsQuerying" + separator + model.replace(":", "-") + '_' + shortOntology;
    }

    public static Set<String> getAllPossibleAxiomsCombinations(OWLOntology expectedOntology) {
        var parser = new OWLParserImpl(expectedOntology);
        var classes = parser.getClassesNamesAsString().stream().filter(s -> !s.toLowerCase().contains("thin")).distinct().toList();
        var properties = parser.getObjectPropertiesAsString();
        /*There are three types of statements:
        1. (A ∩ B) ⊑ C
        2. B ⊑ ∃R.A
        3. ∃R.A ⊑ B
        * Generate all possible combinations of these statements
         */
        var statement1 = classes.stream().flatMap(c1 -> classes.stream().flatMap(c2 -> classes.stream().map(c3 -> "( " + c1 + " and " + c2 + " ) SubClassOf: " + c3)))
                .collect(Collectors.toSet());
        var statement2 = classes.stream().flatMap(c1 -> properties.stream().flatMap(p -> classes.stream().map(c2 -> c1 + " SubClassOf: " + p + " some " + c2)))
                .collect(Collectors.toSet());
        var statement3 = classes.stream().flatMap(c1 -> properties.stream().flatMap(p -> classes.stream().map(c2 -> p + " some " + c1 + " SubClassOf: " + c2)))
                .collect(Collectors.toSet());
        //check empty set
        statement1.addAll(statement2);
        statement1.addAll(statement3);
        return statement1;
    }
}
