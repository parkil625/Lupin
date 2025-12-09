-- ============================================
-- Auction Mock Data (Entity Matched Version)
-- Description:
--   - Java Entity(Auction.java, AuctionItem.java) 구조와 100% 일치
--   - 테이블이 없으면 자동 생성 (JPA ddl-auto보다 먼저 실행되어도 안전)
-- ============================================

-- 1. 테이블이 없으면 생성 (CREATE TABLE IF NOT EXISTS)
CREATE TABLE IF NOT EXISTS auctions (
    auction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    current_price BIGINT NOT NULL DEFAULT 0,
    start_time DATETIME NOT NULL,
    regular_end_time DATETIME NOT NULL,
    over_time_started BOOLEAN NOT NULL DEFAULT FALSE,
    over_time_end_time DATETIME,
    over_time_seconds INT NOT NULL DEFAULT 30,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    winner_id BIGINT,
    winning_bid BIGINT,
    total_bids INT NOT NULL DEFAULT 0,

    INDEX idx_auction_status (status),
    INDEX idx_auction_start_time (start_time),
    INDEX idx_auction_end_time (regular_end_time)
);

CREATE TABLE IF NOT EXISTS auction_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    item_image VARCHAR(500),
    auction_id BIGINT,

    INDEX idx_auction_item_auction (auction_id)
);

CREATE TABLE IF NOT EXISTS auction_bids (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    auction_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    bid_amount BIGINT NOT NULL,
    bid_time DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    INDEX idx_bid_auction (auction_id),
    INDEX idx_bid_user (user_id),
    INDEX idx_bid_status (status),
    INDEX idx_bid_time (bid_time)
);

-- 2. 기존 데이터 초기화 (순서 중요: 자식 -> 부모)
DELETE FROM auction_bids;
DELETE FROM auction_items;
DELETE FROM auctions;

-- 3. 데이터 입력 시작

-- --------------------------------------------
-- 1. [진행 중] 아이패드 프로 12.9
-- --------------------------------------------
INSERT INTO auctions (
    current_price,
    start_time,
    regular_end_time,
    over_time_started,
    over_time_seconds,
    status,
    total_bids,
    winner_id
) VALUES (
    100,
    NOW() - INTERVAL 1 DAY,
    NOW() + INTERVAL 7 DAY,
    false,
    30,
    'ACTIVE',
    15,
    (SELECT id FROM users WHERE user_id = 'user01' LIMIT 1)
);

SET @active_auction_id = LAST_INSERT_ID();

INSERT INTO auction_items (item_name, description, item_image, auction_id)
VALUES (
    '아이패드 프로 12.9형 (6세대)',
    'M2 칩 탑재, Liquid Retina XDR 디스플레이. 스페이스 그레이 색상 256GB 모델입니다. 미개봉 새상품입니다.',
    'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?q=80&w=1000&auto=format&fit=crop',
    @active_auction_id
);

-- --------------------------------------------
-- 2. [예정] 맥북 에어 15인치
-- --------------------------------------------
INSERT INTO auctions (
    current_price,
    start_time,
    regular_end_time,
    over_time_started,
    over_time_seconds,
    status,
    total_bids,
    winner_id
) VALUES (
    0,
    NOW() + INTERVAL 8 DAY,
    NOW() + INTERVAL 9 DAY,
    false,
    30,
    'SCHEDULED',
    0,
    NULL
);

SET @scheduled_id_1 = LAST_INSERT_ID();

INSERT INTO auction_items (item_name, description, item_image, auction_id)
VALUES (
    'MacBook Air 15 미드나이트',
    '가볍지만 강력한 M3 칩 탑재. 15인치 대화면으로 즐기는 쾌적한 작업 환경. 램 16GB 업그레이드 모델.',
    'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?q=80&w=1026&auto=format&fit=crop',
    @scheduled_id_1
);

-- --------------------------------------------
-- 3. [예정] 플레이스테이션 5 프로
-- --------------------------------------------
INSERT INTO auctions (
    current_price,
    start_time,
    regular_end_time,
    over_time_started,
    over_time_seconds,
    status,
    total_bids,
    winner_id
) VALUES (
    0,
    NOW() + INTERVAL 9 DAY,
    NOW() + INTERVAL 10 DAY,
    false,
    30,
    'SCHEDULED',
    0,
    NULL
);

SET @scheduled_id_2 = LAST_INSERT_ID();

INSERT INTO auction_items (item_name, description, item_image, auction_id)
VALUES (
    'PlayStation 5 Pro',
    '더욱 강력해진 성능의 PS5 Pro. 듀얼센스 엣지 컨트롤러 포함 패키지입니다.',
    'https://images.unsplash.com/photo-1606144042614-b2417e99c4e3?q=80&w=1000&auto=format&fit=crop',
    @scheduled_id_2
);
