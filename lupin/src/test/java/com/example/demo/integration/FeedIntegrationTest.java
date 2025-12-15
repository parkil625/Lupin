package com.example.demo.integration;

import com.example.demo.config.TestRedisConfig;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.request.FeedRequest;
import com.example.demo.repository.FeedImageRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestRedisConfig.class)
class FeedIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private FeedImageRepository feedImageRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId("testuser")
                .password("password")
                .name("Test User")
                .role(Role.MEMBER)
                .build();
        userRepository.save(testUser);
    }

    @Test
    @WithUserDetails(value = "testuser", userDetailsServiceBeanName = "userDetailsService")
    @DisplayName("피드 생성, 수정, 삭제 통합 테스트")
    void feedCudIntegrationTest() throws Exception {
        // 1. Create
        FeedRequest createRequest = new FeedRequest("running", "New Feed", "start.jpg", "end.jpg", List.of("other.jpg"), List.of());
        String responseBody = mockMvc.perform(post("/api/feeds")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("New Feed"))
                .andReturn().getResponse().getContentAsString();

        Feed savedFeed = objectMapper.readValue(responseBody, Feed.class);
        assertThat(feedRepository.findById(savedFeed.getId())).isPresent();
        assertThat(feedImageRepository.count()).isEqualTo(3);

        // 2. Update
        FeedRequest updateRequest = new FeedRequest("swimming", "Updated Feed", "new_start.jpg", "new_end.jpg", List.of(), List.of());
        mockMvc.perform(put("/api/feeds/" + savedFeed.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Updated Feed"));

        Feed updatedFeed = feedRepository.findById(savedFeed.getId()).orElseThrow();
        assertThat(updatedFeed.getActivity()).isEqualTo("swimming");
        assertThat(feedImageRepository.count()).isEqualTo(2);

        // 3. Delete
        mockMvc.perform(delete("/api/feeds/" + savedFeed.getId())
                        .with(csrf()))
                .andExpect(status().isOk());

        assertThat(feedRepository.findById(savedFeed.getId())).isEmpty();
    }
}