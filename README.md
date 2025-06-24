# Spring AI 学习项目

一个基于Spring AI构建的智能对话和RAG（检索增强生成）系统。

## 📖 项目简介

本项目是一个基于 **Spring AI** 的学习项目，集成了 **RAG (Retrieval-Augmented Generation)** 系统，支持智能问答和知识库管理。

### ✨ 主要特性

- 🤖 **智能对话**：集成 DeepSeek 模型，支持流式和非流式对话
- 📚 **RAG 系统**：支持文档上传、向量化存储和语义搜索
- 🗄️ **向量数据库**：使用 Weaviate 向量数据库
- 💾 **聊天记忆**：基于 Redis 的会话记忆功能
- 📄 **多格式文档**：支持 PDF、Word、TXT 等多种文档格式
- 🔄 **流控制**：响应式编程，支持服务端推送事件(SSE)

## 🎯 核心功能

### 1. 知识库管理
- 支持创建多个知识库，每个知识库ID直接作为 Weaviate className
- 动态管理向量存储实例，实现完全的数据隔离
- 文档上传和自动处理（解析、分块、向量化）

### 2. 智能问答
- 基于知识库ID的语义搜索
- 一个知识库对应一个独立的向量存储空间
- RAG 模式下的上下文增强问答

### 3. 聊天功能
- 支持思维模式（DeepSeek Reasoner）
- 会话记忆管理
- 流式响应支持

## 🚀 快速开始

### 环境要求
- Java 17+
- Maven 3.6+
- Redis 6.0+
- Weaviate 1.20+

### 环境变量配置
```bash
# AI 模型配置
export DEEPSEEK_API_KEY=your_deepseek_api_key
export SILICON_FLOW_API_KEY=your_silicon_flow_api_key

# Redis 配置
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=your_redis_password

# Weaviate 配置
export WEAVIATE_API_KEY=your_weaviate_api_key
```

### 启动应用
```bash
mvn spring-boot:run
```

## 📋 API 接口

### 知识库管理 API

#### 创建知识库
```http
POST /api/rag/knowledge-bases
Content-Type: application/json

{
  "name": "技术文档库",
  "description": "存储技术相关文档",
  "dimension": 1536,
  "embeddingModel": "BAAI/bge-m3"
}
```

> 💡 **说明**：知识库创建后，系统会自动使用知识库ID作为Weaviate的className

#### 获取所有知识库
```http
GET /api/rag/knowledge-bases
```

#### 更新知识库
```http
PUT /api/rag/knowledge-bases/{id}
Content-Type: application/json

{
  "name": "更新后的名称",
  "description": "更新后的描述",
  "dimension": 1536
}
```

#### 删除知识库
```http
DELETE /api/rag/knowledge-bases/{id}
```

#### 在指定知识库中搜索
```http
POST /api/rag/knowledge-bases/{id}/search?query=搜索内容&topK=5&similarityThreshold=0.7
```

### 文档管理 API

#### 上传文档
```http
POST /api/rag/documents/upload/{knowledgeBaseId}
Content-Type: multipart/form-data

file: [选择文件]
```

#### 获取知识库文档列表
```http
GET /api/rag/documents/knowledge-base/{knowledgeBaseId}
```

#### 删除文档
```http
DELETE /api/rag/documents/{documentId}
```

### 聊天 API

#### 流式聊天
```http
POST /chat/stream
Content-Type: application/json

{
  "prompt": "你的问题",
  "chatSessionId": "session-123",
  "knowledgeBaseId": "kb-456",
  "thinkingMode": false
}
```

#### 普通聊天
```http
POST /chat
Content-Type: application/json

{
  "prompt": "你的问题",
  "chatSessionId": "session-123",
  "knowledgeBaseId": "kb-456",
  "thinkingMode": false
}
```

## 🔧 配置说明

### Weaviate 配置
```properties
# Weaviate 连接配置
spring.ai.vectorstore.weaviate.api-key=${WEAVIATE_API_KEY}
spring.ai.vectorstore.weaviate.host=localhost:8080

# 应用特定配置
app.rag.default-top-k=5
app.rag.default-threshold=0.7
```

### 核心设计原则

#### 知识库与向量存储的映射
- **一对一映射**：每个知识库ID直接作为Weaviate的className
- **完全隔离**：不同知识库的向量数据完全隔离，互不干扰
- **简化管理**：无需额外配置className，系统自动管理

#### 搜索机制
- **按知识库搜索**：指定知识库ID，系统自动在对应的向量空间中搜索
- **自动路由**：根据知识库ID自动路由到正确的向量存储实例

## 🏗️ 项目架构

```
src/main/java/com/ally/learn/springailearning/
├── chat/           # 聊天模块
│   ├── controller/ # 聊天控制器
│   ├── dto/        # 聊天数据传输对象
│   └── service/    # 聊天服务
├── rag/            # RAG模块
│   ├── controller/ # RAG控制器
│   ├── dto/        # RAG数据传输对象
│   ├── entity/     # RAG实体类
│   ├── service/    # RAG服务（含向量存储工厂）
│   └── config/     # RAG配置
├── common/         # 公共模块
│   ├── advisor/    # 聊天顾问
│   └── service/    # 公共服务
└── config/         # 全局配置
```

## 🔍 使用示例

### 1. 创建知识库并上传文档
```bash
# 1. 创建知识库
RESPONSE=$(curl -X POST http://localhost:8080/api/rag/knowledge-bases \
  -H "Content-Type: application/json" \
  -d '{
    "name": "技术文档",
    "description": "技术相关文档",
    "dimension": 1536
  }')

# 提取知识库ID
KB_ID=$(echo $RESPONSE | jq -r '.id')
echo "知识库ID: $KB_ID (同时作为Weaviate className)"

# 2. 上传文档
curl -X POST http://localhost:8080/api/rag/documents/upload/$KB_ID \
  -F "file=@your_document.pdf"
```

### 2. 在知识库中搜索
```bash
curl -X POST "http://localhost:8080/api/rag/knowledge-bases/$KB_ID/search?query=Spring Boot 配置&topK=3&similarityThreshold=0.8"
```

### 3. RAG 模式聊天
```bash
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d "{
    \"prompt\": \"如何配置 Spring Boot？\",
    \"chatSessionId\": \"session-001\",
    \"knowledgeBaseId\": \"$KB_ID\"
  }"
```

## 💡 设计亮点

### 简化的架构设计
- **无配置复杂性**：不需要手动管理className
- **自动映射**：知识库ID即className，简洁直观
- **完全隔离**：每个知识库拥有独立的向量存储空间

### 高效的缓存机制
- **工厂模式**：WeaviateVectorStoreFactory统一管理向量存储实例
- **智能缓存**：自动缓存已创建的向量存储，提高性能
- **动态清理**：知识库删除时自动清理相关缓存

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

本项目采用 MIT 许可证。
