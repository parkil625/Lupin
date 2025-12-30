-- =========================================================
-- Auction Seed (FK-safe / deterministic within one run)
-- - TRUNCATE 대신 DELETE 사용 (FK 안정성)
-- - @now 고정으로 한 번 실행 내에서 시간 일관
-- - winner_id: users에 존재하는 값만 세팅, 없으면 NULL
-- =========================================================

SET time_zone = '+09:00';

-- 한 번 실행 내에서 시간 일관성 확보
SET @now := NOW();

-- --------------------------------------------------------
-- 초기화: 기존 데이터 삭제 (FK-safe)
-- --------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;

-- 경매가 다른 테이블(예: auction_bids)이 FK로 물려있다면,
-- 그 테이블도 먼저 지워야 함. (아래는 예시)
-- DELETE FROM auction_bids;

DELETE FROM auction_items;
DELETE FROM auctions;

-- AUTO_INCREMENT 초기화 (TRUNCATE 대체)
ALTER TABLE auction_items AUTO_INCREMENT = 1;
ALTER TABLE auctions AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;

-- --------------------------------------------------------
-- (선택) 낙찰자 후보 조회 (존재하는 유저만 winner_id로 사용)
-- user_id는 너희 seed에 맞게 바꿔도 됨.
-- 없으면 NULL로 들어가게 됨.
-- --------------------------------------------------------
SET @winner_201 := (SELECT id FROM users WHERE id = 201 LIMIT 1);
SET @winner_202 := (SELECT id FROM users WHERE id = 202 LIMIT 1);

-- --------------------------------------------------------
-- 1. [진행 중] 아이패드 프로 (시작됨, 입찰 없음)
-- - 너무 길게 ACTIVE로 잡으면 테스트가 애매해서,
--   "어제 시작 ~ 내일 종료" 정도로 현실적인 범위로 구성
-- --------------------------------------------------------
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
             0,
             @now - INTERVAL 1 DAY,
             @now + INTERVAL 8 HOUR,
             false,
             30,
             'ACTIVE',
             0,
             NULL,
             NULL
         );

SET @active_auction_id := LAST_INSERT_ID();

INSERT INTO auction_items (item_name, description, item_image, auction_id)
VALUES (
           '아이패드 프로 12.9형 (6세대)',
           'M2 칩 탑재, Liquid Retina XDR 디스플레이. 스페이스 그레이 색상 256GB 모델입니다. 미개봉 새상품입니다.',
           '/auctionActiveImg1.webp',
           @active_auction_id
       );

-- --------------------------------------------------------
-- 2. [예정] 맥북 에어 15인치
-- --------------------------------------------------------
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
             0,
             @now + INTERVAL 8 DAY,
             @now + INTERVAL 9 DAY,
             false,
             30,
             'SCHEDULED',
             0,
             NULL,
             NULL
         );

SET @scheduled_id_1 := LAST_INSERT_ID();

INSERT INTO auction_items (item_name, description, item_image, auction_id)
VALUES (
           'MacBook Air 15 미드나이트',
           '가볍지만 강력한 M3 칩 탑재. 15인치 대화면으로 즐기는 쾌적한 작업 환경. 램 16GB 업그레이드 모델.',
           '/auctionScheduledImg1.webp',
           @scheduled_id_1
       );

-- --------------------------------------------------------
-- 3. [예정] 플레이스테이션 5 프로
-- --------------------------------------------------------
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
             0,
             @now + INTERVAL 9 DAY,
             @now + INTERVAL 10 DAY,
             false,
             30,
             'SCHEDULED',
             0,
             NULL,
             NULL
         );

SET @scheduled_id_2 := LAST_INSERT_ID();

INSERT INTO auction_items (item_name, description, item_image, auction_id)
VALUES (
           'PlayStation 5 Pro',
           '더욱 강력해진 성능의 PS5 Pro. 듀얼센스 엣지 컨트롤러 포함 패키지입니다.',
           '/auctionScheduledImg2.webp',
           @scheduled_id_2
       );

-- --------------------------------------------------------
-- 4. [종료] 갤럭시 워치6 (winner_id는 존재하면 넣고, 없으면 NULL)
-- --------------------------------------------------------
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
             @now - INTERVAL 5 DAY,
             @now - INTERVAL 4 DAY,
             false,
             30,
             'ENDED',
             15,
             @winner_202,  -- users에 202가 없으면 NULL
             100
         );

SET @ended_auction_id_1 := LAST_INSERT_ID();

INSERT INTO auction_items (item_name, description, item_image, auction_id)
VALUES (
           '갤럭시 워치6 클래식 47mm',
           '회전 베젤의 클래식한 디자인. 블랙 색상, 블루투스 모델입니다. 스트랩 미사용 새제품 포함.',
           '/auctionEndedImg1.webp',
           @ended_auction_id_1
       );

-- --------------------------------------------------------
-- 5. [종료] 소니 노이즈캔슬링 헤드폰 (winner_id는 존재하면 넣고, 없으면 NULL)
-- --------------------------------------------------------
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
             @now - INTERVAL 10 DAY,
             @now - INTERVAL 9 DAY,
             false,
             30,
             'ENDED',
             23,
             @winner_201,  -- users에 201이 없으면 NULL
             200
         );

SET @ended_auction_id_2 := LAST_INSERT_ID();

INSERT INTO auction_items (item_name, description, item_image, auction_id)
VALUES (
           'Sony WH-1000XM5',
           '업계 최고의 노이즈 캔슬링. 플래티넘 실버 색상입니다. 박스 풀구성, 상태 S급입니다.',
           '/auctionEndedImg2.webp',
           @ended_auction_id_2
       );