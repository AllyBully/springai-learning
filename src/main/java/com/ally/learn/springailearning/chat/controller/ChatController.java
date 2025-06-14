package com.ally.learn.springailearning.chat.controller;

import com.ally.learn.springailearning.chat.dto.ChatMessage;
import com.ally.learn.springailearning.chat.service.ChatService;
import com.ally.learn.springailearning.common.service.StreamControlService;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

/**
 * @author cgl
 * @description 聊天控制器
 * @date 2025-06-13
 * @Version 1.0
 **/
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final StreamControlService streamControlService;

    public ChatController(ChatService chatService, StreamControlService streamControlService) {
        this.chatService = chatService;
        this.streamControlService = streamControlService;
    }

    @PostMapping(value = "/stream", produces = TEXT_EVENT_STREAM_VALUE)
    public Flux<AssistantMessage> streamChat(@RequestBody ChatMessage message) {
        return chatService.generateStream(message);
    }

    @PostMapping("/stop")
    public ResponseEntity<Void> stopStream(@RequestParam String chatSessionId) {
        streamControlService.cancelStream(chatSessionId);
        return ResponseEntity.ok().build();
    }
} 