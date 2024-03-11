package org.exactlearner.parser;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.util.Optional;
import java.util.Set;

public class OWLParserImpl implements OWLParser {
    private OWLOntology owl;

    public OWLParserImpl(String pathOfFile) throws OWLOntologyCreationException {
        System.out.println("Parsing file: " + pathOfFile);
        loadFile(pathOfFile);
    }

    private void loadFile(String pathOfFile) throws OWLOntologyCreationException {
        OWLDataFactory df = OWLManager.getOWLDataFactory();
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        owl = manager.loadOntologyFromOntologyDocument(new File(pathOfFile));
    }

    @Override
    public Optional<Set<OWLClass>> getClasses() {
        return Optional.ofNullable(owl.getClassesInSignature());
    }
}
