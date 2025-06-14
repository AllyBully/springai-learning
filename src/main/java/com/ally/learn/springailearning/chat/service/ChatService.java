package com.ally.learn.springailearning.chat.service;

import com.ally.learn.springailearning.chat.dto.ChatMessage;
import com.ally.learn.springailearning.rag.service.KnowledgeBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author cgl
 * @description 聊天服务，支持RAG
 * @date 2025-06-13
 * @Version 1.0
 **/
@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient chatClient;
    private final KnowledgeBaseService knowledgeBaseService;

    // RAG提示模板
    private static final String RAG_PROMPT_TEMPLATE = """
            请基于以下提供的上下文信息来回答用户的问题。如果上下文信息中没有相关内容，请明确说明无法基于提供的信息回答。
            
            上下文信息：
            {context}
            
            用户问题：{question}
            
            请提供准确、有用的回答：
            """;

    public ChatService(ChatClient chatClient, KnowledgeBaseService knowledgeBaseService) {
        this.chatClient = chatClient;
        this.knowledgeBaseService = knowledgeBaseService;
    }

    /**
     * 生成流式聊天回复
     */
    public Flux<AssistantMessage> generateStream(ChatMessage message) {
        Boolean thinkingMode = message.getThinkingMode();
        String finalPrompt = buildPromptWithRAG(message);

        return chatClient.prompt(new Prompt(message.getPrompt()))
                .options(DeepSeekChatOptions.builder()
                        .model(thinkingMode ? DeepSeekApi.ChatModel.DEEPSEEK_REASONER.value : DeepSeekApi.ChatModel.DEEPSEEK_CHAT.value)
                        .build())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, message.getChatSessionId()))
                .advisors(a -> a.param(QuestionAnswerAdvisor.FILTER_EXPRESSION, "knowledge_base_id=" + message.getKnowledgeBaseId()))
                .stream().chatResponse()
                .map(chatResponse -> chatResponse.getResult().getOutput());
    }

    /**
     * 构建包含RAG上下文的提示
     */
    private String buildPromptWithRAG(ChatMessage message) {
        String originalPrompt = message.getPrompt();
        
        // 如果没有指定知识库，直接返回原始提示
        if (!StringUtils.hasText(message.getKnowledgeBaseId())) {
            return originalPrompt;
        }

        try {
            // 执行向量搜索
            SearchRequest searchRequest = SearchRequest.builder().query(originalPrompt).topK(5).similarityThreshold(0.7).build();
            List<Document> searchResults = knowledgeBaseService.search(searchRequest, message.getKnowledgeBaseId());
            
            if (searchResults.isEmpty()) {
                logger.info("No relevant documents found for query: {}", originalPrompt);
                return originalPrompt + "\n\n注：未在知识库中找到相关信息，以下回答基于模型的通用知识。";
            }

            // 构建上下文
            String context = searchResults.stream()
                    .map(doc -> {
                        String content = doc.getFormattedContent();
                        String source = doc.getMetadata().getOrDefault("document_name", "未知来源").toString();
                        return String.format("来源：%s\n内容：%s", source, content);
                    })
                    .collect(Collectors.joining("\n\n---\n\n"));

            // 使用RAG模板构建最终提示
            PromptTemplate promptTemplate = new PromptTemplate(RAG_PROMPT_TEMPLATE);
            Map<String, Object> promptVariables = Map.of(
                    "context", context,
                    "question", originalPrompt
            );

            String ragPrompt = promptTemplate.render(promptVariables);
            
            logger.info("Built RAG prompt with {} context documents for knowledge base: {}", 
                    searchResults.size(), message.getKnowledgeBaseId());
            
            return ragPrompt;

        } catch (Exception e) {
            logger.error("Failed to build RAG context for knowledge base: {}", message.getKnowledgeBaseId(), e);
            // 如果RAG处理失败，回退到原始提示
            return originalPrompt + "\n\n注：知识库搜索失败，以下回答基于模型的通用知识。";
        }
    }

    /**
     * 非流式聊天（用于简单对话）
     */
    public String chat(ChatMessage message) {
        Boolean thinkingMode = message.getThinkingMode();
        String finalPrompt = buildPromptWithRAG(message);
        
        return chatClient.prompt(new Prompt(finalPrompt))
                .options(DeepSeekChatOptions.builder()
                        .model(thinkingMode ? DeepSeekApi.ChatModel.DEEPSEEK_REASONER.value : DeepSeekApi.ChatModel.DEEPSEEK_CHAT.value)
                        .build())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, message.getChatSessionId()))
                .call()
                .content();
    }
} 