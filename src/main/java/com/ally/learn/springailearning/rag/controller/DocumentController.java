package com.ally.learn.springailearning.rag.controller;

import com.ally.learn.springailearning.rag.entity.DocumentInfo;
import com.ally.learn.springailearning.rag.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * @author cgl
 * @description 文档管理控制器
 * @date 2025-06-13
 * @Version 1.0
 **/
@RestController
@RequestMapping("/api/rag/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * 上传文档到指定知识库
     */
    @PostMapping("/upload")
    public ResponseEntity<DocumentInfo> uploadDocument(
            @RequestParam("knowledgeBaseId") String knowledgeBaseId,
            @RequestParam("file") MultipartFile file) {
        
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }
        
        DocumentInfo documentInfo = documentService.uploadDocument(knowledgeBaseId, file);
        return ResponseEntity.ok(documentInfo);
    }

    /**
     * 获取指定知识库的所有文档
     */
    @GetMapping("/knowledge-base/{knowledgeBaseId}")
    public ResponseEntity<List<DocumentInfo>> getDocumentsByKnowledgeBase(
            @PathVariable String knowledgeBaseId) {
        List<DocumentInfo> documentInfos = documentService.getDocumentsByKnowledgeBase(knowledgeBaseId);
        return ResponseEntity.ok(documentInfos);
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentInfo> getDocument(@PathVariable String documentId) {
        DocumentInfo documentInfo = documentService.getDocument(documentId);
        return ResponseEntity.ok(documentInfo);
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Map<String, String>> deleteDocument(@PathVariable String documentId) {
        documentService.deleteDocument(documentId);
        return ResponseEntity.ok(Map.of("message", "文档删除成功"));
    }
} 