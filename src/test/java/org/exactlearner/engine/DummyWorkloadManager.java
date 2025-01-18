package org.exactlearner.engine;

import org.experiments.workload.WorkloadManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class DummyWorkloadManager implements WorkloadManager {
    List<String> queries = new ArrayList<>();
    Function<String, Boolean> response;

    public DummyWorkloadManager() {
        response = x -> true;
    }

    public DummyWorkloadManager(Function<String, Boolean> response) {
        this.response = response;
    }

    @Override
    public boolean runWorkload(String message) {
        queries.add(message);
        return response.apply(message);
    }

    public List<String> getQueries() {
        return queries;
    }
}
