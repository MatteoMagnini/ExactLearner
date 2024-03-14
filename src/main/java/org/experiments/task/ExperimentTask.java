package org.experiments.task;

import static org.experiments.utility.SHA256Hash.sha256;

public class ExperimentTask implements Task {

    private String taskName;
    private String modelName;
    private String ontology;
    private String query;

    private Runnable workload;

    public ExperimentTask(String taskName, String modelName, String ontology, String query, Runnable workload) {
        this.taskName = taskName;
        this.modelName = modelName;
        this.ontology = ontology;
        this.query = query;
        this.workload = workload;

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
        return SHA256Hash(taskName + modelName + ontology + query);
    }

    @Override
    public void run() {
        workload.run();
    }

    public int hashCode() {
        return taskName.hashCode() + modelName.hashCode() + ontology.hashCode() + query.hashCode();
    }

    public String toString() {
        return "Task:\n\tName: " + taskName + "\n\tModel: " + modelName + "\n\tOntology: " + ontology + "\n\tQuery: " + query;
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
                && query.equals(task.getQuery());
    }
}
