package com.ally.learn.springailearning.controller;

import com.ally.learn.springailearning.repository.RedisChatMemoryRepository;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author cgl
 * @description 聊天记忆管理控制器
 * @date 2025-06-13
 * @Version 1.0
 **/
@RestController
@RequestMapping("/api/chat/memory")
public class ChatMemoryController {

    private final ChatMemory chatMemory;
    private final RedisChatMemoryRepository redisChatMemoryRepository;

    public ChatMemoryController(ChatMemory chatMemory, RedisChatMemoryRepository redisChatMemoryRepository) {
        this.chatMemory = chatMemory;
        this.redisChatMemoryRepository = redisChatMemoryRepository;
    }

    /**
     * 获取所有对话ID
     */
    @GetMapping("/conversations")
    public ResponseEntity<List<String>> getAllConversations() {
        List<String> conversationIds = redisChatMemoryRepository.findConversationIds();
        return ResponseEntity.ok(conversationIds);
    }

    /**
     * 获取指定对话的消息历史
     */
    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<List<Message>> getConversationMessages(@PathVariable String conversationId) {
        List<Message> messages = chatMemory.get(conversationId);
        return ResponseEntity.ok(messages);
    }

    /**
     * 删除指定对话
     */
    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Map<String, String>> deleteConversation(@PathVariable String conversationId) {
        redisChatMemoryRepository.deleteByConversationId(conversationId);
        return ResponseEntity.ok(Map.of("message", "Conversation deleted successfully"));
    }

    /**
     * 清理过期的对话记录
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, String>> cleanupExpiredConversations() {
        redisChatMemoryRepository.cleanupExpiredConversations();
        return ResponseEntity.ok(Map.of("message", "Cleanup completed successfully"));
    }

    /**
     * 获取Redis连接状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        try {
            List<String> conversationIds = redisChatMemoryRepository.findConversationIds();
            return ResponseEntity.ok(Map.of(
                "status", "connected",
                "conversationCount", conversationIds.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
} 