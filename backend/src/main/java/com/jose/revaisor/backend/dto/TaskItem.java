package com.jose.revaisor.backend.dto;

public class TaskItem {

    private String task;
    private String assignee;
    private String deadline;

    public TaskItem() {}

    public TaskItem(String task, String assignee, String deadline) {
        this.task = task;
        this.assignee = assignee;
        this.deadline = deadline;
    }

    public String getTask() { return task; }
    public void setTask(String task) { this.task = task; }

    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }
}