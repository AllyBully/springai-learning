package com.ally.learn.springailearning.dto;

import lombok.Data;

/**
 * @author cgl
 * @description
 * @date 2025-06-09
 * @Version 1.0
 **/
@Data
public class ChatMessage {
    private String prompt;
    private String chatSessionId;
    private Boolean thinkingMode;
    private Boolean searchMode;
}
