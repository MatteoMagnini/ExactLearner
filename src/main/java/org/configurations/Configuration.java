package org.configurations;
import java.util.List;

public class Configuration {

    private List<String> models;
    private List<String> ontologies;
    private String system;
    private int maxTokens;
    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

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
        this.system = system.trim();
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
                ", type='" + type + '\'' +
                '}';
    }
}
