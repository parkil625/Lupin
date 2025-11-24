package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.example.demo.config.TestRedisConfig;

@SpringBootTest
@Import(TestRedisConfig.class)
@ActiveProfiles("test")
class LupinApplicationTests {

	@Test
	void contextLoads() {
	}

}
