package com.example.demo.config;

import com.example.demo.service.AuctionSseService;
import com.example.demo.service.NotificationSseService;
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
import com.example.demo.service.ChatWebSocketService;

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
            MessageListenerAdapter auctionListenerAdapter,
            MessageListenerAdapter notificationListenerAdapter,
            MessageListenerAdapter chatListenerAdapter,
            MessageListenerAdapter notificationDeleteListenerAdapter, // [추가]
            ChannelTopic auctionTopic,
            ChannelTopic notificationTopic,
            ChannelTopic chatTopic,
            ChannelTopic notificationDeleteTopic // [추가]
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // "auction-update" 채널 리스너
        container.addMessageListener(auctionListenerAdapter, auctionTopic);
        // "notification-update" 채널 리스너
        container.addMessageListener(notificationListenerAdapter, notificationTopic);
        // "notification-delete" 채널 리스너 [추가]
        container.addMessageListener(notificationDeleteListenerAdapter, notificationDeleteTopic);

        container.addMessageListener(chatListenerAdapter, chatTopic);
        return container;
    }

    // ▼ 경매 메시지 리스너 어댑터
    @Bean
    @ConditionalOnProperty(name="app.redis.pubsub.enabled", havingValue="true", matchIfMissing = true)
    public MessageListenerAdapter auctionListenerAdapter(@Lazy AuctionSseService auctionSseService) {
        return new MessageListenerAdapter(auctionSseService, "handleMessage");
    }

    // ▼ 알림 메시지 리스너 어댑터
    @Bean
    @ConditionalOnProperty(name="app.redis.pubsub.enabled", havingValue="true", matchIfMissing = true)
    public MessageListenerAdapter notificationListenerAdapter(@Lazy NotificationSseService notificationSseService) {
        return new MessageListenerAdapter(notificationSseService, "handleMessage");
    }

    // ▼ [추가] 알림 삭제 메시지 리스너 어댑터
    @Bean
    @ConditionalOnProperty(name="app.redis.pubsub.enabled", havingValue="true", matchIfMissing = true)
    public MessageListenerAdapter notificationDeleteListenerAdapter(@Lazy NotificationSseService notificationSseService) {
        return new MessageListenerAdapter(notificationSseService, "handleDeleteMessage");
    }

    @Bean
    @ConditionalOnProperty(name="app.redis.pubsub.enabled", havingValue="true", matchIfMissing = true)
    public MessageListenerAdapter chatListenerAdapter(@Lazy ChatWebSocketService chatWebSocketService) {
        // chatWebSocketService "handleMessage"라는 메서드를 가지고 있어야 함
        return new MessageListenerAdapter(chatWebSocketService, "handleMessage");
    }

    // ▼ 경매 Pub/Sub 채널
    @Bean
    @ConditionalOnProperty(name="app.redis.pubsub.enabled", havingValue="true", matchIfMissing = true)
    public ChannelTopic auctionTopic() {
        return new ChannelTopic("auction-update");
    }

    // ▼ 알림 Pub/Sub 채널
    @Bean
    @ConditionalOnProperty(name="app.redis.pubsub.enabled", havingValue="true", matchIfMissing = true)
    public ChannelTopic notificationTopic() {
        return new ChannelTopic("notification-update");
    }

    // ▼ [추가] 알림 삭제 Pub/Sub 채널
    @Bean
    @ConditionalOnProperty(name="app.redis.pubsub.enabled", havingValue="true", matchIfMissing = true)
    public ChannelTopic notificationDeleteTopic() {
        return new ChannelTopic("notification-delete");
    }

    // ▼ chat Pub/Sub 채널
    @Bean
    @ConditionalOnProperty(name="app.redis.pubsub.enabled", havingValue="true", matchIfMissing = true)
    public ChannelTopic chatTopic() {
        return new ChannelTopic("chat-update"); 
    }
}
