package com.xushu.webtable.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xushu.webtable.common.OperationLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类，提供两种 RedisTemplate 配置：
 * 1. String -> OperationLog：用于操作日志存储
 * 2. String -> Object：通用对象存储
 * 
 * 特别注意：配置了 JavaTimeModule 来处理 LocalDateTime 等 Java 8 时间类型
 */
@Configuration
public class RedisConfig {

    /**
     * 创建支持 Java 8 时间类型的 GenericJackson2JsonRedisSerializer
     */
    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // 启用类型信息支持，确保能正确反序列化具体类型
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    @Bean
    public RedisTemplate<String, OperationLog> operationLogRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, OperationLog> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 设置键序列化器为字符串
        template.setKeySerializer(new StringRedisSerializer());
        
        // 设置值序列化器为 JSON，特别处理 LocalDateTime
        template.setValueSerializer(createJsonSerializer());
        
        // 设置哈希键和值的序列化器
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(createJsonSerializer());
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置通用的 RedisTemplate，用于存储任意对象
     * 使用 JSON 序列化，支持各种类型的对象存取
     * 特别处理 LocalDateTime 等 Java 8 时间类型
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 设置键序列化器为字符串
        template.setKeySerializer(new StringRedisSerializer());
        
        // 设置值序列化器为 JSON，特别处理 LocalDateTime
        template.setValueSerializer(createJsonSerializer());
        
        // 设置哈希键和值的序列化器
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(createJsonSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
}
