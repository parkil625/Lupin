-- Soft Delete 컬럼 추가

-- feeds 테이블
ALTER TABLE feeds ADD COLUMN deleted_at DATETIME NULL;
CREATE INDEX idx_feeds_deleted_at ON feeds(deleted_at);

-- comments 테이블
ALTER TABLE comments ADD COLUMN deleted_at DATETIME NULL;
CREATE INDEX idx_comments_deleted_at ON comments(deleted_at);
