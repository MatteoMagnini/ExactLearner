package org.experiments;

import java.util.ArrayList;

public class Configuration {

    private ArrayList<String> models;
    private ArrayList<String> ontologies;
    private String system;
    private int maxTokens;

    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ArrayList<String> getModels() {
        return models;
    }

    public void setModels(ArrayList<String> models) {
        this.models = models;
    }

    public ArrayList<String> getOntologies() {
        return ontologies;
    }

    public void setOntologies(ArrayList<String> ontologies) {
        this.ontologies = ontologies;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Configuration(ArrayList<String> models, ArrayList<String> ontologies, String system, int maxTokens, String type) {
        this.models = models;
        this.ontologies = ontologies;
        this.system = system;
        this.maxTokens = maxTokens;
        this.type = type;
    }
}
