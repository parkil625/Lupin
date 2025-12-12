package com.example.demo.config;

import com.example.demo.service.AuctionSseService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key-Value를 단순 문자열로 저장하기 위한 직렬화 설정
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());

        return template;
    }

    // ▼ [추가됨] Redis Pub/Sub 메시지를 수신하는 컨테이너
    @Bean
    @ConditionalOnProperty(name="app.redis.pubsub.enabled", havingValue="true", matchIfMissing = true)
    public RedisMessageListenerContainer redisMessageListener(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapter,
            ChannelTopic topic) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // "auction-update"라는 주제(Topic)로 오는 메시지를 리스너에 연결
        container.addMessageListener(listenerAdapter, topic);
        return container;
    }

    // ▼ [추가됨] 메시지가 오면 처리할 서비스 메서드 지정
    // @Lazy는 혹시 모를 순환 참조(Service <-> Config)를 예방하기 위해 추가하면 좋습니다.
    @Bean
    @ConditionalOnProperty(name="app.redis.pubsub.enabled", havingValue="true", matchIfMissing = true)
    public MessageListenerAdapter listenerAdapter(@Lazy AuctionSseService auctionSseService) {
        // AuctionSseService의 "handleMessage" 메서드를 실행하라고 지정
        return new MessageListenerAdapter(auctionSseService, "handleMessage");
    }

    // ▼ [추가됨] Pub/Sub 채널 이름 설정
    @Bean
    @ConditionalOnProperty(name="app.redis.pubsub.enabled", havingValue="true", matchIfMissing = true)
    public ChannelTopic topic() {
        return new ChannelTopic("auction-update");
    }
}
