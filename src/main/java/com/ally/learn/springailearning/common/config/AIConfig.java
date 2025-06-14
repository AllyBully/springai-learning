package com.ally.learn.springailearning.common.config;

import com.ally.learn.springailearning.common.advisor.CustomMessageChatMemoryAdvisor;
import com.ally.learn.springailearning.common.advisor.MySqlChatHistoryAdvisor;
import com.ally.learn.springailearning.common.advisor.StreamControlAdvisor;
import com.ally.learn.springailearning.common.tool.DateTools;
import com.ally.learn.springailearning.common.service.StreamControlService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
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
                                     StreamControlService streamControlService,
                                     VectorStore vectorStore) {
        return ChatClient.builder(deepSeekChatModel)
                .defaultAdvisors(
                    // 流控制advisor - 最高优先级，最先执行
                    StreamControlAdvisor.builder(streamControlService)
                        .order(-1)
                        .build(),
                    // 聊天记忆advisor
                    CustomMessageChatMemoryAdvisor.builder(chatMemory).build(),
                    // 历史记录advisor
                    MySqlChatHistoryAdvisor.builder().build(),
                    QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(SearchRequest.builder().similarityThreshold(0.8d).topK(6).build())
                        .build()
                )
                .defaultSystem("""
                        你是一个乐观的小助手
                        """)
                .defaultTools(dateTools)
                .build();
    }
}
