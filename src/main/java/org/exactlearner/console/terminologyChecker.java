package org.exactlearner.console;


import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.util.Set;

public class terminologyChecker {



    public static void main(String[] args) {
        try {
            // targetOntology from parameters
            String filePath = args[0];

            System.out.println("Trying to load targetOntology");
            File targetFile = new File(filePath);

            OWLOntologyManager myManager = OWLManager.createOWLOntologyManager();
            OWLOntology targetOntology = myManager.loadOntologyFromOntologyDocument(targetFile);

            Boolean terminology = true;

            for (OWLAxiom axe : targetOntology.getAxioms()) {
                if (axe.isOfType(AxiomType.SUBCLASS_OF)) {
                    OWLSubClassOfAxiom sax = (OWLSubClassOfAxiom) axe;
                    if (sax.getSubClass().isAnonymous() && sax.getSuperClass().isAnonymous()) {
                        terminology = false;
                    }
                }
                if (axe.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
                    OWLEquivalentClassesAxiom rex = (OWLEquivalentClassesAxiom) axe;
                    Set<OWLSubClassOfAxiom> eqsubclassaxioms = rex.asOWLSubClassOfAxioms();

                    for (OWLSubClassOfAxiom subClassAxiom : eqsubclassaxioms) {
                        if (subClassAxiom.getSubClass().isAnonymous() && subClassAxiom.getSuperClass().isAnonymous()) {
                            terminology = false;
                        }
                    }
                }
            }
            System.out.print(filePath + ":  ");
            if (terminology) {
                System.out.println("Terminology");
            }
            else {
                System.out.println("Not a terminology");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
