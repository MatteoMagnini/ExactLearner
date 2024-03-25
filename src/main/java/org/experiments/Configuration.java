package org.experiments;

import java.util.ArrayList;
import java.util.List;

public class Configuration {

    private List<String> models;
    private List<String> ontologies;
    private String system;
    private int maxTokens;

    public List<String> getModels() {
        return models;
    }

    public void setModels(List<String> models) {
        this.models = models;
    }

    public List<String> getOntologies() {
        return ontologies;
    }

    public void setOntologies(List<String> ontologies) {
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


    public String toString() {
        return "Configuration{" +
                "models=" + models +
                ", ontologies=" + ontologies +
                ", system='" + system + '\'' +
                ", maxTokens=" + maxTokens +
                '}';
    }
}
