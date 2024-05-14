package org.experiments.workload;

import org.exactlearner.connection.OllamaBridge;
import org.experiments.logger.SmartLogger;

import java.net.URL;
import java.net.URLConnection;
import java.util.List;

public class OllamaWorkload implements BaseWorkload {
    private final String model;
    private final String system;
    private final String query;
    private final int maxTokens;
    public static final List<String> supportedModels = List.of("mistral", "mixtral", "llama2", "llama2:13b","llama2:70b","megadolphin","llama3","llava-llama3","llama3:70b","llama3-chatqa","dolphin-llama3");

    public OllamaWorkload(String model, String system, String query,  int maxTokens) {
        this.model = model;
        this.system = system;
        this.query = query;
        this.maxTokens = maxTokens;
    }

    @Override
    public void run() {
        OllamaBridge bridge = new OllamaBridge("clusters.almaai.unibo.it",11434, model,maxTokens);
        //OllamaBridge bridge =  new OllamaBridge(model,maxTokens);
        checkConnection(bridge);
        String response = bridge.ask(query, system);
        // Sleep for 100 milliseconds to avoid overloading the Ollama bridge and retrying the request
        if (response == null) {
            int maxRetries = 2; // So the total number of retries is 3
            for (int i = 0; i < maxRetries; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                response = bridge.ask(query, system);
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

    private void checkConnection(OllamaBridge bridge) {
        try {
            URLConnection connection = new URL(bridge.getUrl()).openConnection();
            connection.connect();
        } catch (Exception e) {
            throw new IllegalStateException("Could not connect to the Ollama bridge.");
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
