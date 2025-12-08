-- ============================================
-- Auction & AuctionItem Mock Data (Long Life Version)
-- Date: 2025-11-27
-- Description:
--   - 데이터 수명을 넉넉하게 늘려 테스트 중 데이터가 사라지는 것을 방지함
--   - 1. 진행 중인 경매 (ACTIVE) : 시작 1일 전 ~ 종료 7일 후 (일주일간 유지됨)
--   - 2. 예정된 경매 1 (SCHEDULED) : 8일 뒤 시작
--   - 3. 예정된 경매 2 (SCHEDULED) : 9일 뒤 시작
-- ============================================

-- --------------------------------------------
-- 1. [진행 중] 아이패드 프로 12.9
-- (테스트를 위해 앞으로 7일 동안 계속 '진행 중' 상태로 유지됩니다)
-- --------------------------------------------
-- 기존 경매 데이터 초기화 (재실행 시 중복 방지)
-- 외래 키 제약 조건 때문에 자식 테이블(auction_items)을 먼저 삭제해야 합니다.
DELETE FROM auction_items WHERE id > 0;
DELETE FROM auctions WHERE id > 0;


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
             NOW() - INTERVAL 1 DAY,   -- 어제 시작함
             NOW() + INTERVAL 7 DAY,   -- 앞으로 7일 뒤 종료
             false,
             30,
             'ACTIVE',
             15,
             (SELECT id FROM users WHERE user_id = 'user01')
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
-- (진행 중인 경매가 끝난 다음 날인, 8일 뒤에 시작됩니다)
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
             NOW() + INTERVAL 8 DAY, -- 8일 뒤 시작
             NOW() + INTERVAL 9 DAY, -- 9일 뒤 종료 (하루 동안 진행)
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
           'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?q=80&w=1026&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D',
           @scheduled_id_1
       );


-- --------------------------------------------
-- 3. [예정] 플레이스테이션 5 프로
-- (9일 뒤에 시작됩니다)
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
             NOW() + INTERVAL 9 DAY,  -- 9일 뒤 시작
             NOW() + INTERVAL 10 DAY, -- 10일 뒤 종료
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