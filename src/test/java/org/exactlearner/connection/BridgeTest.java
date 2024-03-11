package org.exactlearner.connection;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
    public void testChatGPTBridgeConnection() {
        String api_key = System.getenv("OPENAI_API_KEY").split("=")[1];

        // Test the ChatGPTBridge
        ChatGPTBridge chatGPTBridge = new ChatGPTBridge();
        assertTrue(chatGPTBridge.checkConnection(api_key));
    }

    @Test
    public void testChatGPTBridgeAsk() {
        String api_key = System.getenv("OPENAI_API_KEY").split("=")[1];

        // Test the ChatGPTBridge
        ChatGPTBridge chatGPTBridge = new ChatGPTBridge();
        String response = chatGPTBridge.ask("Hello, how are you?", api_key);
        assertNotNull(response);
    }
}
