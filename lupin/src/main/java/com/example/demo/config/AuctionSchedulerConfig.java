package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class AuctionSchedulerConfig {

    @Bean(name = "auctionScheduler")
    public TaskScheduler auctionScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        // 스레드 풀 크기 설정 (경매가 많으면 숫자를 늘려야 할 수도 있음)
        scheduler.setPoolSize(5);

        // 로그 찍힐 때 'Auction-Scheduler-1' 처럼 나와서 디버깅하기 편해집니다
        scheduler.setThreadNamePrefix("Auction-Scheduler-");

        scheduler.initialize();
        return scheduler;
    }
}
