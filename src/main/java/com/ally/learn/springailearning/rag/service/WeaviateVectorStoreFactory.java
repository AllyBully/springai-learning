package com.ally.learn.springailearning.rag.service;

import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.schema.model.WeaviateClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.weaviate.WeaviateVectorStore;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author cgl
 * @description Weaviate向量存储工厂
 * 使用知识库ID作为Weaviate className，确保每个知识库拥有独立的向量存储空间
 * @date 2025-06-13
 * @Version 1.0
 **/
@Service
public class WeaviateVectorStoreFactory {

    private static final Logger logger = LoggerFactory.getLogger(WeaviateVectorStoreFactory.class);

    private final EmbeddingModel embeddingModel;
    private final WeaviateClient weaviateClient;
    
    // 缓存已创建的向量存储实例，key为知识库ID
    private final ConcurrentHashMap<String, VectorStore> vectorStoreCache = new ConcurrentHashMap<>();

    public WeaviateVectorStoreFactory(EmbeddingModel embeddingModel, WeaviateClient weaviateClient) {
        this.embeddingModel = embeddingModel;
        this.weaviateClient = weaviateClient;
    }

    /**
     * 创建知识库对应的向量存储实例（同时创建Weaviate class）
     *
     * @param knowledgeBaseId 知识库ID，作为Weaviate className
     */
    public void createVectorStore(String knowledgeBaseId) {
        if (vectorStoreCache.containsKey(knowledgeBaseId)) {
            logger.warn("Vector store already exists for knowledge base: {}", knowledgeBaseId);
            vectorStoreCache.get(knowledgeBaseId);
            return;
        }

        try {
            VectorStore vectorStore = WeaviateVectorStore.builder(weaviateClient, embeddingModel)
                    .objectClass(knowledgeBaseId)
                    .build();
            
            // 缓存实例
            vectorStoreCache.put(knowledgeBaseId, vectorStore);
            
            logger.info("Created Weaviate class and vector store for knowledge base: {}", knowledgeBaseId);
        } catch (Exception e) {
            logger.error("Failed to create vector store for knowledge base: {}", knowledgeBaseId, e);
            throw new RuntimeException("创建向量存储失败: " + e.getMessage());
        }
    }

    /**
     * 根据知识库ID获取向量存储实例
     * @param knowledgeBaseId 知识库ID，同时作为Weaviate className
     * @return VectorStore实例
     * @throws RuntimeException 如果向量存储不存在
     */
    public VectorStore getVectorStore(String knowledgeBaseId) {
        VectorStore vectorStore = vectorStoreCache.get(knowledgeBaseId);
        if (vectorStore == null) {
            throw new RuntimeException("向量存储不存在，知识库ID: " + knowledgeBaseId);
        }
        return vectorStore;
    }

    /**
     * 删除知识库对应的整个Weaviate class和向量存储实例
     * @param knowledgeBaseId 知识库ID，作为Weaviate className
     */
    public void deleteVectorStore(String knowledgeBaseId) {
        try {
            // 从缓存中移除
            vectorStoreCache.remove(knowledgeBaseId);
            
            // 删除Weaviate中的整个class
            Result<Boolean> result = weaviateClient.schema().classDeleter()
                    .withClassName(knowledgeBaseId)
                    .run();
            
            if (result.hasErrors()) {
                logger.warn("Failed to delete Weaviate class: {}, errors: {}", 
                        knowledgeBaseId, result.getError());
            } else {
                logger.info("Successfully deleted Weaviate class: {}", knowledgeBaseId);
            }
        } catch (Exception e) {
            logger.error("Failed to delete vector store for knowledge base: {}", knowledgeBaseId, e);
            throw new RuntimeException("删除向量存储失败: " + e.getMessage());
        }
    }

    /**
     * 检查知识库对应的Weaviate class是否存在
     * @param knowledgeBaseId 知识库ID
     * @return 是否存在
     */
    public boolean classExists(String knowledgeBaseId) {
        try {
            Result<WeaviateClass> result = weaviateClient.schema().classGetter()
                    .withClassName(knowledgeBaseId)
                    .run();
            
            return !result.hasErrors() && result.getResult() != null;
        } catch (Exception e) {
            logger.error("Failed to check if class exists: {}", knowledgeBaseId, e);
            return false;
        }
    }

    /**
     * 检查向量存储是否在缓存中
     * @param knowledgeBaseId 知识库ID
     * @return 是否在缓存中
     */
    public boolean isVectorStoreCached(String knowledgeBaseId) {
        return vectorStoreCache.containsKey(knowledgeBaseId);
    }

    /**
     * 清除指定知识库的缓存（不删除Weaviate class）
     * @param knowledgeBaseId 知识库ID
     */
    public void clearCache(String knowledgeBaseId) {
        vectorStoreCache.remove(knowledgeBaseId);
    }

    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        vectorStoreCache.clear();
    }
} 