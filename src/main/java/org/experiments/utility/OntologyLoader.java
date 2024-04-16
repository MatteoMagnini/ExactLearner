package org.experiments.utility;

import org.exactlearner.parser.OWLParser;
import org.exactlearner.parser.OWLParserImpl;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class OntologyLoader {
    public OWLParser getParser(String ontologyFilePath) {
        try {
            return new OWLParserImpl(ontologyFilePath);
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }
    }
}
