package org.zhaowl.console;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.semanticweb.HermiT.Reasoner;
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
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
        import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
        import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.zhaowl.engine.ELEngine;
import org.zhaowl.tree.ELTree;

import java.io.File;

public class QueryingUnnamedClassExpressionsDiffReasoners {

    public static void main(String[] args) throws OWLOntologyCreationException {

        Logger.getRootLogger().setLevel(Level.TRACE);
        consoleLearner maker = new consoleLearner();

        OWLOntologyManager man = OWLManager.createOWLOntologyManager();
        OWLDataFactory dataFactory = man.getOWLDataFactory();

        // Load your ontology.
        OWLOntology ont = man.loadOntologyFromOntologyDocument(new File("src/main/resources/ontologies/animalsTest.owl"));

        // Create an ELK reasoner.
        OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();


         
        OWLReasoner elkReasoner = reasonerFactory.createReasoner(ont);

 
        // Create your desired query class expression. In this example we
        // will query ObjectIntersectionOf(A ObjectSomeValuesFrom(R B)).
        PrefixManager pm = new DefaultPrefixManager("http://www.semanticweb.org/ontologies/2014/3/animals.owl#");
        OWLObjectProperty R1 = dataFactory.getOWLObjectProperty(":eats", pm);
        OWLObjectProperty R2 = dataFactory.getOWLObjectProperty(":is_alive", pm);
        OWLClass A = dataFactory.getOWLThing();
        OWLClass B = dataFactory.getOWLClass(":Living_Being", pm);
        OWLClassExpression query = dataFactory.getOWLObjectIntersectionOf(A, dataFactory.getOWLObjectSomeValuesFrom(R1, B));
        OWLClassExpression query2 = dataFactory.getOWLObjectSomeValuesFrom(R2, A);
        // Create a fresh name for the query.
        OWLClass newName = dataFactory.getOWLClass(IRI.create("temp001"));
        OWLClass newName2 = dataFactory.getOWLClass(IRI.create("temp002"));
        elkReasoner.flush();
        elkReasoner.getSubClasses(newName, true);
        
        elkReasoner.dispose();

       
       
        // Make the query equivalent to the fresh class
        OWLAxiom definition = dataFactory.getOWLEquivalentClassesAxiom(newName, query);
        man.addAxiom(ont, definition);
        OWLAxiom definition2 = dataFactory.getOWLEquivalentClassesAxiom(newName2, query2);
        man.addAxiom(ont, definition2);
        // Remember to either flush the reasoner after the ontology change
        // or create the reasoner in non-buffering mode. Note that querying
        // a reasoner after an ontology change triggers re-classification of
        // the whole ontology which might be costly. Therefore, if you plan
        // to query for multiple complex class expressions, it will be more
        // efficient to add the corresponding definitions to the ontology at
        // once before asking any queries to the reasoner.
 
        elkReasoner = reasonerFactory.createReasoner(ont);
        elkReasoner.flush();
        NodeSet<OWLClass> subClasses3=elkReasoner.getSubClasses(newName2, false);
    
        // You can now retrieve subclasses, superclasses, and instances of
        // the query class by using its new name instead.
        elkReasoner.getSubClasses(newName, true);
        
     
     
        System.out.println("(Strict) superClasses of " + newName2 + " ");
        NodeSet<OWLClass> superClasses =elkReasoner.getSuperClasses(newName2, true);
        for(OWLClass c : superClasses.getFlattened()) {
            System.out.println(c.toString());
            if(superClasses.containsEntity(newName))
            	System.out.println("Entailed!");
        }
   
        elkReasoner.dispose();
     
        OWLSubClassOfAxiom axiom=dataFactory.getOWLSubClassOfAxiom(newName2, newName);
        OWLReasoner hermitReasoner = new Reasoner.ReasonerFactory().createReasoner(ont);
        hermitReasoner.flush();
        if (hermitReasoner.isEntailed( axiom))
        	System.out.println("Is entailed");
        ShortFormProvider shortFormProvider =new SimpleShortFormProvider();
        ELEngine engineForT=new ELEngine(ont );
        ELTree treeRight = null;
        ELTree treeLeft = null;
		try {
			treeRight = new ELTree(query2 );
			treeLeft  = new ELTree(query);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 
 
		if(engineForT.parseClassExpression(treeLeft.toDescriptionString())!=null
				&&
				engineForT.parseClassExpression(treeRight.toDescriptionString())!=null
				&&
				dataFactory.getOWLSubClassOfAxiom(
		        		engineForT.parseClassExpression(treeLeft.toDescriptionString()), 
		        		  engineForT.parseClassExpression(treeRight.toDescriptionString()))!=null
				)
			if (hermitReasoner.isEntailed( 
        		dataFactory.getOWLSubClassOfAxiom(
        		engineForT.parseClassExpression(treeLeft.toDescriptionString()), 
        		  engineForT.parseClassExpression(treeRight.toDescriptionString())))) 				 
				System.out.println("We are not getting here!");
        elkReasoner = reasonerFactory.createReasoner(ont);
        man.removeAxiom(ont, definition);
        man.removeAxiom(ont, definition2);
        System.out.println("After removing");
         
       
        NodeSet<OWLClass>   superClasses2 = elkReasoner.getSuperClasses(newName2, true);
        System.out.println("(Strict) superClasses of " + newName2 + " ");
        for(OWLClass c : superClasses2.getFlattened()) {
            System.out.println(c.toString());
        }

        

        // After you are done with the query, you should remove the definition

        // You can now add new definitions for new queries in the same way

        // After you are done with all queries, do not forget to free the
        // resources occupied by the reasoner

        hermitReasoner.dispose();
    }
}