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
        ChatGPTBridge bridge = new ChatGPTBridge(model, maxTokens);
        checkConnection(bridge);
        String response = bridge.ask(query, System.getenv("OPENAI_API_KEY"), system);
        // Sleep for 100 milliseconds to avoid overloading the Ollama bridge and retrying the request
        if (response == null) {
            int maxRetries = 2; // So the total number of retries is 3
            for (int i = 0; i < maxRetries; i++) {
                try {
                    Thread.sleep(1000 * (i + 1));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                response = bridge.ask(query, System.getenv("OPENAI_API_KEY"), system);
                if (response != null) {
                    break;
                }
            }
        }
        if (response == null) {
            System.out.println("Could not get a response from the Ollama bridge.");
            System.out.println("Check file " + SmartLogger.getFilename() + " for more information.");
            response = "";
        }
        SmartLogger.log(query + ", " + response);
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
