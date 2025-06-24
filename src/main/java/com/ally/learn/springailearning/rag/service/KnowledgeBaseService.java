package com.ally.learn.springailearning.rag.service;

import com.ally.learn.springailearning.rag.dto.KnowledgeBaseRequest;
import com.ally.learn.springailearning.rag.entity.KnowledgeBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author cgl
 * @description 知识库管理服务
 * 使用知识库ID作为Weaviate className，简化管理
 * @date 2025-06-13
 * @Version 1.0
 **/
@Service
public class KnowledgeBaseService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseService.class);

    private final WeaviateVectorStoreFactory vectorStoreFactory;
    // 简单的内存存储，实际应用中应该使用数据库
    private final Map<String, KnowledgeBase> knowledgeBaseMap = new ConcurrentHashMap<>();

    public KnowledgeBaseService(WeaviateVectorStoreFactory vectorStoreFactory) {
        this.vectorStoreFactory = vectorStoreFactory;
    }

    /**
     * 创建知识库
     */
    public KnowledgeBase createKnowledgeBase(KnowledgeBaseRequest request) {
        String id = UUID.randomUUID().toString();
        KnowledgeBase knowledgeBase = KnowledgeBase.builder()
                .id(id)
                .name(request.getName())
                .description(request.getDescription())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .documentCount(0L)
                .status("ACTIVE")
                .build();

        try {
            // 创建知识库的同时创建对应的向量存储
            vectorStoreFactory.createVectorStore(id);
            
            knowledgeBaseMap.put(id, knowledgeBase);
            logger.info("Created knowledge base: {} with ID: {} (Weaviate className created)", 
                    knowledgeBase.getName(), knowledgeBase.getId());
            return knowledgeBase;
        } catch (Exception e) {
            logger.error("Failed to create knowledge base: {}", request.getName(), e);
            throw new RuntimeException("创建知识库失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有知识库
     */
    public List<KnowledgeBase> getAllKnowledgeBases() {
        return knowledgeBaseMap.values().stream().toList();
    }

    /**
     * 获取指定知识库
     */
    public KnowledgeBase getKnowledgeBase(String id) {
        KnowledgeBase knowledgeBase = knowledgeBaseMap.get(id);
        if (knowledgeBase == null) {
            throw new RuntimeException("知识库不存在: " + id);
        }
        return knowledgeBase;
    }

    /**
     * 更新知识库
     */
    public KnowledgeBase updateKnowledgeBase(String id, KnowledgeBaseRequest request) {
        KnowledgeBase knowledgeBase = getKnowledgeBase(id);

        knowledgeBase.setName(request.getName());
        knowledgeBase.setDescription(request.getDescription());
        knowledgeBase.setUpdateTime(LocalDateTime.now());

        knowledgeBaseMap.put(id, knowledgeBase);
        logger.info("Updated knowledge base: {} with ID: {}", knowledgeBase.getName(), knowledgeBase.getId());
        return knowledgeBase;
    }

    /**
     * 删除知识库
     */
    public void deleteKnowledgeBase(String id) {
        KnowledgeBase knowledgeBase = getKnowledgeBase(id);

        try {
            // 从内存中移除知识库记录
            knowledgeBaseMap.remove(id);
            
            // 删除整个Weaviate class和向量存储实例
            vectorStoreFactory.deleteVectorStore(id);
            
            logger.info("Deleted knowledge base: {} with ID: {} (Weaviate class deleted)", 
                    knowledgeBase.getName(), knowledgeBase.getId());
        } catch (Exception e) {
            logger.error("Failed to delete knowledge base: {}", id, e);
            throw new RuntimeException("删除知识库失败: " + e.getMessage());
        }
    }

    /**
     * 向知识库添加文档
     */
    public void addDocuments(String knowledgeBaseId, List<Document> documents) {
        KnowledgeBase knowledgeBase = getKnowledgeBase(knowledgeBaseId);

        try {
            // 获取对应的向量存储实例
            VectorStore vectorStore = vectorStoreFactory.getVectorStore(knowledgeBaseId);
            
            // 只添加必要的元数据，不需要knowledge_base_id（因为已通过className隔离）
            documents.forEach(doc -> {
                doc.getMetadata().put("knowledge_base_name", knowledgeBase.getName());
                doc.getMetadata().put("created_time", LocalDateTime.now().toString());
            });
            vectorStore.add(documents);

            // 更新文档计数
            knowledgeBase.setDocumentCount(knowledgeBase.getDocumentCount() + documents.size());
            knowledgeBase.setUpdateTime(LocalDateTime.now());
            knowledgeBaseMap.put(knowledgeBaseId, knowledgeBase);

            logger.info("Added {} documents to knowledge base: {} (className: {})", 
                    documents.size(), knowledgeBase.getName(), knowledgeBaseId);
        } catch (Exception e) {
            logger.error("Failed to add documents to knowledge base: {}", knowledgeBaseId, e);
            throw new RuntimeException("添加文档失败: " + e.getMessage());
        }
    }

    /**
     * 搜索知识库
     */
    public List<Document> search(SearchRequest searchRequest, String knowledgeBaseId) {
        try {
            KnowledgeBase knowledgeBase = getKnowledgeBase(knowledgeBaseId);
            
            // 获取对应的向量存储实例（已通过className完全隔离，无需过滤器）
            VectorStore vectorStore = vectorStoreFactory.getVectorStore(knowledgeBaseId);
            
            // 直接在对应的向量存储中搜索，无需过滤器
            SearchRequest vectorSearchRequest =
                    SearchRequest.builder()
                            .query(searchRequest.getQuery())
                            .topK(searchRequest.getTopK())
                            .similarityThreshold(searchRequest.getSimilarityThreshold())
                            .build();
            
            List<Document> results = vectorStore.similaritySearch(vectorSearchRequest);
            logger.info("Search knowledge base: {} (className: {}), found {} documents", 
                    knowledgeBase.getName(), knowledgeBaseId, results.size());
            
            return results;
        } catch (Exception e) {
            logger.error("Failed to search knowledge base: {}", knowledgeBaseId, e);
            throw new RuntimeException("搜索失败: " + e.getMessage());
        }
    }
} 