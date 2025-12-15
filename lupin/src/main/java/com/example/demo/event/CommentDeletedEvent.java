package com.example.demo.event;

import java.util.List;

public record CommentDeletedEvent(
        Long commentId,
        Long feedId,
        Long writerId,
        boolean isParent,
        List<Long> replyIds
) {
    public static CommentDeletedEvent of(Long commentId, Long feedId, Long writerId, boolean isParent, List<Long> replyIds) {
        return new CommentDeletedEvent(commentId, feedId, writerId, isParent, replyIds);
    }
}
