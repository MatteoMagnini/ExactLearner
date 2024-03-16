package org.experiments.workload;

import org.exactlearner.connection.OllamaBridge;
import org.experiments.logger.SmartLogger;

import java.net.Inet4Address;
import java.net.URL;
import java.net.URLConnection;

public class OllamaWorkload implements BaseWorkload {
    private String query;
    private String model;
    private String system;

    public OllamaWorkload() {
    }

    @Override
    public void run() {
        checkSetup();
        OllamaBridge bridge = new OllamaBridge(model);
        checkConnection(bridge);
        String response = bridge.ask(query,system);
        SmartLogger.log(query);
        SmartLogger.log(", ");
        SmartLogger.log(response);
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

    public void setUp(String model, String query, String system){
        this.model = model;
        this.query = query;
        this.system = system;
    }
}
