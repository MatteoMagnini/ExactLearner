package org.exactlearner.engine;

import org.experiments.workload.WorkloadManager;

import java.util.ArrayList;
import java.util.List;

public class DummyWorkloadManager implements WorkloadManager {
    List<String> queries = new ArrayList<>();

    @Override
    public boolean runWorkload(String message) {
        queries.add(message);
        return true;
    }

    public List<String> getQueries() {
        return queries;
    }
}
