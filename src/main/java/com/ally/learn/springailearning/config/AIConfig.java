package com.ally.learn.springailearning.config;

import com.ally.learn.springailearning.tool.DateTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @author cgl
 * @description
 * @date 2025-05-30
 * @Version 1.0
 **/
@Configuration
public class AIConfig {

    @Bean("deepSeekClient")
    public ChatClient deepSeekClient(DateTools dateTools, 
                                   DeepSeekChatModel deepSeekChatModel, 
                                   ChatMemory chatMemory,
                                   StreamControlService streamControlService) {
        return ChatClient.builder(deepSeekChatModel)
                .defaultAdvisors(
                    // 流控制advisor - 最高优先级，最先执行
                    StreamControlAdvisor.builder(streamControlService)
                        .order(50)
                        .build(),
                    // 聊天记忆advisor
                    CustomMessageChatMemoryAdvisor.builder(chatMemory).build(),
                    // 历史记录advisor
                    MySqlChatHistoryAdvisor.builder().build()
                )
                .defaultSystem("""
                        你是一个乐观的小助手
                        """)
                .defaultTools(dateTools)
                .build();
    }

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(20).build();
    }
}
