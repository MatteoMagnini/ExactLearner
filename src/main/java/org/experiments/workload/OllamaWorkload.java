package org.experiments.workload;

import org.exactlearner.connection.OllamaBridge;
import org.experiments.logger.SmartLogger;

import java.net.URL;
import java.net.URLConnection;

public class OllamaWorkload implements BaseWorkload {
    private final String model;
    private final String system;
    private final String query;
    private final int maxTokens;

    public OllamaWorkload(String model, String system, String query,  int maxTokens) {
        this.model = model;
        this.system = system;
        this.query = query;
        this.maxTokens = maxTokens;
    }

    @Override
    public void run() {
        checkSetup();
        OllamaBridge bridge = new OllamaBridge(model, maxTokens);
        checkConnection(bridge);
        String response = bridge.ask(query, system);
        SmartLogger.log(query + ", " + response);
    }

    private void checkConnection(OllamaBridge bridge) {
        try {
            URLConnection connection = new URL(bridge.getUrl()).openConnection();
            connection.connect();
        } catch (Exception e) {
            throw new IllegalStateException("Could not connect to the Ollama bridge.");
        }

    }

    private void checkSetup() {
        if (model == null || query == null) {
            throw new IllegalStateException("Model and query must be set up before running the workload.");
        }
    }
    public String getModel() {
        return model;
    }

    public String getSystem() {
        return system;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public String getQuery() {
        return query;
    }
}
