package com.example.demo;

import com.example.demo.config.TestRedisConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
class LupinApplicationTests {

	@Test
	void contextLoads() {
	}

}
