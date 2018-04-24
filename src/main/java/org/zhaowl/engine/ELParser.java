package org.zhaowl.engine;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ELParser {
	private final OWLOntology rootOntology;
    private final BidirectionalShortFormProvider bidiShortFormProvider;
    private final OWLDataFactory dataFactory;
    private static final Logger LOGGER_ = LoggerFactory
			.getLogger(ELParser.class);
    /** Constructs a ELQueryParser using the specified ontology and short form
     * provider to map entity IRIs to short names.
     * 
     * @param rootOntology
     *            The root ontology. This essentially provides the domain
     *            vocabulary for the query.
     * @param shortFormProvider
     *            A short form provider to be used for mapping back and forth
     *            between entities and their short names (renderings). */
    public ELParser(OWLOntology rootOntology, ShortFormProvider shortFormProvider) {
        this.rootOntology = rootOntology;
        OWLOntologyManager manager = rootOntology.getOWLOntologyManager();
        Set<OWLOntology> importsClosure = rootOntology.getImportsClosure();
        // Create a bidirectional short form provider to do the actual mapping.
        // It will generate names using the input
        // short form provider.
        bidiShortFormProvider = new BidirectionalShortFormProviderAdapter(manager,
                importsClosure, shortFormProvider);
        dataFactory = rootOntology.getOWLOntologyManager().getOWLDataFactory();
        
    }
    
//    /** Parses a class expression string to obtain a class expression.
//     * 
//     * @param classExpressionString
//     *            The class expression string
//     * @return The corresponding class expression if the class expression string
//     *         is malformed or contains unknown entity names. */
//    public OWLClassExpression parseClassExpression(String classExpressionString) {
//    	// Set up the real parser
//        ManchesterOWLSyntaxEditorParser parser = new ManchesterOWLSyntaxEditorParser(
//                dataFactory, classExpressionString);
//        parser.setDefaultOntology(rootOntology);
//        // Specify an entity checker that wil be used to check a class
//        // expression contains the correct names.
//        OWLEntityChecker entityChecker = new ShortFormEntityChecker(bidiShortFormProvider);
//        parser.setOWLEntityChecker(entityChecker);
//        // Do the actual parsing
//        try {
//			return parser.parseClassExpression();
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			LOGGER_.trace("The concept " + classExpressionString + "  is not in this Ontology!");
//			 
//		}
//		return null;
//    }
    
    public OWLAxiom parseSubClassOfAxiom(OWLClassExpression concept1, OWLClassExpression concept2){
    	return dataFactory.getOWLSubClassOfAxiom(concept1, concept2);
    }
}
