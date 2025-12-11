-- ============================================
-- Feed 반정규화 필드 동기화
-- Date: 2025-12-11
-- Description:
--   - like_count: 실제 좋아요 수와 동기화
--   - comment_count: 실제 댓글 수와 동기화
-- ============================================

-- 좋아요 수 동기화
UPDATE feeds f
SET f.like_count = (
    SELECT COUNT(*)
    FROM feed_likes fl
    WHERE fl.feed_id = f.id
);

-- 댓글 수 동기화
UPDATE feeds f
SET f.comment_count = (
    SELECT COUNT(*)
    FROM comments c
    WHERE c.feed_id = f.id
);
