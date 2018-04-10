package org.zhaowl.userInterface;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.ShortFormProvider;

public class ELEngine {
	public final OWLReasoner reasoner;
    public final ELParser parser;

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
    
	public Boolean entailed(OWLAxiom subclassAxiom){
		return reasoner.isEntailed(subclassAxiom);
    }
	
	
	public Boolean entailed(Set<OWLAxiom> axioms) {
		return reasoner.isEntailed(axioms);
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
}
