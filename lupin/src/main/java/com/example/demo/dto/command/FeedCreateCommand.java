package com.example.demo.dto.command;

import com.example.demo.domain.entity.User;
import com.example.demo.dto.request.FeedRequest;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 피드 생성 명령 객체 (Parameter Object 패턴)
 * 여러 파라미터를 의미 있는 단위로 그룹화
 */
public record FeedCreateCommand(
        User writer,
        String activity,
        String content,
        String startImageKey,
        String endImageKey,
        List<String> otherImageKeys,
        // [추가] 시간 정보 필드
        LocalDateTime startAt,
        LocalDateTime endAt
) {
    public static FeedCreateCommand of(User writer, FeedRequest request) {
        return new FeedCreateCommand(
                writer,
                request.getActivity(),
                request.getContent(),
                request.getStartImageKey(),
                request.getEndImageKey(),
                request.getOtherImageKeys(),
                // [추가] Request에서 시간 정보 매핑
                request.getStartAt(),
                request.getEndAt()
        );
    }

    // 테스트용 생성자 (시간 정보가 없을 때 null 처리)
    public static FeedCreateCommand of(User writer, String activity, String content,
                                       String startImageKey, String endImageKey, List<String> otherImageKeys) {
        return new FeedCreateCommand(writer, activity, content, startImageKey, endImageKey, otherImageKeys, null, null);
    }

    public boolean hasImages() {
        return startImageKey != null && endImageKey != null;
    }
}
