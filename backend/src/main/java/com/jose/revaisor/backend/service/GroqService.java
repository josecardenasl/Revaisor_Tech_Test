package com.jose.revaisor.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jose.revaisor.backend.dto.ActionItemsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.http.HttpClient;
import java.time.Duration;
import com.jose.revaisor.backend.exception.AiServiceException;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Service
public class GroqService {

    private static final String MODEL = "openai/gpt-oss-20b";

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GroqService(@Value("${groq.api.key}") String apiKey) {
    HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(Duration.ofSeconds(30));

    this.restClient = RestClient.builder()
            .baseUrl("https://api.groq.com/openai/v1")
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .requestFactory(requestFactory)
            .build();
}

    public ActionItemsResponse extractActionItems(String meetingNotes) {
        Map<String, Object> requestBody = Map.of(
                "model", MODEL,
                "reasoning_effort", "low",
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "Sos un asistente que extrae tareas accionables de notas de reunión. " +
                                "Para cada tarea identificá la descripción, el responsable (si se menciona) " +
                                "y la fecha límite (si se menciona). Si algo no se menciona, devolvé null."),
                        Map.of("role", "user", "content", meetingNotes)
                ),
                "response_format", buildResponseFormat()
        );

        try {
        Map<String, Object> apiResponse = restClient.post()
                .uri("/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        String content = extractContent(apiResponse);
        return objectMapper.readValue(content, ActionItemsResponse.class);
        } catch (RestClientException e) {
        throw new AiServiceException(
            "No se pudo contactar al servicio de IA. Intentá de nuevo en unos momentos.", e);
        } catch (Exception e) {
        throw new AiServiceException("La respuesta de la IA no tuvo el formato esperado.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> apiResponse) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) apiResponse.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    private Map<String, Object> buildResponseFormat() {
        Map<String, Object> taskSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "task", Map.of("type", "string"),
                        "assignee", Map.of("type", List.of("string", "null")),
                        "deadline", Map.of("type", List.of("string", "null"))
                ),
                "required", List.of("task", "assignee", "deadline"),
                "additionalProperties", false
        );

        Map<String, Object> fullSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "tasks", Map.of("type", "array", "items", taskSchema)
                ),
                "required", List.of("tasks"),
                "additionalProperties", false
        );

        return Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                        "name", "action_items",
                        "strict", true,
                        "schema", fullSchema
                )
        );
    }
}