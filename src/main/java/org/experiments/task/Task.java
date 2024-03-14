package org.experiments.task;

public interface Task extends Runnable{

    /**
     * This method returns the name of the task.
     * @return the name of the task.
     */
    String getTaskName();

    /**
     * This method returns the name of the model.
     * E.g., Mixtral, Ollama, etc.
     * @return the name of the model.
     */
    String getModelName();

    /**
     * This method returns the name of the ontology.
     * E.g., animals, generations, etc.
     * @return the name of the ontology.
     */
    String getOntology();

    /**
     * This method returns the query of the task.
     * @return the query of the task.
     */
    String getQuery();

    /**
     * This method returns the SHA-256 hash of the input string.
     * @param input the string to be hashed.
     * @return the SHA-256 hash of the input string.
     */
    String SHA256Hash(String input);

    /**
     * This method returns the file name of the task.
     * @return the file name of the task.
     */
    String getFileName();

    /**
     * This method run the task.
     */
    void run();

}


