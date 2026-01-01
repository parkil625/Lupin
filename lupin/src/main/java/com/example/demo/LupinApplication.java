package com.example.demo;

import jakarta.annotation.PostConstruct; // [추가]
import java.util.TimeZone; // [추가]
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
public class LupinApplication {

	public static void main(String[] args) {
		SpringApplication.run(LupinApplication.class, args);
	}

    // [추가] 앱 실행 시 전역 시간대를 한국(Seoul)으로 고정 (EC2/Docker 환경 시차 해결)
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        System.out.println(">>> [Application] TimeZone set to Asia/Seoul: " + java.time.LocalDateTime.now());
    }

}
