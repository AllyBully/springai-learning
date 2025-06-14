package com.ally.learn.springailearning.common.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author cgl
 * @description 基于Redis的聊天记忆存储库
 * @date 2025-06-13
 * @Version 1.0
 **/
@Repository
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final Logger logger = LoggerFactory.getLogger(RedisChatMemoryRepository.class);
    
    private static final String CONVERSATION_KEY_PREFIX = "chat:conversation:";
    private static final String CONVERSATION_LIST_KEY = "chat:conversations";
    
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;

    public RedisChatMemoryRepository(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<String> findConversationIds() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> conversationIds = jedis.smembers(CONVERSATION_LIST_KEY);
            return new ArrayList<>(conversationIds);
        } catch (Exception e) {
            logger.error("Error finding conversation IDs", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = CONVERSATION_KEY_PREFIX + conversationId;
            List<String> messageJsonList = jedis.lrange(key, 0, -1);
            
            List<Message> messages = new ArrayList<>();
            for (String messageJson : messageJsonList) {
                try {
                    Message message = deserializeMessage(messageJson);
                    if (message != null) {
                        messages.add(message);
                    }
                } catch (Exception e) {
                    logger.error("Error deserializing message: {}", messageJson, e);
                }
            }
            
            return messages;
        } catch (Exception e) {
            logger.error("Error finding messages for conversation: {}", conversationId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            String key = CONVERSATION_KEY_PREFIX + conversationId;
            
            // 使用Pipeline批量操作
            Pipeline pipeline = jedis.pipelined();
            
            // 添加到对话列表
            pipeline.sadd(CONVERSATION_LIST_KEY, conversationId);
            
            // 保存消息（追加到列表末尾）
            for (Message message : messages) {
                try {
                    String messageJson = serializeMessage(message);
                    pipeline.rpush(key, messageJson);
                } catch (Exception e) {
                    logger.error("Error serializing message: {}", message, e);
                }
            }
            
            // 设置过期时间（7天）
            pipeline.expire(key, 7 * 24 * 60 * 60);
            
            pipeline.sync();
            
            logger.debug("Saved {} messages for conversation: {}", messages.size(), conversationId);
        } catch (Exception e) {
            logger.error("Error saving messages for conversation: {}", conversationId, e);
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = CONVERSATION_KEY_PREFIX + conversationId;
            
            Pipeline pipeline = jedis.pipelined();
            // 删除对话消息
            pipeline.del(key);
            // 从对话列表中移除
            pipeline.srem(CONVERSATION_LIST_KEY, conversationId);
            pipeline.sync();
            
            logger.debug("Deleted conversation: {}", conversationId);
        } catch (Exception e) {
            logger.error("Error deleting conversation: {}", conversationId, e);
        }
    }

    /**
     * 清理过期的对话记录
     */
    public void cleanupExpiredConversations() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> conversationIds = jedis.smembers(CONVERSATION_LIST_KEY);
            
            for (String conversationId : conversationIds) {
                String key = CONVERSATION_KEY_PREFIX + conversationId;
                if (!jedis.exists(key)) {
                    // 如果对话数据已过期，从列表中移除
                    jedis.srem(CONVERSATION_LIST_KEY, conversationId);
                    logger.debug("Cleaned up expired conversation: {}", conversationId);
                }
            }
        } catch (Exception e) {
            logger.error("Error cleaning up expired conversations", e);
        }
    }

    private String serializeMessage(Message message) throws JsonProcessingException {
        // 创建一个简化的消息表示
        MessageData messageData = new MessageData(
            message.getClass().getSimpleName(),
            message.getText(),
            message.getMetadata()
        );
        return objectMapper.writeValueAsString(messageData);
    }

    private Message deserializeMessage(String messageJson) throws JsonProcessingException {
        MessageData messageData = objectMapper.readValue(messageJson, MessageData.class);
        
        // 根据消息类型创建相应的Message实例
        return switch (messageData.type()) {
            case "UserMessage" -> new org.springframework.ai.chat.messages.UserMessage(
                messageData.content()
            );
            case "AssistantMessage" -> new org.springframework.ai.chat.messages.AssistantMessage(
                messageData.content(),
                messageData.metadata()
            );
            case "SystemMessage" -> new org.springframework.ai.chat.messages.SystemMessage(
                messageData.content()
            );
            default -> {
                logger.warn("Unknown message type: {}", messageData.type());
                yield null;
            }
        };
    }

    /**
     * 消息数据传输对象
     */
    private record MessageData(
        String type,
        String content,
        java.util.Map<String, Object> metadata
    ) {}
}
