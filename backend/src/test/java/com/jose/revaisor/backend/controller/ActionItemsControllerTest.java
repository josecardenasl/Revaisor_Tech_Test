package com.jose.revaisor.backend.controller;

import com.jose.revaisor.backend.dto.ActionItemsResponse;
import com.jose.revaisor.backend.dto.TaskItem;
import com.jose.revaisor.backend.service.GroqService;
import com.jose.revaisor.backend.exception.AiServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActionItemsController.class)
class ActionItemsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GroqService groqService;

    @Test
    void returnsTasksProvidedByService() throws Exception {
        ActionItemsResponse fakeResponse = new ActionItemsResponse(List.of(
                new TaskItem("Enviar propuesta", "Juan", "el viernes"),
                new TaskItem("Revisar presupuesto", null, null)
        ));
        when(groqService.extractActionItems(anyString())).thenReturn(fakeResponse);

        mockMvc.perform(post("/api/action-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"meetingNotes\": \"notas de prueba\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks.length()").value(2))
                .andExpect(jsonPath("$.tasks[0].assignee").value("Juan"))
                .andExpect(jsonPath("$.tasks[1].deadline").isEmpty());
    }

    @Test
    void rejectsBlankMeetingNotesWithoutCallingTheAi() throws Exception {
        mockMvc.perform(post("/api/action-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"meetingNotes\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("meetingNotes no puede estar vacío"));

        verify(groqService, never()).extractActionItems(anyString());
    }

    @Test
    void returnsBadGatewayWhenAiServiceFails() throws Exception {
        when(groqService.extractActionItems(anyString()))
                .thenThrow(new AiServiceException("fallo simulado", new RuntimeException()));

        mockMvc.perform(post("/api/action-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"meetingNotes\": \"notas de prueba\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("fallo simulado"));
    }
}
