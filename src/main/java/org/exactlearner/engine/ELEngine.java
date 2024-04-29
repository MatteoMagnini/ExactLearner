package org.exactlearner.engine;

import java.util.Set;

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ELEngine implements BaseEngine{
	private final OWLReasoner myReasoner;
	private final OWLOntology myOntology;
	private final OWLOntologyManager myManager;
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
    }

    public OWLOntology getOntology(){
    	return myOntology;
    } 
    public Set<OWLClass> getClassesInSignature() {
        return myOntology.getClassesInSignature();
    }
	
	public OWLSubClassOfAxiom getSubClassAxiom(OWLClassExpression concept1, OWLClassExpression concept2){
		return myManager.getOWLDataFactory().getOWLSubClassOfAxiom(concept1, concept2);
    }

	public OWLEquivalentClassesAxiom getOWLEquivalentClassesAxiom(OWLClassExpression concept1, OWLClassExpression concept2){
		return myManager.getOWLDataFactory().getOWLEquivalentClassesAxiom(concept1, concept2);
    }

	public OWLClassExpression getOWLObjectIntersectionOf(Set<OWLClassExpression>mySet){
		return myManager.getOWLDataFactory().getOWLObjectIntersectionOf(mySet);
    }
	
    private Boolean entailedEQ(OWLSubClassOfAxiom subclassAxiom) {
        OWLClassExpression left  = subclassAxiom.getSubClass();
        OWLClassExpression right = subclassAxiom.getSuperClass();

        Boolean workaround = false;

        OWLDataFactory dataFactory = myManager.getOWLDataFactory();

        OWLClass leftName;
        OWLAxiom leftDefinition = null;

        if(left.isAnonymous()) {
            leftName  = dataFactory.getOWLClass(IRI.create("#temp001"));
            leftDefinition  = dataFactory.getOWLSubClassOfAxiom(leftName,  left);
            myManager.addAxiom(myReasoner.getRootOntology(), leftDefinition);
        }
        else {
            leftName = left.asOWLClass();
        }


        OWLClass rightName;
        OWLAxiom rightDefinition = null;
        if(right.isAnonymous()) {
            rightName = dataFactory.getOWLClass(IRI.create("#temp002"));
            rightDefinition = dataFactory.getOWLSubClassOfAxiom(right, rightName);
            myManager.addAxiom(myReasoner.getRootOntology(), rightDefinition);
        }
        else {
            rightName = right.asOWLClass();
        }


        myReasoner.flush();

        NodeSet<OWLClass> superClasses = myReasoner.getSuperClasses(leftName, false);


        if (!superClasses.isEmpty() && superClasses.containsEntity(rightName)) {
            workaround = true;
        }
        else {
            Node<OWLClass> equivClasses = myReasoner.getEquivalentClasses(leftName);
            if (!equivClasses.getEntities().isEmpty() && equivClasses.getEntities().contains(rightName)) {
                workaround = true;
            }
        }


        if(leftDefinition != null) {
            myManager.removeAxiom(myReasoner.getRootOntology(), leftDefinition);
        }

        if(rightDefinition != null) {
            myManager.removeAxiom(myReasoner.getRootOntology(), rightDefinition);
        }

        LOGGER_.trace("returning " + workaround);
        return workaround;
    }

	public Boolean entailed(OWLAxiom ax) {
        LOGGER_.trace("InputAx: {}", ax.toString());
        if(ax.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
            OWLEquivalentClassesAxiom eax = (OWLEquivalentClassesAxiom) ax;
            for(OWLSubClassOfAxiom sax : eax.asOWLSubClassOfAxioms()) {
                if(!entailedEQ(sax)) {
                    
                    return false;
                }
            }
            
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
                 
                return false;
            }
        }
         
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

// --Commented out by Inspection START (30/04/2018, 15:29):
//    /** Gets the subclasses of a class expression parsed from a string.
//     *
//     * @param subclass
//     *            The string from which the class expression will be parsed.
//     * @param direct
//     *            Specifies whether direct subclasses should be returned or not.
//     * @return The subclasses of the specified class expression If there was a
//     *         problem parsing the class expression. */
//    public Set<OWLClass> getSubClasses(OWLClassExpression subclass, boolean direct) {
//        NodeSet<OWLClass> subClasses = myReasoner.getSubClasses(subclass, direct);
//        return subClasses.getFlattened();
//    }
// --Commented out by Inspection STOP (30/04/2018, 15:29)

    private OWLReasoner createReasoner(final OWLOntology rootOntology) {
        LOGGER_.trace("Reasoner created");
         
        System.out.flush();
        ElkReasonerFactory reasoningFactory = new ElkReasonerFactory();
        return reasoningFactory.createReasoner(rootOntology);
    }
    public void disposeOfReasoner() {
        LOGGER_.trace("Reasoner "  + " disposed of");
         
        System.out.flush();
        myReasoner.dispose();
    }
    public void applyChange(OWLOntologyChange change) {
    	myManager.applyChange(change);
    }	
    	 


    
 
}
