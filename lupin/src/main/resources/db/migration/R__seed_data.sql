-- =====================================================
-- Flyway Repeatable Migration: Seed Data
-- 실제 활동하는 커뮤니티처럼 보이는 풍부한 테스트 데이터
-- 파일이 변경되면 자동으로 다시 실행됨
-- =====================================================

-- =====================================================
-- 1. USERS (14명의 활발한 회원들)
-- 비밀번호: 'test1234' (BCrypt 해시) / user01, doctor01은 비번 '1'
-- =====================================================
INSERT IGNORE INTO users (id, user_id, password, name, role, height, weight, gender, birth_date, avatar)
VALUES
    -- 테스트 계정 (비밀번호: 1)
    (13, 'user01', '$2a$10$c2TQaJnZaQLbMxjQ2GRs8OQr4aO7a6l8C0hKQ3wCGnA3lxmzz6vUi', '테스트유저', 'MEMBER', 175.0, 70.0, 'M', '1995-01-01', 'https://api.dicebear.com/7.x/avataaars/svg?seed=user01'),
    (14, 'doctor01', '$2a$10$c2TQaJnZaQLbMxjQ2GRs8OQr4aO7a6l8C0hKQ3wCGnA3lxmzz6vUi', '테스트의사', 'DOCTOR', 178.0, 72.0, 'M', '1985-01-01', 'https://api.dicebear.com/7.x/avataaars/svg?seed=doctor01'),
    -- 기존 회원들 (비밀번호: test1234)
    (1, 'minjun90', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3Iu.AryBxTfJJp6t6F/e', '김민준', 'MEMBER', 175.5, 70.0, 'M', '1990-01-15', 'https://api.dicebear.com/7.x/avataaars/svg?seed=minjun'),
    (2, 'soyeon95', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3Iu.AryBxTfJJp6t6F/e', '이소연', 'MEMBER', 168.0, 55.5, 'F', '1995-05-20', 'https://api.dicebear.com/7.x/avataaars/svg?seed=soyeon'),
    (3, 'junhyuk88', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3Iu.AryBxTfJJp6t6F/e', '박준혁', 'MEMBER', 180.0, 75.0, 'M', '1988-11-30', 'https://api.dicebear.com/7.x/avataaars/svg?seed=junhyuk'),
    (4, 'dr.choi', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3Iu.AryBxTfJJp6t6F/e', '최성훈', 'DOCTOR', 172.0, 68.0, 'M', '1985-03-10', 'https://api.dicebear.com/7.x/avataaars/svg?seed=sunghoon'),
    (5, 'yuna92', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3Iu.AryBxTfJJp6t6F/e', '정유나', 'MEMBER', 165.0, 52.0, 'F', '1992-07-25', 'https://api.dicebear.com/7.x/avataaars/svg?seed=yuna'),
    (6, 'runner_kim', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3Iu.AryBxTfJJp6t6F/e', '김태호', 'MEMBER', 178.0, 72.0, 'M', '1991-08-12', 'https://api.dicebear.com/7.x/avataaars/svg?seed=taeho'),
    (7, 'yoga_jin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3Iu.AryBxTfJJp6t6F/e', '한진아', 'MEMBER', 162.0, 48.0, 'F', '1994-02-28', 'https://api.dicebear.com/7.x/avataaars/svg?seed=jina'),
    (8, 'health_master', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3Iu.AryBxTfJJp6t6F/e', '오승민', 'MEMBER', 182.0, 80.0, 'M', '1987-06-15', 'https://api.dicebear.com/7.x/avataaars/svg?seed=seungmin'),
    (9, 'swim_love', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3Iu.AryBxTfJJp6t6F/e', '강수빈', 'MEMBER', 170.0, 58.0, 'F', '1993-12-05', 'https://api.dicebear.com/7.x/avataaars/svg?seed=subin'),
    (10, 'bike_rider', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3Iu.AryBxTfJJp6t6F/e', '임동현', 'MEMBER', 176.0, 73.0, 'M', '1989-04-22', 'https://api.dicebear.com/7.x/avataaars/svg?seed=donghyun'),
    (11, 'pilates_queen', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3Iu.AryBxTfJJp6t6F/e', '서예진', 'MEMBER', 166.0, 50.0, 'F', '1996-09-18', 'https://api.dicebear.com/7.x/avataaars/svg?seed=yejin'),
    (12, 'crossfit_pro', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3Iu.AryBxTfJJp6t6F/e', '장현우', 'MEMBER', 184.0, 85.0, 'M', '1990-11-08', 'https://api.dicebear.com/7.x/avataaars/svg?seed=hyunwoo');

-- =====================================================
-- 2. FEEDS (30개의 다양한 운동 피드)
-- 규칙: 피드당 최대 30점, 유저당 하루 1개 피드
-- =====================================================
INSERT IGNORE INTO feeds (id, writer_id, activity, calories, content, points, created_at)
VALUES
    -- 오늘 피드들
    (1, 1, '러닝', 320, '[{"type":"paragraph","content":[{"type":"text","text":"오늘 아침 5km 러닝 완료! 날씨가 좋아서 기분도 상쾌하네요. 한강변 코스 추천합니다!"}]}]', 28, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    (2, 6, '러닝', 450, '[{"type":"paragraph","content":[{"type":"text","text":"10km 완주! 드디어 50분대 진입했습니다. 꾸준히 하니까 되네요 ㅎㅎ"}]}]', 30, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
    (3, 7, '요가', 180, '[{"type":"paragraph","content":[{"type":"text","text":"아침 명상 요가 30분. 마음이 정말 편안해졌어요. 하루를 시작하기 좋은 루틴입니다."}]}]', 22, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
    (4, 8, '웨이트', 400, '[{"type":"paragraph","content":[{"type":"text","text":"등운동 데이! 데드리프트 140kg 5x5 성공. 점점 무게가 늘어나는 게 느껴집니다."}]}]', 30, DATE_SUB(NOW(), INTERVAL 5 HOUR)),

    -- 어제 피드들
    (5, 1, '웨이트', 380, '[{"type":"paragraph","content":[{"type":"text","text":"상체 운동 완료. 벤치프레스 개인 기록 갱신! 80kg 3세트 달성했습니다."}]}]', 27, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (6, 2, '요가', 150, '[{"type":"paragraph","content":[{"type":"text","text":"저녁 요가 클래스 다녀왔어요. 스트레칭하니까 하루 피로가 싹 풀리네요."}]}]', 20, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (7, 3, '수영', 350, '[{"type":"paragraph","content":[{"type":"text","text":"자유형 1km 완주! 수영은 관절에 무리 없어서 좋아요. 전신운동 최고."}]}]', 30, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (8, 9, '수영', 280, '[{"type":"paragraph","content":[{"type":"text","text":"오늘은 배영 연습했어요. 아직 서툴지만 재밌네요! 물에 뜨는 느낌이 좋아요."}]}]', 25, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (9, 10, '사이클', 500, '[{"type":"paragraph","content":[{"type":"text","text":"한강 라이딩 30km! 날씨 완전 좋았어요. 자전거 타고 바람 맞으니 스트레스 해소 됩니다."}]}]', 30, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (10, 11, '필라테스', 200, '[{"type":"paragraph","content":[{"type":"text","text":"기구 필라테스 1시간. 코어 운동 집중했더니 뱃살이 점점 빠지는 것 같아요!"}]}]', 25, DATE_SUB(NOW(), INTERVAL 1 DAY)),

    -- 2일 전 피드들
    (11, 2, '러닝', 250, '[{"type":"paragraph","content":[{"type":"text","text":"아침 조깅 3km. 꾸준히 하는 게 중요해요. 오늘도 화이팅!"}]}]', 22, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (12, 5, '필라테스', 180, '[{"type":"paragraph","content":[{"type":"text","text":"매트 필라테스 40분 완료. 자세 교정 중인데 확실히 효과가 있어요."}]}]', 20, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (13, 6, '웨이트', 420, '[{"type":"paragraph","content":[{"type":"text","text":"하체 운동 완료! 스쿼트 100kg 도전 성공. 다리가 후들후들하네요 ㅋㅋ"}]}]', 30, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (14, 12, '크로스핏', 550, '[{"type":"paragraph","content":[{"type":"text","text":"오늘 WOD 완료! 버피 100개 + 케틀벨 스윙 100개. 땀이 비오듯이 ㅋㅋㅋ"}]}]', 30, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (15, 8, '러닝', 380, '[{"type":"paragraph","content":[{"type":"text","text":"인터벌 러닝 5km. 400m 스프린트 + 400m 조깅 반복. 심폐지구력 훈련!"}]}]', 28, DATE_SUB(NOW(), INTERVAL 2 DAY)),

    -- 3일 전 피드들
    (16, 3, '웨이트', 350, '[{"type":"paragraph","content":[{"type":"text","text":"어깨 운동 집중! 오버헤드프레스 50kg 성공. 어깨가 넓어지는 중 ㅎㅎ"}]}]', 28, DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (17, 7, '필라테스', 160, '[{"type":"paragraph","content":[{"type":"text","text":"리포머 필라테스 체험! 기구 운동이라 더 효과적인 것 같아요. 추천합니다!"}]}]', 18, DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (18, 9, '러닝', 300, '[{"type":"paragraph","content":[{"type":"text","text":"퇴근 후 야간 러닝 5km. 밤공기가 시원해서 뛰기 좋았어요!"}]}]', 26, DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (19, 11, '요가', 140, '[{"type":"paragraph","content":[{"type":"text","text":"핫요가 60분! 땀을 엄청 흘렸어요. 디톡스 효과 만점입니다."}]}]', 18, DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (20, 4, '러닝', 280, '[{"type":"paragraph","content":[{"type":"text","text":"점심시간 가볍게 3km 조깅. 바쁜 일상 속 운동 시간 만들기!"}]}]', 24, DATE_SUB(NOW(), INTERVAL 3 DAY)),

    -- 4일 전 피드들
    (21, 1, '수영', 320, '[{"type":"paragraph","content":[{"type":"text","text":"접영 연습 중! 아직 어렵지만 조금씩 나아지고 있어요. 수영 재밌네요."}]}]', 26, DATE_SUB(NOW(), INTERVAL 4 DAY)),
    (22, 5, '요가', 120, '[{"type":"paragraph","content":[{"type":"text","text":"비니야사 요가 클래스. 유연성이 점점 좋아지는 게 느껴져요!"}]}]', 16, DATE_SUB(NOW(), INTERVAL 4 DAY)),
    (23, 10, '웨이트', 380, '[{"type":"paragraph","content":[{"type":"text","text":"가슴 운동 완료! 인클라인 벤치프레스 위주로 했어요. 윗가슴 발달 중!"}]}]', 28, DATE_SUB(NOW(), INTERVAL 4 DAY)),
    (24, 12, '러닝', 400, '[{"type":"paragraph","content":[{"type":"text","text":"트레일 러닝 8km! 산길 뛰니까 색다른 재미가 있네요. 자연 속 운동 최고!"}]}]', 30, DATE_SUB(NOW(), INTERVAL 4 DAY)),

    -- 5일 전 피드들
    (25, 2, '수영', 300, '[{"type":"paragraph","content":[{"type":"text","text":"평영 1km 완주! 수영은 정말 전신운동이에요. 다이어트에 효과적!"}]}]', 26, DATE_SUB(NOW(), INTERVAL 5 DAY)),
    (26, 6, '사이클', 480, '[{"type":"paragraph","content":[{"type":"text","text":"북한산 힐클라임! 오르막이 힘들었지만 정상에서 보는 뷰가 최고였어요."}]}]', 30, DATE_SUB(NOW(), INTERVAL 5 DAY)),
    (27, 8, '수영', 340, '[{"type":"paragraph","content":[{"type":"text","text":"자유형 + 배영 번갈아가며 1.5km. 수영장 가는 게 이제 습관이 됐어요!"}]}]', 28, DATE_SUB(NOW(), INTERVAL 5 DAY)),

    -- 6-7일 전 피드들
    (28, 3, '크로스핏', 500, '[{"type":"paragraph","content":[{"type":"text","text":"무술라 WOD 도전! 역시 크로스핏은 강도가 다르네요. 온몸이 땀범벅!"}]}]', 30, DATE_SUB(NOW(), INTERVAL 6 DAY)),
    (29, 7, '러닝', 280, '[{"type":"paragraph","content":[{"type":"text","text":"공원 조깅 4km. 가을 날씨에 뛰니까 기분이 좋아요. 단풍도 예쁘고!"}]}]', 24, DATE_SUB(NOW(), INTERVAL 6 DAY)),
    (30, 9, '필라테스', 170, '[{"type":"paragraph","content":[{"type":"text","text":"소도구 필라테스! 폼롤러로 근막이완 했더니 뭉친 근육이 풀리네요."}]}]', 19, DATE_SUB(NOW(), INTERVAL 7 DAY));

-- =====================================================
-- 3. POINT_LOGS (각 피드의 points와 일치)
-- =====================================================
INSERT IGNORE INTO point_logs (id, user_id, points, created_at)
VALUES
    (1, 1, 28, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    (2, 6, 30, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
    (3, 7, 22, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
    (4, 8, 30, DATE_SUB(NOW(), INTERVAL 5 HOUR)),
    (5, 1, 27, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (6, 2, 20, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (7, 3, 30, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (8, 9, 25, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (9, 10, 30, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (10, 11, 25, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (11, 2, 22, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (12, 5, 20, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (13, 6, 30, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (14, 12, 30, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (15, 8, 28, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (16, 3, 28, DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (17, 7, 18, DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (18, 9, 26, DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (19, 11, 18, DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (20, 4, 24, DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (21, 1, 26, DATE_SUB(NOW(), INTERVAL 4 DAY)),
    (22, 5, 16, DATE_SUB(NOW(), INTERVAL 4 DAY)),
    (23, 10, 28, DATE_SUB(NOW(), INTERVAL 4 DAY)),
    (24, 12, 30, DATE_SUB(NOW(), INTERVAL 4 DAY)),
    (25, 2, 26, DATE_SUB(NOW(), INTERVAL 5 DAY)),
    (26, 6, 30, DATE_SUB(NOW(), INTERVAL 5 DAY)),
    (27, 8, 28, DATE_SUB(NOW(), INTERVAL 5 DAY)),
    (28, 3, 30, DATE_SUB(NOW(), INTERVAL 6 DAY)),
    (29, 7, 24, DATE_SUB(NOW(), INTERVAL 6 DAY)),
    (30, 9, 19, DATE_SUB(NOW(), INTERVAL 7 DAY));

-- =====================================================
-- 4. FEED_LIKES (다양한 좋아요 - 총 60개)
-- =====================================================
INSERT IGNORE INTO feed_likes (id, user_id, feed_id, created_at)
VALUES
    -- Feed 1 (김민준의 러닝): 5개 좋아요
    (1, 2, 1, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
    (2, 3, 1, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
    (3, 6, 1, DATE_SUB(NOW(), INTERVAL 90 MINUTE)),
    (4, 7, 1, DATE_SUB(NOW(), INTERVAL 100 MINUTE)),
    (5, 8, 1, DATE_SUB(NOW(), INTERVAL 110 MINUTE)),

    -- Feed 2 (김태호의 10km 러닝): 7개 좋아요
    (6, 1, 2, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    (7, 3, 2, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    (8, 5, 2, DATE_SUB(NOW(), INTERVAL 150 MINUTE)),
    (9, 8, 2, DATE_SUB(NOW(), INTERVAL 160 MINUTE)),
    (10, 9, 2, DATE_SUB(NOW(), INTERVAL 170 MINUTE)),
    (11, 11, 2, DATE_SUB(NOW(), INTERVAL 175 MINUTE)),
    (12, 12, 2, DATE_SUB(NOW(), INTERVAL 180 MINUTE)),

    -- Feed 3 (한진아의 요가): 4개 좋아요
    (13, 2, 3, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
    (14, 5, 3, DATE_SUB(NOW(), INTERVAL 200 MINUTE)),
    (15, 9, 3, DATE_SUB(NOW(), INTERVAL 210 MINUTE)),
    (16, 11, 3, DATE_SUB(NOW(), INTERVAL 220 MINUTE)),

    -- Feed 4 (오승민의 웨이트): 6개 좋아요
    (17, 1, 4, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
    (18, 3, 4, DATE_SUB(NOW(), INTERVAL 250 MINUTE)),
    (19, 6, 4, DATE_SUB(NOW(), INTERVAL 260 MINUTE)),
    (20, 10, 4, DATE_SUB(NOW(), INTERVAL 270 MINUTE)),
    (21, 12, 4, DATE_SUB(NOW(), INTERVAL 280 MINUTE)),
    (22, 2, 4, DATE_SUB(NOW(), INTERVAL 290 MINUTE)),

    -- Feed 5-10: 각 3-4개 좋아요
    (23, 2, 5, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
    (24, 6, 5, DATE_SUB(NOW(), INTERVAL 21 HOUR)),
    (25, 9, 5, DATE_SUB(NOW(), INTERVAL 22 HOUR)),

    (26, 1, 6, DATE_SUB(NOW(), INTERVAL 23 HOUR)),
    (27, 7, 6, DATE_SUB(NOW(), INTERVAL 24 HOUR)),
    (28, 11, 6, DATE_SUB(NOW(), INTERVAL 25 HOUR)),
    (29, 5, 6, DATE_SUB(NOW(), INTERVAL 26 HOUR)),

    (30, 1, 7, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
    (31, 2, 7, DATE_SUB(NOW(), INTERVAL 21 HOUR)),
    (32, 9, 7, DATE_SUB(NOW(), INTERVAL 22 HOUR)),
    (33, 10, 7, DATE_SUB(NOW(), INTERVAL 23 HOUR)),

    (34, 3, 8, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
    (35, 5, 8, DATE_SUB(NOW(), INTERVAL 21 HOUR)),
    (36, 7, 8, DATE_SUB(NOW(), INTERVAL 22 HOUR)),

    (37, 1, 9, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
    (38, 3, 9, DATE_SUB(NOW(), INTERVAL 21 HOUR)),
    (39, 6, 9, DATE_SUB(NOW(), INTERVAL 22 HOUR)),
    (40, 8, 9, DATE_SUB(NOW(), INTERVAL 23 HOUR)),

    (41, 2, 10, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
    (42, 7, 10, DATE_SUB(NOW(), INTERVAL 21 HOUR)),
    (43, 9, 10, DATE_SUB(NOW(), INTERVAL 22 HOUR)),

    -- Feed 14 (크로스핏): 8개 좋아요 (인기 게시물)
    (44, 1, 14, DATE_SUB(NOW(), INTERVAL 40 HOUR)),
    (45, 2, 14, DATE_SUB(NOW(), INTERVAL 41 HOUR)),
    (46, 3, 14, DATE_SUB(NOW(), INTERVAL 42 HOUR)),
    (47, 6, 14, DATE_SUB(NOW(), INTERVAL 43 HOUR)),
    (48, 7, 14, DATE_SUB(NOW(), INTERVAL 44 HOUR)),
    (49, 8, 14, DATE_SUB(NOW(), INTERVAL 45 HOUR)),
    (50, 9, 14, DATE_SUB(NOW(), INTERVAL 46 HOUR)),
    (51, 10, 14, DATE_SUB(NOW(), INTERVAL 47 HOUR)),

    -- 나머지 피드들 좋아요
    (52, 1, 13, DATE_SUB(NOW(), INTERVAL 44 HOUR)),
    (53, 5, 13, DATE_SUB(NOW(), INTERVAL 45 HOUR)),
    (54, 8, 13, DATE_SUB(NOW(), INTERVAL 46 HOUR)),
    (55, 12, 13, DATE_SUB(NOW(), INTERVAL 47 HOUR)),

    (56, 2, 16, DATE_SUB(NOW(), INTERVAL 68 HOUR)),
    (57, 6, 16, DATE_SUB(NOW(), INTERVAL 69 HOUR)),
    (58, 10, 16, DATE_SUB(NOW(), INTERVAL 70 HOUR)),

    (59, 1, 28, DATE_SUB(NOW(), INTERVAL 140 HOUR)),
    (60, 5, 28, DATE_SUB(NOW(), INTERVAL 141 HOUR));

-- =====================================================
-- 5. COMMENTS (40개의 활발한 댓글들)
-- =====================================================
INSERT IGNORE INTO comments (id, writer_id, feed_id, parent_id, content, created_at)
VALUES
    -- Feed 1 (김민준의 러닝) 댓글들
    (1, 2, 1, NULL, '대단해요! 아침 러닝 부럽습니다 ㅎㅎ', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
    (2, 6, 1, NULL, '한강 어느 코스로 뛰셨어요? 저도 가보고 싶네요!', DATE_SUB(NOW(), INTERVAL 90 MINUTE)),
    (3, 1, 1, 2, '잠실 쪽에서 뚝섬까지 왕복했어요! 추천합니다 ㅎㅎ', DATE_SUB(NOW(), INTERVAL 80 MINUTE)),
    (4, 7, 1, NULL, '아침 운동하면 하루가 개운하죠 ㅎㅎ 화이팅!', DATE_SUB(NOW(), INTERVAL 70 MINUTE)),

    -- Feed 2 (김태호의 10km 러닝) 댓글들
    (5, 1, 2, NULL, '50분대 진입 축하드려요! 대단하십니다 👏', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    (6, 3, 2, NULL, '저도 50분대 목표인데... 비결이 뭐예요?', DATE_SUB(NOW(), INTERVAL 150 MINUTE)),
    (7, 6, 2, 6, '인터벌 훈련 추천해요! 속도 많이 올라갑니다.', DATE_SUB(NOW(), INTERVAL 140 MINUTE)),
    (8, 8, 2, NULL, '꾸준함이 답이네요. 저도 열심히 해야겠어요!', DATE_SUB(NOW(), INTERVAL 130 MINUTE)),
    (9, 12, 2, NULL, '다음 목표는 뭐예요? 하프 마라톤 도전?', DATE_SUB(NOW(), INTERVAL 120 MINUTE)),
    (10, 6, 2, 9, '네 다음 달 하프마라톤 나갈 예정입니다!', DATE_SUB(NOW(), INTERVAL 110 MINUTE)),

    -- Feed 3 (한진아의 요가) 댓글들
    (11, 5, 3, NULL, '아침 요가 저도 해보고 싶어요! 추천 동작 있으신가요?', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
    (12, 7, 3, 11, '태양 경배 시퀀스로 시작하면 좋아요!', DATE_SUB(NOW(), INTERVAL 170 MINUTE)),
    (13, 11, 3, NULL, '명상까지 하시다니 부럽네요. 저는 집중이 잘 안돼요 ㅠㅠ', DATE_SUB(NOW(), INTERVAL 160 MINUTE)),

    -- Feed 4 (오승민의 웨이트) 댓글들
    (14, 3, 4, NULL, '데드 140kg 대단하십니다! 폼 어떻게 잡으셨어요?', DATE_SUB(NOW(), INTERVAL 4 HOUR)),
    (15, 12, 4, NULL, '등 운동 같이 해요! 저도 데드 좋아합니다 ㅎㅎ', DATE_SUB(NOW(), INTERVAL 250 MINUTE)),
    (16, 8, 4, 14, '컨벤셔널 폼으로 하고 있어요. 허리 보호가 중요합니다!', DATE_SUB(NOW(), INTERVAL 240 MINUTE)),
    (17, 10, 4, NULL, '저도 데드리프트 배우고 싶은데 무서워요 ㅠㅠ', DATE_SUB(NOW(), INTERVAL 230 MINUTE)),
    (18, 8, 4, 17, '처음엔 빈 바로 폼부터 연습하시면 돼요!', DATE_SUB(NOW(), INTERVAL 220 MINUTE)),

    -- Feed 7 (박준혁의 수영) 댓글들
    (19, 9, 7, NULL, '자유형 1km 대단해요! 저는 500m도 힘들어요 ㅋㅋ', DATE_SUB(NOW(), INTERVAL 20 HOUR)),
    (20, 3, 7, 19, '천천히 하다보면 늘어요! 처음엔 저도 그랬어요 ㅎㅎ', DATE_SUB(NOW(), INTERVAL 19 HOUR)),
    (21, 2, 7, NULL, '수영 관절에 좋다는 말 진짜예요?', DATE_SUB(NOW(), INTERVAL 18 HOUR)),
    (22, 3, 7, 21, '네 부력 덕분에 관절 부담이 적어요. 재활 운동으로도 좋습니다!', DATE_SUB(NOW(), INTERVAL 17 HOUR)),

    -- Feed 14 (장현우의 크로스핏) 댓글들
    (23, 1, 14, NULL, '버피 100개 미쳤어요 ㄷㄷㄷ 존경합니다', DATE_SUB(NOW(), INTERVAL 40 HOUR)),
    (24, 6, 14, NULL, '크로스핏 하시는 분들 체력이 진짜 대단한 것 같아요', DATE_SUB(NOW(), INTERVAL 39 HOUR)),
    (25, 8, 14, NULL, 'WOD 뭐였어요? 저도 해보고 싶네요!', DATE_SUB(NOW(), INTERVAL 38 HOUR)),
    (26, 12, 14, 25, '신디 변형이에요! 5라운드 AMRAP입니다.', DATE_SUB(NOW(), INTERVAL 37 HOUR)),
    (27, 3, 14, NULL, '크로스핏 체험 해보고 싶은데 어디서 하시나요?', DATE_SUB(NOW(), INTERVAL 36 HOUR)),
    (28, 12, 14, 27, '강남에 있는 OO박스 추천해요!', DATE_SUB(NOW(), INTERVAL 35 HOUR)),

    -- Feed 9 (임동현의 사이클) 댓글들
    (29, 6, 9, NULL, '한강 라이딩 최고죠! 저도 어제 탔어요 ㅎㅎ', DATE_SUB(NOW(), INTERVAL 20 HOUR)),
    (30, 1, 9, NULL, '30km 대단해요! 엉덩이 안 아프세요?', DATE_SUB(NOW(), INTERVAL 19 HOUR)),
    (31, 10, 9, 30, 'ㅋㅋㅋ 패드 바지 필수입니다!', DATE_SUB(NOW(), INTERVAL 18 HOUR)),

    -- Feed 10 (서예진의 필라테스) 댓글들
    (32, 7, 10, NULL, '기구 필라테스 효과 좋나요? 매트만 하고 있어서요', DATE_SUB(NOW(), INTERVAL 20 HOUR)),
    (33, 11, 10, 32, '네 훨씬 좋아요! 자세 교정에 특히 효과적이에요.', DATE_SUB(NOW(), INTERVAL 19 HOUR)),
    (34, 5, 10, NULL, '어느 센터 다니세요? 저도 등록하고 싶어요!', DATE_SUB(NOW(), INTERVAL 18 HOUR)),

    -- 기타 피드들 댓글
    (35, 2, 13, NULL, '스쿼트 100kg 대단해요! 무릎은 괜찮으세요?', DATE_SUB(NOW(), INTERVAL 44 HOUR)),
    (36, 6, 13, 35, '무릎 보호대 차고 해요! 워밍업 충분히 하면 괜찮습니다.', DATE_SUB(NOW(), INTERVAL 43 HOUR)),
    (37, 1, 26, NULL, '북한산 힐클라임 대단해요! 저도 도전해보고 싶네요.', DATE_SUB(NOW(), INTERVAL 116 HOUR)),
    (38, 3, 16, NULL, '오버헤드프레스 어깨 넓어지는데 최고죠!', DATE_SUB(NOW(), INTERVAL 68 HOUR)),
    (39, 5, 22, NULL, '비니야사 저도 좋아해요! 어디서 배우셨어요?', DATE_SUB(NOW(), INTERVAL 92 HOUR)),
    (40, 11, 19, NULL, '핫요가 땀 진짜 많이 나죠 ㅋㅋ 디톡스 효과 최고!', DATE_SUB(NOW(), INTERVAL 68 HOUR));

-- =====================================================
-- 정합성 요약:
-- 규칙: 피드당 최대 30점, 유저당 하루 1개 피드 (모두 준수)
-- =====================================================
-- | 항목     | 수량  | 비고                            |
-- |----------|-------|---------------------------------|
-- | Users    | 12명  | 11명 MEMBER + 1명 DOCTOR        |
-- | Feeds    | 30개  | 7일간의 다양한 운동 기록        |
-- | Likes    | 60개  | 활발한 좋아요 활동              |
-- | Comments | 40개  | 답글 포함 활발한 댓글           |
-- | Points   | 30개  | 각 피드와 1:1 매칭              |
-- =====================================================
