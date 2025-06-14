package com.ally.learn.springailearning.rag.service;

import com.ally.learn.springailearning.rag.dto.KnowledgeBaseRequest;
import com.ally.learn.springailearning.rag.entity.KnowledgeBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author cgl
 * @description 知识库管理服务
 * @date 2025-06-13
 * @Version 1.0
 **/
@Service
public class KnowledgeBaseService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseService.class);

    private final MilvusVectorStore vectorStore;
    // 简单的内存存储，实际应用中应该使用数据库
    private final Map<String, KnowledgeBase> knowledgeBaseMap = new ConcurrentHashMap<>();

    public KnowledgeBaseService(MilvusVectorStore vectorStore) {
        this.vectorStore = vectorStore;
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
            knowledgeBaseMap.put(id, knowledgeBase);
            logger.info("Created knowledge base: {}", knowledgeBase.getName());
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
        logger.info("Updated knowledge base: {}", knowledgeBase.getName());
        return knowledgeBase;
    }

    /**
     * 删除知识库
     */
    public void deleteKnowledgeBase(String id) {
        KnowledgeBase knowledgeBase = getKnowledgeBase(id);

        try {
            knowledgeBaseMap.remove(id);
            // 创建过滤器，只搜索指定知识库的文档
            Filter.Expression filter = new Filter.Expression(
                    Filter.ExpressionType.EQ,
                    new Filter.Key("knowledge_base_id"),
                    new Filter.Value(id));
            vectorStore.delete(filter);
            logger.info("Deleted knowledge base: {}", knowledgeBase.getName());
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
            // 添加知识库ID到文档元数据
            documents.forEach(doc -> {
                doc.getMetadata().put("knowledge_base_id", knowledgeBaseId);
                doc.getMetadata().put("knowledge_base_name", knowledgeBase.getName());
            });
            vectorStore.add(documents);

            // 更新文档计数
            knowledgeBase.setDocumentCount(knowledgeBase.getDocumentCount() + documents.size());
            knowledgeBase.setUpdateTime(LocalDateTime.now());
            knowledgeBaseMap.put(knowledgeBaseId, knowledgeBase);

            logger.info("Added {} documents to knowledge base: {}", documents.size(), knowledgeBase.getName());
        } catch (Exception e) {
            logger.error("Failed to add documents to knowledge base: {}", knowledgeBaseId, e);
            throw new RuntimeException("添加文档失败: " + e.getMessage());
        }
    }

    /**
     * 搜索知识库
     */
    public List<Document> search(SearchRequest searchRequest, String knowledgeBaseId) {
        KnowledgeBase knowledgeBase = getKnowledgeBase(knowledgeBaseId);

        try {
            // 创建过滤器，只搜索指定知识库的文档
            Filter.Expression filter = new Filter.Expression(
                    Filter.ExpressionType.EQ,
                    new Filter.Key("knowledge_base_id"),
                    new Filter.Value(knowledgeBaseId));

            SearchRequest vectorSearchRequest =
                    SearchRequest.builder()
                            .query(searchRequest.getQuery())
                            .topK(searchRequest.getTopK())
                            .similarityThreshold(searchRequest.getSimilarityThreshold())
                            .filterExpression(filter)
                            .build();

            return vectorStore.similaritySearch(vectorSearchRequest);
        } catch (Exception e) {
            logger.error("Failed to search knowledge base: {}", knowledgeBaseId, e);
            throw new RuntimeException("搜索失败: " + e.getMessage());
        }
    }
} 