package org.exactlearner.parser;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class OWLParserImpl implements OWLParser {
    private OWLOntology owl;

    public OWLParserImpl(String pathOfFile) {
        System.out.println("Parsing file: " + pathOfFile);
        try {
            loadFile(pathOfFile);
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }
    }

    public OWLParserImpl(OWLOntology owl) {
        this.owl = owl;
    }

    private void loadFile(String pathOfFile) throws OWLOntologyCreationException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        owl = manager.loadOntologyFromOntologyDocument(new File(pathOfFile));
    }

    @Override
    public Optional<Set<OWLClass>> getClasses() {
        return Optional.ofNullable(owl.getClassesInSignature());
    }

    public OWLOntology getOwl() {
        return owl;
    }

    @Override
    public Set<String> getClassesNamesAsString() {
        if (this.getClasses().isEmpty()) {
            return new HashSet<>();
        }
        return this.getClasses().get().stream()
                .map(OWLClass::toString)
                .map(s -> s.substring(s.indexOf("#") + 1, s.length() - 1))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<OWLAxiom> getAxioms() {
        return owl.getAxioms();
    }

    @Override
    public Set<OWLObjectProperty> getObjectProperties() {
        return owl.getObjectPropertiesInSignature();
    }


}
