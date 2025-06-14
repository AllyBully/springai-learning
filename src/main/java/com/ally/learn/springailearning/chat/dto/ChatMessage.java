package com.ally.learn.springailearning.chat.dto;

import lombok.Data;

/**
 * @author cgl
 * @description 聊天消息DTO
 * @date 2025-06-09
 * @Version 1.0
 **/
@Data
public class ChatMessage {
    private String prompt;
    private String chatSessionId;
    private Boolean thinkingMode;
    private Boolean searchMode;
    private String knowledgeBaseId; // 新增：知识库ID
} 