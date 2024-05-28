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

    public OWLParserImpl(String pathOfFile, OWLOntologyManager manager) {
        System.out.println("Parsing file: " + pathOfFile);
        try {
            loadFile(pathOfFile, manager);
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }
    }

    public OWLParserImpl(OWLOntology owl) {
        this.owl = owl;
    }

    private void loadFile(String pathOfFile, OWLOntologyManager manager) throws OWLOntologyCreationException {
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
                .map(s -> s.substring(s.indexOf("#") + 1, s.length() - 1)).map(s -> s.substring(s.lastIndexOf("/") + 1))
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

    @Override
    public Set<String> getObjectPropertiesAsString() {
        return owl.getObjectPropertiesInSignature().stream()
                .map(Object::toString)
                .map(s -> s.split("#")[1].replace(">", ""))
                .collect(Collectors.toSet());
    }



}
