package com.ally.learn.springailearning.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * @author cgl
 * @description 知识库创建/更新请求
 * @date 2025-06-13
 * @Version 1.0
 **/
@Data
public class KnowledgeBaseRequest {
    
    @NotBlank(message = "知识库名称不能为空")
    private String name;
    
    private String description;
    
    @NotNull(message = "向量维度不能为空")
    private Integer dimension;
    
    private String embeddingModel = "text-embedding-3-small"; // 默认嵌入模型
} 