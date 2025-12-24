-- ============================================
-- 1. [필수] 꼬여있는 기존 테이블을 먼저 확실히 지웁니다.
-- ============================================
SET FOREIGN_KEY_CHECKS = 0; -- 외래키 에러 방지
DROP TABLE IF EXISTS auction_bids;
DROP TABLE IF EXISTS auction_items;
DROP TABLE IF EXISTS auctions;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 2. 여기서부터는 작성하신 코드와 동일합니다 (새 구조로 생성)
-- ============================================

CREATE TABLE auctions ( -- IF NOT EXISTS 제거함 (위에서 지웠으니 무조건 생성)
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

CREATE TABLE auction_items (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               item_name VARCHAR(200) NOT NULL,
                               description VARCHAR(1000),
                               item_image VARCHAR(500),
                               auction_id BIGINT,

                               INDEX idx_auction_item_auction (auction_id)
);

CREATE TABLE auction_bids (
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

-- 3. 데이터 입력 시작

-- --------------------------------------------
-- 1. [진행 중] 아이패드 프로 12.9 (2분 뒤 종료 테스트용)
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
             NOW() - INTERVAL 1 DAY,
             NOW() + INTERVAL 2 DAY,
             false,
             30,
             'ACTIVE',
             0,
             NULL -- 진행 중에는 아직 낙찰자가 없으므로 NULL이 맞습니다 (user01 제거)
         );

SET @active_auction_id = LAST_INSERT_ID();

INSERT INTO auction_items (item_name, description, item_image, auction_id)
VALUES (
           '아이패드 프로 12.9형 (6세대)',
           'M2 칩 탑재, Liquid Retina XDR 디스플레이. 스페이스 그레이 색상 256GB 모델입니다. 미개봉 새상품입니다.',
           '/auctionActiveImg1.webp',
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
           '/auctionScheduledImg1.webp',
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
           '/auctionScheduledImg2.webp',
           @scheduled_id_2
       );

-- --------------------------------------------
-- 4. [종료] 갤럭시 워치6 (낙찰됨)
-- --------------------------------------------
INSERT INTO auctions (
    current_price,
    start_time,
    regular_end_time,
    over_time_started,
    over_time_seconds,
    status,
    total_bids,
    winner_id,
    winning_bid
) VALUES (
             100,
             NOW() - INTERVAL 5 DAY,
             NOW() - INTERVAL 4 DAY,
             false,
             30,
             'ENDED',
             15,
             (SELECT id FROM users WHERE user_id = 'user01' LIMIT 1),
    100
    );

SET @ended_auction_id_1 = LAST_INSERT_ID();

INSERT INTO auction_items (item_name, description, item_image, auction_id)
VALUES (
           '갤럭시 워치6 클래식 47mm',
           '회전 베젤의 클래식한 디자인. 블랙 색상, 블루투스 모델입니다. 스트랩 미사용 새제품 포함.',
           '/auctionEndedImg1.webp',
           @ended_auction_id_1
       );

-- --------------------------------------------
-- 5. [종료] 소니 노이즈캔슬링 헤드폰 (낙찰됨)
-- --------------------------------------------
INSERT INTO auctions (
    current_price,
    start_time,
    regular_end_time,
    over_time_started,
    over_time_seconds,
    status,
    total_bids,
    winner_id,
    winning_bid
) VALUES (
             200,
             NOW() - INTERVAL 10 DAY,
             NOW() - INTERVAL 9 DAY,
             false,
             30,
             'ENDED',
             23,
             (SELECT id FROM users WHERE user_id = 'user02' LIMIT 1),
    200
    );

SET @ended_auction_id_2 = LAST_INSERT_ID();

INSERT INTO auction_items (item_name, description, item_image, auction_id)
VALUES (
           'Sony WH-1000XM5',
           '업계 최고의 노이즈 캔슬링. 플래티넘 실버 색상입니다. 박스 풀구성, 상태 S급입니다.',
           '/auctionEndedImg2.webp',
           @ended_auction_id_2
       );