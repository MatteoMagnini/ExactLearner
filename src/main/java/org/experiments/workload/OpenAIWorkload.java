package org.experiments.workload;

import org.exactlearner.connection.ChatGPTBridge;
import org.exactlearner.connection.OllamaBridge;
import org.experiments.logger.SmartLogger;

import java.net.Inet4Address;
import java.net.URL;
import java.net.URLConnection;

public class OpenAIWorkload implements BaseWorkload {
    private String query;
    private String system;

    public OpenAIWorkload() {
    }

    @Override
    public void run() {
        checkSetup();
        ChatGPTBridge bridge = new ChatGPTBridge();
        checkConnection(bridge);
        String response = bridge.ask(query, System.getenv("OPENAI_API_KEY"), system);
        SmartLogger.log(query + ", " + response);
    }

    private void checkSetup() {
        if (query == null) {
            throw new IllegalStateException("Query must be set up before running the workload.");
        }
    }

    private void checkConnection(ChatGPTBridge bridge) {
        try {
            URLConnection connection = new URL(bridge.getUrl()).openConnection();
            connection.connect();
        } catch (Exception e) {
            throw new IllegalStateException("Could not connect to the ChatGPT bridge.");
        }
    }

    public void setUp(String query, String system) {
        this.query = query;
        this.system = system;
    }
}
