package com.ally.learn.springailearning.common.config;

import com.ally.learn.springailearning.common.repository.RedisChatMemoryRepository;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author cgl
 * @description 聊天记忆配置类
 * @date 2025-06-13
 * @Version 1.0
 **/
@Configuration
public class ChatMemoryConfig {

    /**
     * 创建基于Redis的聊天记忆Bean
     */
    @Bean
    @Primary
    public ChatMemory redisChatMemory(RedisChatMemoryRepository redisChatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .maxMessages(20) // 最多保留20条消息
                .chatMemoryRepository(redisChatMemoryRepository)
                .build();
    }

    /**
     * 备用的内存版本ChatMemory（用于测试或Redis不可用时）
     */
    @Bean("inMemoryChatMemory")
    public ChatMemory inMemoryChatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }
} 