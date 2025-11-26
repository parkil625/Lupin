package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.dto.request.FeedCreateRequest;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final FeedRepository feedRepository;
    private final UserRepository userRepository;

    public Feed createFeed(Long userId, FeedCreateRequest request) {
        // TODO: 구현 필요
        return null;
    }
}
