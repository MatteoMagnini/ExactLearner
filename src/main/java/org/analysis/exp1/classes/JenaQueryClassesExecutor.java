package org.analysis.exp1.classes;

import org.analysis.common.JenaQueryExecutor;

public class JenaQueryClassesExecutor extends JenaQueryExecutor {
    String query;
    String ontology;

    public JenaQueryClassesExecutor(String query, String ontology) {
        this.query = parseQuery(query);
        this.ontology = ontology;
    }

    private String parseQuery(String query) {
        query = "<" + query + ">";
        query = query.replaceFirst(" ", "> ");
        query = query.replaceFirst("SubClassOf ", "SubClassOf <");
        query = query.replace("SubClassOf", "rdfs:subClassOf");
        return query;
    }

    public String executeQuery() {
        return super.executeQuery(query, ontology);
    }
}
