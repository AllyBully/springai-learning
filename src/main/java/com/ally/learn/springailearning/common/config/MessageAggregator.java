package com.ally.learn.springailearning.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.deepseek.DeepSeekAssistantMessage;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author cgl
 * @description 自定义消息聚合器，支持DeepSeek的reasoningContent，并处理流取消情况
 * @date 2025-06-10
 * @Version 1.0
 **/
public class MessageAggregator {

	private static final Logger logger = LoggerFactory.getLogger(MessageAggregator.class);

	public Flux<ChatResponse> aggregate(Flux<ChatResponse> fluxChatResponse,
										Consumer<ChatResponse> onAggregationComplete) {

		// Assistant Message
		AtomicReference<StringBuilder> messageTextContentRef = new AtomicReference<>(new StringBuilder());
		AtomicReference<StringBuilder> messageReasoningContentRef = new AtomicReference<>(new StringBuilder());
		AtomicReference<Map<String, Object>> messageMetadataMapRef = new AtomicReference<>();

		// ChatGeneration Metadata
		AtomicReference<ChatGenerationMetadata> generationMetadataRef = new AtomicReference<>(
				ChatGenerationMetadata.NULL);

		// Usage
		AtomicReference<Integer> metadataUsagePromptTokensRef = new AtomicReference<>(0);
		AtomicReference<Integer> metadataUsageGenerationTokensRef = new AtomicReference<>(0);
		AtomicReference<Integer> metadataUsageTotalTokensRef = new AtomicReference<>(0);

		AtomicReference<PromptMetadata> metadataPromptMetadataRef = new AtomicReference<>(PromptMetadata.empty());
		AtomicReference<RateLimit> metadataRateLimitRef = new AtomicReference<>(new EmptyRateLimit());

		AtomicReference<String> metadataIdRef = new AtomicReference<>("");
		AtomicReference<String> metadataModelRef = new AtomicReference<>("");

		return fluxChatResponse.doOnSubscribe(subscription -> {
			logger.debug("Stream subscription started");
			resetReferences(messageTextContentRef, messageReasoningContentRef, messageMetadataMapRef,
					metadataIdRef, metadataModelRef, metadataUsagePromptTokensRef,
					metadataUsageGenerationTokensRef, metadataUsageTotalTokensRef,
					metadataPromptMetadataRef, metadataRateLimitRef);

		}).doOnNext(chatResponse -> {

			if (chatResponse.getResult() != null) {
				if (chatResponse.getResult().getMetadata() != null
						&& chatResponse.getResult().getMetadata() != ChatGenerationMetadata.NULL) {
					generationMetadataRef.set(chatResponse.getResult().getMetadata());
				}
				AssistantMessage output = chatResponse.getResult().getOutput();
				if (output instanceof DeepSeekAssistantMessage && ((DeepSeekAssistantMessage) output).getReasoningContent() != null) {
					messageReasoningContentRef.get().append(((DeepSeekAssistantMessage) output).getReasoningContent());
				}
				if (output.getText() != null) {
					messageTextContentRef.get().append(output.getText());
				}
				messageMetadataMapRef.get().putAll(output.getMetadata());
			}
			if (chatResponse.getMetadata() != null) {
				updateMetadata(chatResponse, metadataUsagePromptTokensRef, metadataUsageGenerationTokensRef,
						metadataUsageTotalTokensRef, metadataPromptMetadataRef, metadataRateLimitRef,
						metadataIdRef, metadataModelRef);
			}
		}).doOnComplete(() -> {
			logger.info("Stream completed normally");
			saveAggregatedResponse(messageTextContentRef, messageReasoningContentRef, messageMetadataMapRef,
					generationMetadataRef, metadataUsagePromptTokensRef, metadataUsageGenerationTokensRef,
					metadataUsageTotalTokensRef, metadataPromptMetadataRef, metadataRateLimitRef,
					metadataIdRef, metadataModelRef, onAggregationComplete);
		}).doOnCancel(() -> {
			logger.info("Stream was cancelled, saving partial response");
			// 流被取消时也保存部分响应
			if (hasPartialContent(messageTextContentRef, messageReasoningContentRef)) {
				saveAggregatedResponse(messageTextContentRef, messageReasoningContentRef, messageMetadataMapRef,
						generationMetadataRef, metadataUsagePromptTokensRef, metadataUsageGenerationTokensRef,
						metadataUsageTotalTokensRef, metadataPromptMetadataRef, metadataRateLimitRef,
						metadataIdRef, metadataModelRef, onAggregationComplete);
			}
		}).doOnError(e -> {
			logger.error("Stream error occurred", e);
			// 错误时也可以选择保存部分响应
			if (hasPartialContent(messageTextContentRef, messageReasoningContentRef)) {
				logger.info("Saving partial response due to error");
				saveAggregatedResponse(messageTextContentRef, messageReasoningContentRef, messageMetadataMapRef,
						generationMetadataRef, metadataUsagePromptTokensRef, metadataUsageGenerationTokensRef,
						metadataUsageTotalTokensRef, metadataPromptMetadataRef, metadataRateLimitRef,
						metadataIdRef, metadataModelRef, onAggregationComplete);
			}
		}).doFinally(signalType -> {
			logger.debug("Stream finished with signal: {}", signalType);
		});
	}

