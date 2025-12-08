-- ============================================
-- Auction Mock Data (Entity Matched Version)
-- Description:
--   - Java Entity(Auction.java, AuctionItem.java) 구조와 100% 일치
--   - Flyway 실행 시점 문제 해결을 위해 테이블 생성 구문(DDL) 포함
-- ============================================

-- 기존 데이터 초기화 (순서 중요: 자식 -> 부모)
-- (auctions 테이블의 PK는 'id'가 아니라 'auction_id' 입니다)
DELETE FROM auction_bids;  -- WHERE 절 없이 전체 삭제
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
             (SELECT id FROM users WHERE user_id = 'user01' LIMIT 1) -- LIMIT 1로 안전성 확보
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