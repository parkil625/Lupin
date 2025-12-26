package com.example.demo.dto.command;

import com.example.demo.domain.entity.User;
import com.example.demo.dto.request.FeedRequest;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 피드 수정 명령 객체 (Parameter Object 패턴)
 */
public record FeedUpdateCommand(
        User user,
        Long feedId,
        String content,
        String activity,
        String startImageKey,
        String endImageKey,
        List<String> otherImageKeys,
        boolean imagesChanged,
        LocalDateTime startAt, // [추가]
        LocalDateTime endAt    // [추가]
) {
    public static FeedUpdateCommand of(User user, Long feedId, FeedRequest request) {
        return new FeedUpdateCommand(
                user,
                feedId,
                request.getContent(),
                request.getActivity(),
                request.getStartImageKey(),
                request.getEndImageKey(),
                request.getOtherImageKeys(),
                // [수정] Boolean -> boolean 안전한 변환 (null이면 false)
                Boolean.TRUE.equals(request.getImagesChanged()),
                request.getStartAt(),
                request.getEndAt()
        );
    }

    public boolean hasImages() {
        return startImageKey != null && endImageKey != null;
    }
}