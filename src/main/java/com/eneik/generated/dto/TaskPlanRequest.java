package com.eneik.generated.dto;

import java.util.List;

public class TaskPlanRequest {
    private List<String> specifications;
    private List<String> rootCauseRepairs;
    private List<String> tasks;

    public TaskPlanRequest() {}

    public TaskPlanRequest(List<String> specifications, List<String> rootCauseRepairs, List<String> tasks) {
        this.specifications = specifications;
        this.rootCauseRepairs = rootCauseRepairs;
        this.tasks = tasks;
    }

    public List<String> getSpecifications() {
        return specifications;
    }

    public void setSpecifications(List<String> specifications) {
        this.specifications = specifications;
    }

    public List<String> getRootCauseRepairs() {
        return rootCauseRepairs;
    }

    public void setRootCauseRepairs(List<String> rootCauseRepairs) {
        this.rootCauseRepairs = rootCauseRepairs;
    }

    public List<String> getTasks() {
        return tasks;
    }

    public void setTasks(List<String> tasks) {
        this.tasks = tasks;
    }
}
