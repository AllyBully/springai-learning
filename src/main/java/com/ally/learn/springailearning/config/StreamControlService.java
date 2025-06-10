package com.ally.learn.springailearning.config;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.concurrent.ConcurrentHashMap;

@Component
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