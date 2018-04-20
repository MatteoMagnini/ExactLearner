package org.zhaowl.console;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
        import org.semanticweb.owlapi.apibinding.OWLManager;
        import org.semanticweb.owlapi.model.IRI;
        import org.semanticweb.owlapi.model.OWLAxiom;
        import org.semanticweb.owlapi.model.OWLClass;
        import org.semanticweb.owlapi.model.OWLClassExpression;
        import org.semanticweb.owlapi.model.OWLDataFactory;
        import org.semanticweb.owlapi.model.OWLObjectProperty;
        import org.semanticweb.owlapi.model.OWLOntology;
        import org.semanticweb.owlapi.model.OWLOntologyCreationException;
        import org.semanticweb.owlapi.model.OWLOntologyManager;
        import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
        import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
        import org.semanticweb.owlapi.util.DefaultPrefixManager;

        import java.io.File;

public class QueryingUnnamedClassExpressions {

    public static void main(String[] args) throws OWLOntologyCreationException {

        Logger.getRootLogger().setLevel(Level.TRACE);
        consoleLearner maker = new consoleLearner();

        OWLOntologyManager man = OWLManager.createOWLOntologyManager();
        OWLDataFactory dataFactory = man.getOWLDataFactory();

        // Load your ontology.
        OWLOntology ont1 = man.loadOntologyFromOntologyDocument(new File("src/main/resources/ontologies/SMALL/football.owl"));
        OWLOntology ont2 = man.loadOntologyFromOntologyDocument(new File("src/main/resources/ontologies/SMALL/animals.owl"));

        // Create an ELK reasoner.
        OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();


        OWLReasoner reasoner1 = reasonerFactory.createReasoner(ont1); // persists
        OWLReasoner reasoner2 = reasonerFactory.createReasoner(ont2);




        // Create your desired query class expression. In this example we
        // will query ObjectIntersectionOf(A ObjectSomeValuesFrom(R B)).
        PrefixManager pm = new DefaultPrefixManager("http://www.semanticweb.org/ontologies/2014/3/animals.owl#");
        OWLClass A = dataFactory.getOWLClass(":Animal", pm);
        OWLObjectProperty R = dataFactory.getOWLObjectProperty(":eats", pm);
        OWLClass B = dataFactory.getOWLClass(":Meat", pm);
        OWLClassExpression query = dataFactory.getOWLObjectIntersectionOf(A, dataFactory.getOWLObjectSomeValuesFrom(R, B));

        // Create a fresh name for the query.
        OWLClass newName = dataFactory.getOWLClass(IRI.create("temp001"));


        reasoner2.getSubClasses(newName, true);

        reasoner2.dispose();

        // Make the query equivalent to the fresh class
        OWLAxiom definition = dataFactory.getOWLEquivalentClassesAxiom(newName, query);
        man.addAxiom(ont2, definition);

        // Remember to either flush the reasoner after the ontology change
        // or create the reasoner in non-buffering mode. Note that querying
        // a reasoner after an ontology change triggers re-classification of
        // the whole ontology which might be costly. Therefore, if you plan
        // to query for multiple complex class expressions, it will be more
        // efficient to add the corresponding definitions to the ontology at
        // once before asking any queries to the reasoner.

        reasoner2 = reasonerFactory.createReasoner(ont2);
        reasoner2.flush();

        // You can now retrieve subclasses, superclasses, and instances of
        // the query class by using its new name instead.
        reasoner2.getSubClasses(newName, true);
        NodeSet<OWLClass> superClasses = reasoner2.getSuperClasses(newName, true);
        System.out.println("(Strict) superClasses of " + newName + " ");
        for(OWLClass c : superClasses.getFlattened()) {
            System.out.println(c.toString());
        }

        Node<OWLClass> equivClassess = reasoner2.getEquivalentClasses(newName);
        System.out.println("equivalentClasses of " + newName + " ");
        for(OWLClass c : equivClassess.getEntities()) {
            System.out.println(c.toString());
        }
        reasoner2.dispose();

        reasoner2 = reasonerFactory.createReasoner(ont2);
        man.removeAxiom(ont2, definition);


        NodeSet<OWLClass> superClasses2 = reasoner2.getSuperClasses(newName, true);
        System.out.println("(Strict) superClasses of " + newName + " ");
        for(OWLClass c : superClasses2.getFlattened()) {
            System.out.println(c.toString());
        }

        Node<OWLClass> equivClassess2 = reasoner2.getEquivalentClasses(newName);
        System.out.println("equivalentClasses of " + newName + " ");
        for(OWLClass c : equivClassess2.getEntities()) {
            System.out.println(c.toString());
        }



        // After you are done with the query, you should remove the definition

        // You can now add new definitions for new queries in the same way

        // After you are done with all queries, do not forget to free the
        // resources occupied by the reasoner

        reasoner1.dispose();
    }
}