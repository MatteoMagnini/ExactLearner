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
    public void testHuggingFaceBridgeConnection() {
        // Test the ChatGPTBridge
        HuggingFaceBridge huggingFaceBridge = new HuggingFaceBridge("gpt2");
        assertTrue(huggingFaceBridge.checkConnection());
    }

    @Test
    public void testHuggingFaceBridgeAsk() {
        String api_key = System.getenv("HUGGINGFACE_API_KEY");

        // Test the ChatGPTBridge
        HuggingFaceBridge huggingFaceBridge = new HuggingFaceBridge("gpt2");
        String response = huggingFaceBridge.ask("What\'s the temperature of the Sun?", api_key);
        assertNotNull(response);
        System.out.println(response);
    }

    @Test
    public void testChatGPTBridgeConnection() {
        // Test the ChatGPTBridge
        ChatGPTBridge chatGPTBridge = new ChatGPTBridge();
        assertTrue(chatGPTBridge.checkConnection());
    }

    @Test
    public void testChatGPTBridgeAsk() {
        String api_key = System.getenv("OPENAI_API_KEY");

        // Test the ChatGPTBridge
        ChatGPTBridge chatGPTBridge = new ChatGPTBridge();
        String response = chatGPTBridge.ask("Hello, how are you?", api_key);
        assertNotNull(response);
    }
}
