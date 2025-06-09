package com.ally.learn.springailearning.controller;

import com.ally.learn.springailearning.dto.ChatMessage;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

/**
 * @author cgl
 * @description
 * @date 2025-05-29
 * @Version 1.0
 **/
@RestController
public class TestController {

    @Resource(name = "reasonDeepSeekClient")
    public ChatClient reasonChatClient;

    @Resource(name = "deepSeekClient")
    public ChatClient chatClient;

    @PostMapping(value = "/ai/streamChat", produces = TEXT_EVENT_STREAM_VALUE)
    public Flux<AssistantMessage> generateStream(@RequestBody ChatMessage message) {
        Boolean thinkingMode = message.getThinkingMode();
        if (thinkingMode) {
            return reasonChatClient.prompt(new Prompt(message.getPrompt()))
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, message.getChatSessionId()))
                    .stream().chatResponse()
                    .map(chatResponse -> chatResponse.getResult().getOutput());
        }
        return chatClient.prompt(new Prompt(message.getPrompt()))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, message.getChatSessionId()))
                .stream().chatResponse()
                .map(chatResponse -> chatResponse.getResult().getOutput());
    }
}
