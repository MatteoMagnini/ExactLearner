package org.experiments.workload;

import org.experiments.Environment;
import org.experiments.Result;
import org.experiments.task.ExperimentTask;
import org.experiments.task.Task;

public class WorkloadManagerImpl implements WorkloadManager {
    private final String model;
    private final String system;
    private final int maxTokens;
    private final String queryFormat;
    private final String ontologyName;

    public WorkloadManagerImpl(String model, String system, int maxTokens, String queryFormat, String ontologyName) {
        this.model = model;
        this.system = system;
        this.maxTokens = maxTokens;
        this.queryFormat = queryFormat;
        this.ontologyName = ontologyName;
    }

    public boolean runWorkload(String message) {
        Runnable work;
        if (OllamaWorkload.supportedModels.contains(model)) {
            work = new OllamaWorkload(model, system, message, maxTokens);
        } else if (OpenAIWorkload.supportedModels.contains(model)) {
            work = new OpenAIWorkload(model, system, message, maxTokens);
        } else {
            throw new IllegalStateException("Invalid model " + model);
        }
        Task task = new ExperimentTask("statementsQuerying", model, queryFormat, ontologyName, message, system, work);
        Environment.run(task);

        return new Result(task.getFileName()).isStrictlyTrue(message);
    }
}
