package com.example.backend.config;

import com.example.backend.event.OrderEventListener;
import com.example.backend.event.RedisMessagePublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    // ==================== Redis Pub/Sub 消息队列 ====================

    /**
     * 消息监听容器 — 异步处理订单、评价、配送事件
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory factory, OrderEventListener orderEventListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);

        // 订单事件频道
        container.addMessageListener(orderEventListener,
                new ChannelTopic(RedisMessagePublisher.CHANNEL_ORDER_EVENTS));
        // 评价事件频道
        container.addMessageListener(orderEventListener,
                new ChannelTopic(RedisMessagePublisher.CHANNEL_EVALUATION_EVENTS));
        // 配送事件频道
        container.addMessageListener(orderEventListener,
                new ChannelTopic(RedisMessagePublisher.CHANNEL_DELIVERY_EVENTS));

        return container;
    }
}
