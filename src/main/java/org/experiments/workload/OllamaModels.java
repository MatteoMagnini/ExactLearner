package org.experiments.workload;

public enum OllamaModels {
    LLAMA2_13B("llama-13b"),
    LLAMA2_7B("llama-7b"),
    MIXTRAL("mixtral"),
    MISTRAL("mistral");

    private final String modelName;

    OllamaModels(String modelName) {
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }
}
