package com.jose.revaisor.backend.dto;

import java.util.List;

public class ActionItemsResponse {

    private List<TaskItem> tasks;

    public ActionItemsResponse() {}

    public ActionItemsResponse(List<TaskItem> tasks) {
        this.tasks = tasks;
    }

    public List<TaskItem> getTasks() { return tasks; }
    public void setTasks(List<TaskItem> tasks) { this.tasks = tasks; }
}