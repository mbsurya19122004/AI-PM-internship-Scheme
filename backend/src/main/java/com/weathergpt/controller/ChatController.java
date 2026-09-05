package com.weathergpt.controller;

import com.weathergpt.dto.ApiResponse;
import com.weathergpt.dto.chat.ChatQueryRequest;
import com.weathergpt.dto.chat.ChatResponse;
import com.weathergpt.service.WeatherQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Natural-language weather query endpoint (public read access — weather data is
 * public information; authentication remains required for account features).
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final WeatherQueryService weatherQueryService;

    @PostMapping("/query")
    public ResponseEntity<ApiResponse<ChatResponse>> query(@Valid @RequestBody ChatQueryRequest request) {
        ChatResponse response = weatherQueryService.processQuery(request.getMessage());
        return ResponseEntity.ok(ApiResponse.success("Query processed", response));
    }
}
