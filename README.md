# Spring AI 学习项目

基于 Spring AI + DeepSeek 大模型的智能聊天应用，实现了完整的对话管理、记忆存储和流式响应控制。

## 🌟 项目特色

- 🚀 **集成 DeepSeek 大模型**：支持 `deepseek-chat` 和 `deepseek-reasoner` 两种模式
- 💭 **思考模式支持**：支持 DeepSeek 思考文本记录，展示AI推理过程
- ⚡ **流式响应**：实时获取AI回复，支持中断响应流
- 🧠 **智能记忆管理**：基于Redis的会话记忆存储，支持对话上下文
- 📝 **会话历史**：完整的对话历史记录管理
- 🎛️ **流控制**：支持实时停止AI响应流
- 🔧 **工具集成**：内置日期工具，可扩展更多功能

## 🏗️ 技术架构

### 核心技术栈
- **Spring Boot 3.5.0** - 应用框架
- **Spring AI 1.0.0** - AI集成框架
- **DeepSeek API** - 大语言模型服务
- **Redis** - 会话记忆存储
- **WebFlux** - 响应式编程支持
- **Maven** - 项目管理

### 架构组件

```
src/main/java/com/ally/learn/springailearning/
├── controller/              # REST API控制器
│   ├── ChatController.java           # 聊天接口
│   └── ChatMemoryController.java     # 记忆管理接口
├── config/                  # 配置类
│   ├── AIConfig.java                 # AI客户端配置
│   ├── ChatMemoryConfig.java         # 记忆配置
│   ├── RedisConfig.java              # Redis配置
│   ├── StreamControlService.java     # 流控制服务
│   └── MessageAggregator.java        # 消息聚合器
├── advisors/                # 自定义顾问
│   ├── CustomMessageChatMemoryAdvisor.java  # 聊天记忆顾问
│   ├── MySqlChatHistoryAdvisor.java         # 历史记录顾问
│   └── StreamControlAdvisor.java            # 流控制顾问
├── repository/              # 数据访问层
│   └── RedisChatMemoryRepository.java       # Redis记忆仓库
├── dto/                     # 数据传输对象
│   └── ChatMessage.java               # 聊天消息DTO
└── tool/                    # 工具类
    └── DateTools.java                # 日期工具
```

## 🚀 快速开始

### 环境要求
- Java 17+
- Maven 3.6+
- Redis 服务器

### 1. 克隆项目
```bash
git clone <repository-url>
cd springai-learning
```

### 2. 配置环境变量
```bash
# DeepSeek API配置
export DEEPSEEK_API_KEY=your-deepseek-api-key

# Redis配置（可选，默认本地连接）
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=
export REDIS_DATABASE=0
```

### 3. 启动应用
```bash
./mvnw spring-boot:run
```

应用启动后访问：http://localhost:8080

## 📚 API文档

### 聊天接口

#### 流式聊天
```http
POST /ai/streamChat
Content-Type: application/json

{
  "prompt": "你好，请介绍一下自己",
  "chatSessionId": "session_123",
  "thinkingMode": false
}
```

**参数说明：**
- `prompt`: 用户输入的问题
- `chatSessionId`: 会话ID，用于维持对话上下文
- `thinkingMode`: 是否启用思考模式（使用deepseek-reasoner）

#### 停止响应流
```http
GET /ai/stopStream?chatSessionId=session_123
```

### 记忆管理接口

#### 获取所有对话
```http
GET /api/chat/memory/conversations
```

#### 获取对话消息
```http
GET /api/chat/memory/conversations/{conversationId}
```

#### 删除对话
```http
DELETE /api/chat/memory/conversations/{conversationId}
```

#### 清理过期对话
```http
POST /api/chat/memory/cleanup
```

#### 获取系统状态
```http
GET /api/chat/memory/status
```

## ⚙️ 配置说明

### application.properties
```properties
# 应用基础配置
spring.application.name=springai-learning

# DeepSeek配置
spring.ai.deepseek.api-key=${DEEPSEEK_API_KEY}
spring.ai.deepseek.chat.options.model=deepseek-chat
spring.ai.deepseek.chat.options.temperature=0.8

# Redis配置
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD:}
spring.data.redis.database=${REDIS_DATABASE:0}
spring.data.redis.timeout=2000ms

# Jedis连接池配置
spring.data.redis.jedis.pool.max-active=50
spring.data.redis.jedis.pool.max-idle=10
spring.data.redis.jedis.pool.min-idle=5
spring.data.redis.jedis.pool.max-wait=10000ms
```

## 🔧 核心功能

### 1. 双模型支持
- **deepseek-chat**: 标准对话模式，快速响应
- **deepseek-reasoner**: 思考模式，展示推理过程

### 2. 智能记忆系统
- 基于Redis存储会话上下文
- 自动过期清理（7天）
- 支持会话隔离

### 3. 流控制机制
- 实时流式响应
- 支持中途停止
- 优雅的流管理

### 4. 顾问系统
- **StreamControlAdvisor**: 流控制处理
- **CustomMessageChatMemoryAdvisor**: 记忆管理
- **MySqlChatHistoryAdvisor**: 历史记录

## 🛠️ 开发指南

### 添加新工具
```java
@Component
public class MyTool {
    @Tool("描述工具功能")
    public String myFunction(String input) {
        // 工具实现
        return "结果";
    }
}
```

### 自定义顾问
```java
@Component
public class CustomAdvisor implements RequestResponseAdvisor {
    // 实现顾问逻辑
}
```

## 📋 待办事项

- [ ] 会话记忆采用Redis存储 ✅ （已完成）
- [ ] 会话历史记录采用MySQL存储
- [ ] 支持更多AI模型
- [ ] 添加用户认证系统
- [ ] 实现聊天界面
- [ ] 添加文件上传支持

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支
3. 提交更改
4. 发起 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 📞 联系方式

如有问题或建议，请提交 Issue 或联系维护者。
