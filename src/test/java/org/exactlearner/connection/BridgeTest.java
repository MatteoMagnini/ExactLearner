package org.exactlearner.connection;
import org.junit.Test;

public class BridgeTest {

    @Test
    public void testEnvironmentVariables() {
        // Test that the user set the OPENAI_API_KEY environment variable
        if (System.getenv("OPENAI_API_KEY") == null) {
            System.out.println("Please set the OPENAI_API_KEY environment variable.");
            System.exit(1);
        }
    }

    @Test
    public void testChatGPTBridge() {
        String api_key = System.getenv("OPENAI_API_KEY");
        // Get only the value of the OPENAI_API_KEY environment variable
        api_key = api_key.split("=")[1];

        // Test the ChatGPTBridge
        ChatGPTBridge chatGPTBridge = new ChatGPTBridge();
        System.out.println(chatGPTBridge.checkConnection("https://api.openai.com/v1/chat/completions", api_key));
        System.out.println(chatGPTBridge.ask("Hello", api_key));
    }
}