	private void updateMetadata(ChatResponse chatResponse,
								AtomicReference<Integer> metadataUsagePromptTokensRef,
								AtomicReference<Integer> metadataUsageGenerationTokensRef,
								AtomicReference<Integer> metadataUsageTotalTokensRef,
								AtomicReference<PromptMetadata> metadataPromptMetadataRef,
								AtomicReference<RateLimit> metadataRateLimitRef,
								AtomicReference<String> metadataIdRef,
								AtomicReference<String> metadataModelRef) {
		if (chatResponse.getMetadata().getUsage() != null) {
			Usage usage = chatResponse.getMetadata().getUsage();
			metadataUsagePromptTokensRef.set(
					usage.getPromptTokens() > 0 ? usage.getPromptTokens() : metadataUsagePromptTokensRef.get());
			metadataUsageGenerationTokensRef.set(usage.getCompletionTokens() > 0 ? usage.getCompletionTokens()
					: metadataUsageGenerationTokensRef.get());
			metadataUsageTotalTokensRef
				.set(usage.getTotalTokens() > 0 ? usage.getTotalTokens() : metadataUsageTotalTokensRef.get());
		}
		if (chatResponse.getMetadata().getPromptMetadata() != null
				&& chatResponse.getMetadata().getPromptMetadata().iterator().hasNext()) {
			metadataPromptMetadataRef.set(chatResponse.getMetadata().getPromptMetadata());
		}
		if (chatResponse.getMetadata().getRateLimit() != null
				&& !(metadataRateLimitRef.get() instanceof EmptyRateLimit)) {
			metadataRateLimitRef.set(chatResponse.getMetadata().getRateLimit());
		}
		if (StringUtils.hasText(chatResponse.getMetadata().getId())) {
			metadataIdRef.set(chatResponse.getMetadata().getId());
		}
		if (StringUtils.hasText(chatResponse.getMetadata().getModel())) {
			metadataModelRef.set(chatResponse.getMetadata().getModel());
		}
	}

	private boolean hasPartialContent(AtomicReference<StringBuilder> messageTextContentRef,
									  AtomicReference<StringBuilder> messageReasoningContentRef) {
		return messageTextContentRef.get().length() > 0 || messageReasoningContentRef.get().length() > 0;
	}

