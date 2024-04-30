package org.experiments.task;

import static org.utility.SHA256Hash.sha256;

public class ExperimentTask implements Task {

    private final String taskName;
    private final String modelName;
    private final String ontology;
    private final String query;
    private final String system;

    private Runnable workload;

    public ExperimentTask(String taskName, String modelName, String ontology, String query, String system, Runnable workload) {
        this.taskName = taskName;
        this.modelName = modelName;
        this.ontology = ontology;
        this.query = query;
        this.workload = workload;
        this.system = system;

    }

    public String getTaskName() {
        return taskName;
    }

    public String getModelName() {
        return modelName;
    }

    public String getOntology() {
        return ontology;
    }

    public String getQuery() {
        return query;
    }

    public String SHA256Hash(String input) {
        return sha256(input);
    }

    public String getFileName() {
        return SHA256Hash(taskName + modelName + ontology + query + system);
    }

    public String getSystem() {
        return system;
    }

    @Override
    public void run() {
        workload.run();
    }

    public int hashCode() {
        return taskName.hashCode() + modelName.hashCode() + ontology.hashCode() + query.hashCode() + system.hashCode();
    }

    public String toString() {
        return "Task:\n\tName: " + taskName + "\n\tModel: " + modelName + "\n\tOntology: " + ontology + "\n\tQuery: " + query + "\n\tSystem: " + system;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != this.getClass()) {
            return false;
        }
        ExperimentTask task = (ExperimentTask) obj;
        return taskName.equals(task.getTaskName())
                && modelName.equals(task.getModelName())
                && ontology.equals(task.getOntology())
                && query.equals(task.getQuery())
                && system.equals(task.getSystem());
    }
}
