package com.jose.revaisor.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class ActionItemsRequest {

    @NotBlank(message = "meetingNotes no puede estar vacío")
    private String meetingNotes;

    public String getMeetingNotes() {
        return meetingNotes;
    }

    public void setMeetingNotes(String meetingNotes) {
        this.meetingNotes = meetingNotes;
    }
}