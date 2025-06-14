package com.ally.learn.springailearning.common.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author cgl
 * @description 流控制服务
 * @date 2025-06-13
 * @Version 1.0
 **/
@Service
public class StreamControlService {
    private final ConcurrentHashMap<String, Sinks.Empty<Void>> cancelSignals = new ConcurrentHashMap<>();
    
    public Mono<Void> getCancelSignal(String sessionId) {
        return cancelSignals.computeIfAbsent(sessionId, k -> Sinks.empty()).asMono();
    }
    
    public void cancelStream(String sessionId) {
        Sinks.Empty<Void> sink = cancelSignals.get(sessionId);
        if (sink != null) {
            sink.emitEmpty(Sinks.EmitFailureHandler.FAIL_FAST);
            cancelSignals.remove(sessionId);
        }
    }
} 