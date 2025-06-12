package com.ally.learn.springailearning.advisors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.memory.ChatMemory;

import com.ally.learn.springailearning.config.StreamControlService;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * @author cgl
 * @description 流控制Advisor - 专门负责处理流的停止控制
 * @date 2025-06-10
 * @Version 1.0
 **/
public class StreamControlAdvisor implements StreamAdvisor {
    
    private static final Logger logger = LoggerFactory.getLogger(StreamControlAdvisor.class);
    
    private final StreamControlService streamControlService;
    private final int order;
    private final Scheduler scheduler;

    private StreamControlAdvisor(StreamControlService streamControlService, int order, Scheduler scheduler) {
        this.streamControlService = streamControlService;
        this.order = order;
        this.scheduler = scheduler;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, 
                                                StreamAdvisorChain streamAdvisorChain) {
        // 从context中获取会话ID
        String sessionId = getSessionId(chatClientRequest);
        
        logger.info("Stream control advisor activated for session: {}", sessionId);
        
        return streamAdvisorChain.nextStream(chatClientRequest)
                .publishOn(scheduler)
                .takeUntilOther(streamControlService.getCancelSignal(sessionId))
                .doOnCancel(() -> {
                    logger.info("Stream cancelled for session: {}", sessionId);
                })
                .doFinally(signalType -> {
                    logger.info("Stream finished with signal: {} for session: {}", signalType, sessionId);
                });
    }
    
    private String getSessionId(ChatClientRequest request) {
        // 从context中获取会话ID
        Object sessionId = request.context().get(ChatMemory.CONVERSATION_ID);
        return sessionId != null ? sessionId.toString() : "default";
    }

    @Override
    public String getName() {
        return "StreamControlAdvisor";
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    public static Builder builder(StreamControlService streamControlService) {
        return new Builder(streamControlService);
    }

    public static class Builder {
        private final StreamControlService streamControlService;
        private int order = 50; // 设置较高的优先级，早于其他advisor执行
        private Scheduler scheduler = Schedulers.boundedElastic();

        private Builder(StreamControlService streamControlService) {
            this.streamControlService = streamControlService;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public Builder scheduler(Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public StreamControlAdvisor build() {
            return new StreamControlAdvisor(streamControlService, order, scheduler);
        }
    }
} 