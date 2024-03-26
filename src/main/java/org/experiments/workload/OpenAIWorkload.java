package org.experiments.workload;

import org.exactlearner.connection.ChatGPTBridge;
import org.exactlearner.connection.OllamaBridge;
import org.experiments.logger.SmartLogger;

import java.net.Inet4Address;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

public class OpenAIWorkload implements BaseWorkload {

    private final String model;
    private final String system;
    private final String query;
    private final int maxTokens;
    public static final List<String> supportedModels = List.of("gpt-3.5-turbo");

    public OpenAIWorkload(String model, String system, String query, int maxTokens) {
        this.model = model;
        this.system = system;
        this.query = query;
        this.maxTokens = maxTokens;
    }

    @Override
    public void run() {
        checkSetup();
        ChatGPTBridge bridge = new ChatGPTBridge(model, maxTokens);
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
}
