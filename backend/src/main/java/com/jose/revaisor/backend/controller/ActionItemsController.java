package com.jose.revaisor.backend.controller;

import com.jose.revaisor.backend.dto.ActionItemsRequest;
import com.jose.revaisor.backend.dto.ActionItemsResponse;
import com.jose.revaisor.backend.service.GroqService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/action-items")
@CrossOrigin(origins = "http://localhost:3000")
public class ActionItemsController {

    private final GroqService groqService;

    public ActionItemsController(GroqService groqService) {
        this.groqService = groqService;
    }

    @PostMapping
    public ActionItemsResponse extractActionItems(@Valid @RequestBody ActionItemsRequest request) {
        return groqService.extractActionItems(request.getMeetingNotes());
    }
}