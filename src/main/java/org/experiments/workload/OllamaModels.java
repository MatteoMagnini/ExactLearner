package org.experiments.workload;

public enum OllamaModels {
    DOLPHIN_MIXTRAL("dolphin-mixtral"),
    GOLIATH("goliath"),
    LLAMA2_70B("llama-70b"),
    LLAMA2_13B("llama-13b"),
    LLAMA2_7B("llama-7b"),
    MIXTRAL("mixtral"),
    NOTUX("notux"),
    NOUS_HERMES_2_MIXTRAL("nous-hermes-2-mixtral"),
    MISTRAL("mistral");

    private final String modelName;

    OllamaModels(String modelName) {
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }
}
