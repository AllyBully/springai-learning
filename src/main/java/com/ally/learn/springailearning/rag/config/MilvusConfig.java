package com.ally.learn.springailearning.rag.config;

import io.milvus.client.MilvusServiceClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author cgl
 * @description Milvus向量数据库配置
 * @date 2025-06-13
 * @Version 1.0
 **/
@Configuration
public class MilvusConfig {

    @Bean
    public VectorStore defaultVectorStore(EmbeddingModel embeddingModel, 
                                         MilvusServiceClient milvusServiceClient) {
        return MilvusVectorStore.builder(milvusServiceClient, embeddingModel).build();
    }
} 