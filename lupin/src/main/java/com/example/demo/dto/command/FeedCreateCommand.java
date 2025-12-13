package com.example.demo.dto.command;

import com.example.demo.domain.entity.User;
import com.example.demo.dto.request.FeedRequest;

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
        List<String> otherImageKeys
) {
    public static FeedCreateCommand of(User writer, FeedRequest request) {
        return new FeedCreateCommand(
                writer,
                request.getActivity(),
                request.getContent(),
                request.getStartImageKey(),
                request.getEndImageKey(),
                request.getOtherImageKeys()
        );
    }

    public static FeedCreateCommand of(User writer, String activity, String content,
                                       String startImageKey, String endImageKey, List<String> otherImageKeys) {
        return new FeedCreateCommand(writer, activity, content, startImageKey, endImageKey, otherImageKeys);
    }

    public boolean hasImages() {
        return startImageKey != null && endImageKey != null;
    }
}
