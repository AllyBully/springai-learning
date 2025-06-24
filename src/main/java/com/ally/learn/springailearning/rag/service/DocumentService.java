package com.ally.learn.springailearning.rag.service;

import com.ally.learn.springailearning.rag.entity.DocumentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author cgl
 * @description 文档处理服务
 * @date 2025-06-13
 * @Version 1.0
 **/
@Service
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);
    
    private final KnowledgeBaseService knowledgeBaseService;
    private final TextSplitter textSplitter;
    // 简单的内存存储，实际应用中应该使用数据库
    private final Map<String, DocumentInfo> documentMap = new ConcurrentHashMap<>();
    
    // 文件存储路径
    private final String uploadPath = "uploads/documents/";

    public DocumentService(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.textSplitter = new TokenTextSplitter();
        
        // 创建上传目录
        try {
            Files.createDirectories(Paths.get(uploadPath));
        } catch (IOException e) {
            logger.error("Failed to create upload directory", e);
        }
    }

    /**
     * 上传并处理文档
     */
    public DocumentInfo uploadDocument(String knowledgeBaseId, MultipartFile file) {
        // 验证知识库存在
        knowledgeBaseService.getKnowledgeBase(knowledgeBaseId);
        
        String documentId = UUID.randomUUID().toString();
        String filename = documentId + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadPath, filename);
        
        DocumentInfo documentInfo = DocumentInfo.builder()
                .id(documentId)
                .knowledgeBaseId(knowledgeBaseId)
                .name(file.getOriginalFilename())
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .filePath(filePath.toString())
                .fileSize(file.getSize())
                .status("UPLOADED")
                .uploadTime(LocalDateTime.now())
                .build();

        try {
            // 保存文件
            file.transferTo(filePath.toFile());
            documentMap.put(documentId, documentInfo);
            
            logger.info("Uploaded document: {} to knowledge base: {}", 
                    file.getOriginalFilename(), knowledgeBaseId);
            
            // 异步处理文档
            processDocumentAsync(documentInfo);
            
            return documentInfo;
        } catch (IOException e) {
            logger.error("Failed to upload document: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("文档上传失败: " + e.getMessage());
        }
    }

    /**
     * 异步处理文档
     */
    private void processDocumentAsync(DocumentInfo documentInfo) {
        // 在实际应用中，这里应该使用异步任务队列
        new Thread(() -> {
            try {
                documentInfo.setStatus("PROCESSING");
                documentInfo.setProcessTime(LocalDateTime.now());
                documentMap.put(documentInfo.getId(), documentInfo);
                
                List<Document> springAiDocuments =
                        parseAndSplitDocument(documentInfo);
                
                // 添加到知识库
                knowledgeBaseService.addDocuments(documentInfo.getKnowledgeBaseId(), springAiDocuments);
                
                documentInfo.setStatus("PROCESSED");
                documentInfo.setChunkCount(springAiDocuments.size());
                documentMap.put(documentInfo.getId(), documentInfo);
                
                logger.info("Processed document: {} with {} chunks", 
                        documentInfo.getName(), springAiDocuments.size());
                
            } catch (Exception e) {
                documentInfo.setStatus("FAILED");
                documentInfo.setErrorMessage(e.getMessage());
                documentMap.put(documentInfo.getId(), documentInfo);
                
                logger.error("Failed to process document: {}", documentInfo.getName(), e);
            }
        }).start();
    }

    /**
     * 解析并分割文档
     */
    private List<Document> parseAndSplitDocument(DocumentInfo documentInfo)
            throws IOException {
        
        Path filePath = Paths.get(documentInfo.getFilePath());
        Resource resource = new FileSystemResource(filePath.toFile());

        DocumentReader reader;
        String contentType = documentInfo.getContentType();

        // 根据文件类型选择合适的读取器
        if ("application/pdf".equals(contentType)) {
            reader = new PagePdfDocumentReader(resource);
        } else {
            // 使用Tika处理其他类型的文档
            reader = new TikaDocumentReader(resource);
        }

        List<Document> documents = reader.get();

        // 添加文档元数据
        documents.forEach(doc -> {
            doc.getMetadata().put("document_id", documentInfo.getId());
            doc.getMetadata().put("document_name", documentInfo.getName());
            doc.getMetadata().put("knowledge_base_id", documentInfo.getKnowledgeBaseId());
            doc.getMetadata().put("content_type", documentInfo.getContentType());
            doc.getMetadata().put("upload_time", documentInfo.getUploadTime().toString());
        });
        
        // 文本分割
        return textSplitter.apply(documents);
    }

    /**
     * 获取文档列表
     */
    public List<DocumentInfo> getDocumentsByKnowledgeBase(String knowledgeBaseId) {
        return documentMap.values().stream()
                .filter(doc -> doc.getKnowledgeBaseId().equals(knowledgeBaseId))
                .toList();
    }

    /**
     * 获取文档详情
     */
    public DocumentInfo getDocument(String documentId) {
        DocumentInfo documentInfo = documentMap.get(documentId);
        if (documentInfo == null) {
            throw new RuntimeException("文档不存在: " + documentId);
        }
        return documentInfo;
    }

    /**
     * 删除文档
     */
    public void deleteDocument(String documentId) {
        DocumentInfo documentInfo = getDocument(documentId);
        
        try {
            // 删除文件
            Files.deleteIfExists(Paths.get(documentInfo.getFilePath()));
            documentMap.remove(documentId);
            
            logger.info("Deleted document: {}", documentInfo.getName());
            // 注意：这里不再直接删除向量数据，因为当知识库删除时会统一清理
            // 如果需要单独删除文档的向量数据，需要通过知识库服务来处理
        } catch (IOException e) {
            logger.error("Failed to delete document file: {}", documentInfo.getFilePath(), e);
            throw new RuntimeException("删除文档失败: " + e.getMessage());
        }
    }
} 