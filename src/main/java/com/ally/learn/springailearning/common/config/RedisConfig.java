package com.ally.learn.springailearning.common.config;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

/**
 * @author cgl
 * @description Redis配置类
 * @date 2025-06-13
 * @Version 1.0
 **/
@Configuration
public class RedisConfig {

    @Bean
    public JedisPool jedisPool(RedisProperties redisProperties) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        
        // 连接池配置
        if (redisProperties.getJedis() != null && redisProperties.getJedis().getPool() != null) {
            RedisProperties.Pool pool = redisProperties.getJedis().getPool();
            poolConfig.setMaxTotal(pool.getMaxActive());
            poolConfig.setMaxIdle(pool.getMaxIdle());
            poolConfig.setMinIdle(pool.getMinIdle());
            
            Duration maxWait = pool.getMaxWait();
            if (maxWait != null) {
                poolConfig.setMaxWait(maxWait);
            }
            
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);
        } else {
            // 默认配置
            poolConfig.setMaxTotal(50);
            poolConfig.setMaxIdle(10);
            poolConfig.setMinIdle(5);
            poolConfig.setMaxWait(Duration.ofMillis(10000));
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);
        }

        // 连接参数
        String host = redisProperties.getHost() != null ? redisProperties.getHost() : "localhost";
        int port = redisProperties.getPort() != 0 ? redisProperties.getPort() : 6379;
        int timeout = redisProperties.getTimeout() != null ? (int) redisProperties.getTimeout().toMillis() : 2000;
        String password = redisProperties.getPassword();
        int database = redisProperties.getDatabase();

        if (password != null && !password.isEmpty()) {
            return new JedisPool(poolConfig, host, port, timeout, password, database);
        } else {
            return new JedisPool(poolConfig, host, port, timeout, null, database);
        }
    }
} 