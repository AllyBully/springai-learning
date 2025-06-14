package com.ally.learn.springailearning.rag.controller;

import com.ally.learn.springailearning.rag.dto.KnowledgeBaseRequest;
import com.ally.learn.springailearning.rag.entity.KnowledgeBase;
import com.ally.learn.springailearning.rag.service.KnowledgeBaseService;
import jakarta.validation.Valid;
import org.springframework.ai.document.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author cgl
 * @description 知识库管理控制器
 * @date 2025-06-13
 * @Version 1.0
 **/
@RestController
@RequestMapping("/api/rag/knowledge-bases")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    /**
     * 创建知识库
     */
    @PostMapping
    public ResponseEntity<KnowledgeBase> createKnowledgeBase(@Valid @RequestBody KnowledgeBaseRequest request) {
        KnowledgeBase knowledgeBase = knowledgeBaseService.createKnowledgeBase(request);
        return ResponseEntity.ok(knowledgeBase);
    }

    /**
     * 获取所有知识库
     */
    @GetMapping
    public ResponseEntity<List<KnowledgeBase>> getAllKnowledgeBases() {
        List<KnowledgeBase> knowledgeBases = knowledgeBaseService.getAllKnowledgeBases();
        return ResponseEntity.ok(knowledgeBases);
    }

    /**
     * 获取指定知识库
     */
    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeBase> getKnowledgeBase(@PathVariable String id) {
        KnowledgeBase knowledgeBase = knowledgeBaseService.getKnowledgeBase(id);
        return ResponseEntity.ok(knowledgeBase);
    }

    /**
     * 更新知识库
     */
    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeBase> updateKnowledgeBase(
            @PathVariable String id,
            @Valid @RequestBody KnowledgeBaseRequest request) {
        KnowledgeBase knowledgeBase = knowledgeBaseService.updateKnowledgeBase(id, request);
        return ResponseEntity.ok(knowledgeBase);
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteKnowledgeBase(@PathVariable String id) {
        knowledgeBaseService.deleteKnowledgeBase(id);
        return ResponseEntity.ok(Map.of("message", "知识库删除成功"));
    }
} 