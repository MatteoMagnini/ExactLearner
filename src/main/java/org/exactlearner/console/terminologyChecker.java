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

            for (OWLAxiom axe : targetOntology.getLogicalAxioms()) {




                if (axe.isOfType(AxiomType.SUBCLASS_OF)) {
                    OWLSubClassOfAxiom sax = (OWLSubClassOfAxiom) axe;
                    if (sax.getSubClass().isAnonymous() && sax.getSuperClass().isAnonymous()) {
                        terminology = false;
                        break;
                    }
                } else if (axe.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
                    OWLEquivalentClassesAxiom rex = (OWLEquivalentClassesAxiom) axe;
                    Set<OWLSubClassOfAxiom> eqsubclassaxioms = rex.asOWLSubClassOfAxioms();

                    for (OWLSubClassOfAxiom subClassAxiom : eqsubclassaxioms) {
                        if (subClassAxiom.getSubClass().isAnonymous() && subClassAxiom.getSuperClass().isAnonymous()) {
                            terminology = false;
                            break;
                        }
                        if(!terminology)
                            break;
                    }
                    // ignoring the object properties, data properties and ABox
                } else if (axe.isOfType(AxiomType.CLASS_ASSERTION)
                        || axe.isOfType(AxiomType.ASYMMETRIC_OBJECT_PROPERTY)
                        || axe.isOfType(AxiomType.EQUIVALENT_DATA_PROPERTIES)
                        || axe.isOfType(AxiomType.OBJECT_PROPERTY_ASSERTION)
                        || axe.isOfType(AxiomType.OBJECT_PROPERTY_DOMAIN)
                        || axe.isOfType(AxiomType.OBJECT_PROPERTY_RANGE)
                        || axe.isOfType(AxiomType.SUB_OBJECT_PROPERTY)
                        || axe.isOfType(AxiomType.SUB_PROPERTY_CHAIN_OF)
                        || axe.isOfType(AxiomType.INVERSE_OBJECT_PROPERTIES)
                        || axe.isOfType(AxiomType.SYMMETRIC_OBJECT_PROPERTY)
                        || axe.isOfType(AxiomType.TRANSITIVE_OBJECT_PROPERTY)
                        || axe.isOfType(AxiomType.DATA_PROPERTY_ASSERTION)
                        || axe.isOfType(AxiomType.DATA_PROPERTY_DOMAIN)
                        || axe.isOfType(AxiomType.DATA_PROPERTY_RANGE)) {
                    //skip
                }
                else {
                    System.out.println(axe.toString());
                    System.out.println(axe.getAxiomType().toString());
                    terminology = false;
                    break;
                }
            }
            System.out.print(filePath + ":  ");
            if (terminology) {
                System.out.println("Terminology");
            } else {
                System.out.println("Not a terminology");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