	private void saveAggregatedResponse(AtomicReference<StringBuilder> messageTextContentRef,
										AtomicReference<StringBuilder> messageReasoningContentRef,
										AtomicReference<Map<String, Object>> messageMetadataMapRef,
										AtomicReference<ChatGenerationMetadata> generationMetadataRef,
										AtomicReference<Integer> metadataUsagePromptTokensRef,
										AtomicReference<Integer> metadataUsageGenerationTokensRef,
										AtomicReference<Integer> metadataUsageTotalTokensRef,
										AtomicReference<PromptMetadata> metadataPromptMetadataRef,
										AtomicReference<RateLimit> metadataRateLimitRef,
										AtomicReference<String> metadataIdRef,
										AtomicReference<String> metadataModelRef,
										Consumer<ChatResponse> onAggregationComplete) {
		
		var usage = new DefaultUsage(metadataUsagePromptTokensRef.get(), metadataUsageGenerationTokensRef.get(),
				metadataUsageTotalTokensRef.get());

		var chatResponseMetadata = ChatResponseMetadata.builder()
			.id(metadataIdRef.get())
			.model(metadataModelRef.get())
			.rateLimit(metadataRateLimitRef.get())
			.usage(usage)
			.promptMetadata(metadataPromptMetadataRef.get())
			.build();
			
		if (messageReasoningContentRef.get().length() > 0) {
			DeepSeekAssistantMessage assistantMessage = new DeepSeekAssistantMessage(
					messageTextContentRef.get().toString(), messageMetadataMapRef.get());
			assistantMessage.setReasoningContent(messageReasoningContentRef.get().toString());
			onAggregationComplete.accept(new ChatResponse(List.of(new Generation(
					assistantMessage,
					generationMetadataRef.get())), chatResponseMetadata));
		} else {
			onAggregationComplete.accept(new ChatResponse(List.of(new Generation(
					new AssistantMessage(messageTextContentRef.get().toString(), messageMetadataMapRef.get()),
					generationMetadataRef.get())), chatResponseMetadata));
		}
		
		// 清理引用
		resetReferences(messageTextContentRef, messageReasoningContentRef, messageMetadataMapRef,
				metadataIdRef, metadataModelRef, metadataUsagePromptTokensRef,
				metadataUsageGenerationTokensRef, metadataUsageTotalTokensRef,
				metadataPromptMetadataRef, metadataRateLimitRef);
	}

	private void resetReferences(AtomicReference<StringBuilder> messageTextContentRef,
								 AtomicReference<StringBuilder> messageReasoningContentRef,
								 AtomicReference<Map<String, Object>> messageMetadataMapRef,
								 AtomicReference<String> metadataIdRef,
								 AtomicReference<String> metadataModelRef,
								 AtomicReference<Integer> metadataUsagePromptTokensRef,
								 AtomicReference<Integer> metadataUsageGenerationTokensRef,
								 AtomicReference<Integer> metadataUsageTotalTokensRef,
								 AtomicReference<PromptMetadata> metadataPromptMetadataRef,
								 AtomicReference<RateLimit> metadataRateLimitRef) {
		messageTextContentRef.set(new StringBuilder());
		messageReasoningContentRef.set(new StringBuilder());
		messageMetadataMapRef.set(new HashMap<>());
		metadataIdRef.set("");
		metadataModelRef.set("");
		metadataUsagePromptTokensRef.set(0);
		metadataUsageGenerationTokensRef.set(0);
		metadataUsageTotalTokensRef.set(0);
		metadataPromptMetadataRef.set(PromptMetadata.empty());
		metadataRateLimitRef.set(new EmptyRateLimit());
	}

	public record DefaultUsage(Integer promptTokens, Integer completionTokens, Integer totalTokens) implements Usage {

		@Override
		public Integer getPromptTokens() {
			return promptTokens();
		}

		@Override
		public Integer getCompletionTokens() {
			return completionTokens();
		}

		@Override
		public Integer getTotalTokens() {
			return totalTokens();
		}

		@Override
		public Map<String, Integer> getNativeUsage() {
			Map<String, Integer> usage = new HashMap<>();
			usage.put("promptTokens", promptTokens());
			usage.put("completionTokens", completionTokens());
			usage.put("totalTokens", totalTokens());
			return usage;
		}
	}
}