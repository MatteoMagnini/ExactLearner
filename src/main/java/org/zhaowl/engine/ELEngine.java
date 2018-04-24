package org.zhaowl.engine;

import java.util.Set;

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ELEngine {
	private final OWLReasoner myReasoner;
	private final OWLOntology myOntology;
	private final OWLOntologyManager myManager;
    private final ELParser myParser;
    private static final Logger LOGGER_ = LoggerFactory
            .getLogger(ELEngine.class);
    /** Constructs a ELQueryEngine. This will answer "DL queries" using the
     * specified myReasoner. A short form provider specifies how entities are
     * rendered.
     *
     * @param ontology reasoning engine for the given ontology*/
    public ELEngine(OWLOntology ontology) {
        myOntology = ontology;
        myManager = myOntology.getOWLOntologyManager();
        myReasoner = createReasoner(ontology);
        ShortFormProvider myShortFormProvider = new SimpleShortFormProvider();
        myParser = new ELParser(myOntology, myShortFormProvider);
    }

    public OWLOntology getOntology(){
    	return myOntology;
    } 
    public Set<OWLClass> getClassesInSignature() {
        return myOntology.getClassesInSignature();
    }

//    public OWLClassExpression parseClassExpression(String s){
//    	return myParser.parseClassExpression(s);
//	}
    
//	public OWLAxiom parseToOWLSubClassOfAxiom(String concept1String, String concept2String){
//    	OWLClassExpression concept1 = myParser.parseClassExpression(concept1String);
//		OWLClassExpression concept2 = myParser.parseClassExpression(concept2String);
//		return getSubClassAxiom(concept1, concept2);
//	}
	
	public OWLAxiom getSubClassAxiom(OWLClassExpression concept1, OWLClassExpression concept2){
		return myParser.parseSubClassOfAxiom(concept1, concept2);
    }

    private Boolean entailedEQ(OWLSubClassOfAxiom subclassAxiom) {
        OWLClassExpression left  = subclassAxiom.getSubClass();
        OWLClassExpression right = subclassAxiom.getSuperClass();

        Boolean workaround = false;
        myReasoner.flush();

        OWLDataFactory dataFactory = myManager.getOWLDataFactory();

        OWLClass leftName  = dataFactory.getOWLClass(IRI.create("#temp001"));
        OWLClass rightName = dataFactory.getOWLClass(IRI.create("#temp002"));

        OWLAxiom leftDefinition  = dataFactory.getOWLEquivalentClassesAxiom(leftName,  left);
        OWLAxiom rightDefinition = dataFactory.getOWLEquivalentClassesAxiom(rightName, right);
        myManager.addAxiom(myReasoner.getRootOntology(), leftDefinition);
        myManager.addAxiom(myReasoner.getRootOntology(), rightDefinition);

        /*
        LOGGER_.trace("ontology: ");
        for(OWLAxiom ax : myReasoner.getRootOntology().getAxioms())
            LOGGER_.trace(ax.toString());
         */
        //System.out.println("SubclassAx: " + subclassAxiom.toString());

        myReasoner.flush();
        myReasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

        NodeSet<OWLClass> superClasses = myReasoner.getSuperClasses(leftName, false);
        Node<OWLClass> equivClasses = myReasoner.getEquivalentClasses(leftName);

        /*
        LOGGER_.trace("(Strict) superClasses of " + leftName + " ");
        for(OWLClass c : superClasses.getFlattened()) {
            LOGGER_.trace(c.toString());
        }
        LOGGER_.trace("");
        */
        if (!superClasses.isEmpty() && superClasses.containsEntity(rightName))
            workaround = true;

        /*
        LOGGER_.trace("equivalentClasses of " + leftName + " ");
        for(OWLClass c : equivClasses.getEntities()) {
            LOGGER_.trace(c.toString());
        }
        LOGGER_.trace("");
        */
        if (!equivClasses.getEntities().isEmpty() && equivClasses.getEntities().contains(rightName))
            workaround = true;


        myManager.removeAxiom(myReasoner.getRootOntology(), leftDefinition);
        myManager.removeAxiom(myReasoner.getRootOntology(), rightDefinition);
        myReasoner.flush();
        LOGGER_.trace("returning " + workaround);
        LOGGER_.trace("");
        LOGGER_.trace("");

        return workaround;
    }

	public Boolean entailed(OWLAxiom ax) {
        LOGGER_.trace("InputAx: {}", ax.toString());
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
            return entailedEQ((OWLSubClassOfAxiom) ax);
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
     * @param superclass
     *            The string from which the class expression will be parsed.
     * @param direct
     *            Specifies whether direct superclasses should be returned or
     *            not.
     * @return The superclasses of the specified class expression If there was a
     *         problem parsing the class expression. */
	
    public Set<OWLClass> getSuperClasses(OWLClassExpression superclass, boolean direct) {
        NodeSet<OWLClass> superClasses = myReasoner.getSuperClasses(superclass, direct);

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
        NodeSet<OWLClass> subClasses = myReasoner.getSubClasses(subclass, direct);
        return subClasses.getFlattened();
    }

    private OWLReasoner createReasoner(final OWLOntology rootOntology) {
        LOGGER_.trace("Reasoner created");
        //Thread.dumpStack();
        System.out.flush();
        ElkReasonerFactory reasoningFactory = new ElkReasonerFactory();
        return reasoningFactory.createReasoner(rootOntology);
    }
    public void disposeOfReasoner() {
        LOGGER_.trace("Reasoner "  + " disposed of");
        //Thread.dumpStack();
        System.out.flush();
        myReasoner.dispose();
    }
    public void applyChange(OWLOntologyChange change) {
    	myManager.applyChange(change);
    }	
    	//	public Reasoner getOWLObjectIntersectionOf(List<OWLClass> classAux) {


    
 
}
