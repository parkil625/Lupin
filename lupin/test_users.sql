-- 20명의 테스트 유저 생성 (포인트 다양하게 설정)
INSERT INTO users (email, password, real_name, role, height, weight, gender, birth_date, current_points, total_points, department, created_at, updated_at)
VALUES
-- 상위 랭커들 (1-10등)
('user1@test.com', '$2a$10$dummypasswordhash', '김강민', 'MEMBER', 175.0, 70.0, '남성', '1990-01-15', 850, 850, '개발팀', NOW(), NOW()),
('user2@test.com', '$2a$10$dummypasswordhash', '이서연', 'MEMBER', 165.0, 55.0, '여성', '1992-03-22', 820, 820, '마케팅팀', NOW(), NOW()),
('user3@test.com', '$2a$10$dummypasswordhash', '박준호', 'MEMBER', 180.0, 75.0, '남성', '1988-07-10', 790, 790, '영업팀', NOW(), NOW()),
('user4@test.com', '$2a$10$dummypasswordhash', '최지우', 'MEMBER', 170.0, 60.0, '여성', '1995-05-18', 760, 760, '디자인팀', NOW(), NOW()),
('user5@test.com', '$2a$10$dummypasswordhash', '정민수', 'MEMBER', 178.0, 72.0, '남성', '1991-11-30', 730, 730, '인사팀', NOW(), NOW()),
('user6@test.com', '$2a$10$dummypasswordhash', '강혜진', 'MEMBER', 162.0, 52.0, '여성', '1993-08-25', 700, 700, '재무팀', NOW(), NOW()),
('user7@test.com', '$2a$10$dummypasswordhash', '윤태양', 'MEMBER', 183.0, 78.0, '남성', '1989-04-12', 670, 670, '법무팀', NOW(), NOW()),
('user8@test.com', '$2a$10$dummypasswordhash', '한소희', 'MEMBER', 168.0, 58.0, '여성', '1994-09-08', 640, 640, '경영지원팀', NOW(), NOW()),
('user9@test.com', '$2a$10$dummypasswordhash', '오성민', 'MEMBER', 176.0, 73.0, '남성', '1990-12-20', 610, 610, '연구개발팀', NOW(), NOW()),
('user10@test.com', '$2a$10$dummypasswordhash', '서은주', 'MEMBER', 163.0, 54.0, '여성', '1996-02-14', 580, 580, '기획팀', NOW(), NOW()),

-- 중간 랭커들 (11-15등)
('user11@test.com', '$2a$10$dummypasswordhash', '임동혁', 'MEMBER', 179.0, 74.0, '남성', '1991-06-18', 550, 550, '개발팀', NOW(), NOW()),
('user12@test.com', '$2a$10$dummypasswordhash', '배수지', 'MEMBER', 166.0, 56.0, '여성', '1993-10-11', 520, 520, '마케팅팀', NOW(), NOW()),
('user13@test.com', '$2a$10$dummypasswordhash', '신재호', 'MEMBER', 181.0, 76.0, '남성', '1987-03-28', 490, 490, '영업팀', NOW(), NOW()),
('user14@test.com', '$2a$10$dummypasswordhash', '조미라', 'MEMBER', 164.0, 53.0, '여성', '1995-12-05', 460, 460, '디자인팀', NOW(), NOW()),
('user15@test.com', '$2a$10$dummypasswordhash', '홍길동', 'MEMBER', 177.0, 71.0, '남성', '1992-08-22', 430, 430, '인사팀', NOW(), NOW()),

-- 하위 랭커들 (16-20등)
('user16@test.com', '$2a$10$dummypasswordhash', '안지영', 'MEMBER', 161.0, 51.0, '여성', '1994-04-16', 400, 400, '재무팀', NOW(), NOW()),
('user17@test.com', '$2a$10$dummypasswordhash', '유재석', 'MEMBER', 174.0, 69.0, '남성', '1990-07-30', 370, 370, '법무팀', NOW(), NOW()),
('user18@test.com', '$2a$10$dummypasswordhash', '송혜교', 'MEMBER', 167.0, 57.0, '여성', '1993-11-19', 340, 340, '경영지원팀', NOW(), NOW()),
('user19@test.com', '$2a$10$dummypasswordhash', '전지현', 'MEMBER', 169.0, 59.0, '여성', '1991-01-25', 310, 310, '연구개발팀', NOW(), NOW()),
('user20@test.com', '$2a$10$dummypasswordhash', '현빈', 'MEMBER', 182.0, 77.0, '남성', '1989-09-13', 280, 280, '기획팀', NOW(), NOW());
