package com.ally.learn.springailearning.common.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;

import com.ally.learn.springailearning.common.config.MessageAggregator;

import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author cgl
 * @description
 * @date 2025-06-09
 * @Version 1.0
 **/
public class MySqlChatHistoryAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger logger = LoggerFactory.getLogger(MySqlChatHistoryAdvisor.class);

    private final int order;

    public MySqlChatHistoryAdvisor() {
        this( 0);
    }


    public MySqlChatHistoryAdvisor(int order) {
        this.order = order;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        addHistory(chatClientRequest);

        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);

        addHistory(chatClientResponse);

        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
                                                 StreamAdvisorChain streamAdvisorChain) {
        addHistory(chatClientRequest);
        AtomicReference<Map<String, Object>> context = new AtomicReference<>(new HashMap<>());
        Flux<ChatClientResponse> chatClientResponses = streamAdvisorChain.nextStream(chatClientRequest);
        return new MessageAggregator().aggregate(chatClientResponses.mapNotNull(chatClientResponse -> {
            context.get().putAll(chatClientResponse.context());
            return chatClientResponse.chatResponse();
        }), aggregatedChatResponse -> {
            ChatClientResponse aggregatedChatClientResponse = ChatClientResponse.builder()
                    .chatResponse(aggregatedChatResponse)
                    .context(context.get())
                    .build();
            addHistory(aggregatedChatClientResponse);
        }).map(chatResponse -> ChatClientResponse.builder().chatResponse(chatResponse).context(context.get()).build());
    }

    private void addHistory(ChatClientRequest request) {
        logger.info("add request: {}", request);
    }

    private void addHistory(ChatClientResponse chatClientResponse) {
        logger.info("add response: {}", chatClientResponse.chatResponse());
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public String toString() {
        return MySqlChatHistoryAdvisor.class.getSimpleName();
    }

    public static MySqlChatHistoryAdvisor.Builder builder() {
        return new MySqlChatHistoryAdvisor.Builder();
    }

    public static final class Builder {

        private int order = 0;

        private Builder() {
        }


        public MySqlChatHistoryAdvisor.Builder order(int order) {
            this.order = order;
            return this;
        }

        public MySqlChatHistoryAdvisor build() {
            return new MySqlChatHistoryAdvisor(this.order);
        }

    }
}
