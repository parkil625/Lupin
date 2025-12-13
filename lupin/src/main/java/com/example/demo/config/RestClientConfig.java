package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * RestClient Bean 설정 (Spring 6.1+ Modern API)
 * - RestTemplate 대체: Fluent API, 더 나은 타입 안전성
 * - OAuth 서비스 등에서 외부 API 호출 시 사용
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
