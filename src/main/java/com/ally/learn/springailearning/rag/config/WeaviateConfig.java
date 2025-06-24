package com.ally.learn.springailearning.rag.config;

import org.springframework.context.annotation.Configuration;

/**
 * @author cgl
 * @description Weaviate向量数据库配置
 * @date 2025-06-13
 * @Version 1.0
 **/
@Configuration
public class WeaviateConfig {
    // WeaviateClient会通过Spring AI自动配置创建
    // 不再需要固定的VectorStore Bean，使用WeaviateVectorStoreFactory动态创建
} 