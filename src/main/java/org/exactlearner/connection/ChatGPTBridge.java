package org.exactlearner.connection;

public class ChatGPTBridge extends BasicBridge{

    private static final String model = "gpt-3.5-turbo";
    private static final String url = "https://api.openai.com/v1/chat/completions";

public ChatGPTBridge() {
        super();
        BasicBridge.model = model;
        BasicBridge.url = url;
    }

}
