package org.analysis.exp1.axioms;

import org.analysis.common.JenaQueryExecutor;

public class JenaQueryAxiomsExecutor extends JenaQueryExecutor {

    String query;
    String ontology;

    public JenaQueryAxiomsExecutor(String query, String ontology) {
        this.query = parseQuery(query);
        this.ontology = ontology;
    }

    private String parseQuery(String query) {
        //TODO: Implement this method
        return null;
    }
    public String executeQuery() {
        return super.executeQuery(query, ontology);
    }
}
