package org.zhaowl.userInterface;

import java.util.Set;

import org.semanticweb.elk.owl.interfaces.ElkClass;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.elk.reasoner.taxonomy.model.Taxonomy;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ELEngine {
	public final OWLReasoner reasoner;
    public final ELParser parser;
    private static final Logger LOGGER_ = LoggerFactory
            .getLogger(ELEngine.class);
    /** Constructs a ELQueryEngine. This will answer "DL queries" using the
     * specified reasoner. A short form provider specifies how entities are
     * rendered.
     * 
     * @param reasoner
     *            The reasoner to be used for answering the queries.
     * @param shortFormProvider
     *            A short form provider. */
    public ELEngine(OWLReasoner reasoner, ShortFormProvider shortFormProvider) {
        this.reasoner = reasoner;
        OWLOntology rootOntology = reasoner.getRootOntology();
        parser = new ELParser(rootOntology, shortFormProvider);
    }    

    public OWLClassExpression parseClassExpression(String s){
    	OWLClassExpression classString = parser.parseClassExpression(s);
		return classString;
	}
    
	public OWLAxiom parseToOWLSubClassOfAxiom(String concept1String, String concept2String){
    	OWLClassExpression concept1 = parser.parseClassExpression(concept1String);
		OWLClassExpression concept2 = parser.parseClassExpression(concept2String);
		return getSubClassAxiom(concept1, concept2);
	}
	
	public OWLAxiom getSubClassAxiom(OWLClassExpression concept1, OWLClassExpression concept2){
		OWLAxiom subclassAxiom = parser.parseSubClassOfAxiom(concept1, concept2);
		return subclassAxiom;
    }

    public Boolean entailedEQ(OWLSubClassOfAxiom subclassAxiom) {
        OWLClassExpression left  = subclassAxiom.getSubClass();
        OWLClassExpression right = subclassAxiom.getSuperClass();

        Boolean workaround = false;
        reasoner.flush();
        OWLOntologyManager man = OWLManager.createOWLOntologyManager();
        OWLDataFactory dataFactory = man.getOWLDataFactory();

        OWLClass leftName  = dataFactory.getOWLClass(IRI.create("#temp001"));
        OWLClass rightName = dataFactory.getOWLClass(IRI.create("#temp002"));

        OWLAxiom leftDefinition  = dataFactory.getOWLEquivalentClassesAxiom(leftName,  left);
        OWLAxiom rightDefinition = dataFactory.getOWLEquivalentClassesAxiom(rightName, right);
        man.addAxiom(reasoner.getRootOntology(), leftDefinition);
        man.addAxiom(reasoner.getRootOntology(), rightDefinition);

        LOGGER_.info("ontology: ");
        for(OWLAxiom ax : reasoner.getRootOntology().getAxioms())
            LOGGER_.info(ax.toString());
        //System.out.println("SubclassAx: " + subclassAxiom.toString());

        reasoner.flush();
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

        NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(leftName, false);
        Node<OWLClass> equivClassess = reasoner.getEquivalentClasses(leftName);

        LOGGER_.info("(Strict) superClasses of " + leftName + " ");
        for(OWLClass c : superClasses.getFlattened()) {
            LOGGER_.info(c.toString());
        }
        LOGGER_.info("");
        if (!superClasses.isEmpty() && superClasses.containsEntity(rightName))
            workaround = true;

        LOGGER_.info("equivalentClasses of " + leftName + " ");
        for(OWLClass c : equivClassess.getEntities()) {
            LOGGER_.info(c.toString());
        }
        LOGGER_.info("");
        if (!equivClassess.getEntities().isEmpty() && equivClassess.getEntities().contains(rightName))
            workaround = true;


        man.removeAxiom(reasoner.getRootOntology(), leftDefinition);
        man.removeAxiom(reasoner.getRootOntology(), rightDefinition);
        reasoner.flush();
        LOGGER_.info("returning " + workaround);
        LOGGER_.info("");
        LOGGER_.info("");

        return workaround;
    }

	public Boolean entailed(OWLAxiom ax) {
        LOGGER_.info("InputAx: " + ax.toString());
        if(ax.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
            OWLEquivalentClassesAxiom eax = (OWLEquivalentClassesAxiom) ax;
            for(OWLSubClassOfAxiom sax : eax.asOWLSubClassOfAxioms()) {
                if(!entailedEQ(sax)) {
                    //System.out.println("returning false");
                    return false;
                }
            }
            //System.out.println("returning true");
            return true;
        }

        if(ax.isOfType(AxiomType.SUBCLASS_OF)){
            Boolean result = entailedEQ((OWLSubClassOfAxiom) ax);
            //System.out.println("returning " + result);
            return result;
        }


        throw new RuntimeException("Axiom type not supported " + ax.toString());

    }

	public Boolean entailed(Set<OWLAxiom> axioms) {
        for(OWLAxiom ax : axioms) {
            if(!entailed(ax)) {
                //System.out.println("SET CALL: " + ax.toString());
                //System.out.println("returning false");
                return false;
            }
        }
        //System.out.println("returning true");
        return true;
	}
    
    /** Gets the superclasses of a class expression parsed from a string.
     * 
     * @param classExpressionString
     *            The string from which the class expression will be parsed.
     * @param direct
     *            Specifies whether direct superclasses should be returned or
     *            not.
     * @return The superclasses of the specified class expression If there was a
     *         problem parsing the class expression. */
	
    public Set<OWLClass> getSuperClasses(OWLClassExpression superclass, boolean direct) {
        OWLClassExpression classExpression = superclass;
        NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(classExpression, direct);

        return superClasses.getFlattened();
    }

    /** Gets the subclasses of a class expression parsed from a string.
     * 
     * @param subclass
     *            The string from which the class expression will be parsed.
     * @param direct
     *            Specifies whether direct subclasses should be returned or not.
     * @return The subclasses of the specified class expression If there was a
     *         problem parsing the class expression. */
    public Set<OWLClass> getSubClasses(OWLClassExpression subclass, boolean direct) {
    	OWLClassExpression classExpression = subclass;
        NodeSet<OWLClass> subClasses = reasoner.getSubClasses(classExpression, direct);
        return subClasses.getFlattened();
    }

    public static OWLReasoner createReasoner(final OWLOntology rootOntology, String reasonerName) {
        LOGGER_.info("Reasoner "+ reasonerName + " created");
        //Thread.dumpStack();
        System.out.flush();
        ElkReasonerFactory reasoningFactory = new ElkReasonerFactory();
        return reasoningFactory.createReasoner(rootOntology);
    }
    public static void disposeOfReasoner(OWLReasoner owlReasoner, String reasonerName) {
        LOGGER_.info("Reasoner " + reasonerName + " disposed of");
        //Thread.dumpStack();
        System.out.flush();
        owlReasoner.dispose();
    }

}
