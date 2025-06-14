package com.ally.learn.springailearning.rag.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author cgl
 * @description 知识库实体
 * @date 2025-06-13
 * @Version 1.0
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBase {
    
    private String id;
    private String name;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long documentCount; // 文档数量
    private String status; // ACTIVE, INACTIVE, PROCESSING
} 