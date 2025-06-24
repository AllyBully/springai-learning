package com.ally.learn.springailearning.rag.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author cgl
 * @description 文档信息实体
 * @date 2025-06-13
 * @Version 1.0
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentInfo {
    
    private String id;
    private String knowledgeBaseId;
    private String name;
    private String originalFilename;
    private String contentType;
    private String filePath;
    private Long fileSize;
    private String status; // UPLOADED, PROCESSING, PROCESSED, FAILED
    private LocalDateTime uploadTime;
    private LocalDateTime processTime;
    private Integer chunkCount; // 分块数量
    private String errorMessage; // 错误信息
} 