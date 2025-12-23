-- =====================================================
-- Flyway Repeatable Migration: Seed Data (Real Names)
-- 초기 가입 상태의 의사 6명 + 일반 유저 9명
-- 비밀번호 통일: $2a$12$xuPvMellTFfVLkWHAgj8uOK06sbIYpnB1jHdsokSfsbvEYn6yz2GS
-- =====================================================

-- 1. DOCTORS (의사 6명) - 실제 의사 같은 이름과 6개 진료과
INSERT IGNORE INTO users (id, user_id, password, name, role, height, weight, gender, birth_date, avatar, department, current_points)
VALUES
    (101, 'doctor01', '$2a$12$xuPvMellTFfVLkWHAgj8uOK06sbIYpnB1jHdsokSfsbvEYn6yz2GS', '이준형', 'DOCTOR', 175.0, 70.0, 'M', '1980-01-15', NULL, '신경정신과', 0),
    (102, 'doctor02', '$2a$12$xuPvMellTFfVLkWHAgj8uOK06sbIYpnB1jHdsokSfsbvEYn6yz2GS', '김서연', 'DOCTOR', 163.0, 52.0, 'F', '1982-03-20', NULL, '내과', 0),
    (103, 'doctor03', '$2a$12$xuPvMellTFfVLkWHAgj8uOK06sbIYpnB1jHdsokSfsbvEYn6yz2GS', '박민재', 'DOCTOR', 180.0, 78.0, 'M', '1979-11-12', NULL, '외과', 0),
    (104, 'doctor04', '$2a$12$xuPvMellTFfVLkWHAgj8uOK06sbIYpnB1jHdsokSfsbvEYn6yz2GS', '최지수', 'DOCTOR', 160.0, 48.0, 'F', '1985-07-07', NULL, '피부과', 0),
    (105, 'doctor05', '$2a$12$xuPvMellTFfVLkWHAgj8uOK06sbIYpnB1jHdsokSfsbvEYn6yz2GS', '정성우', 'DOCTOR', 178.0, 74.0, 'M', '1981-05-05', NULL, '흉부외과', 0),
    (106, 'doctor06', '$2a$12$xuPvMellTFfVLkWHAgj8uOK06sbIYpnB1jHdsokSfsbvEYn6yz2GS', '강유진', 'DOCTOR', 165.0, 50.0, 'F', '1983-09-09', NULL, '산부인과', 0);

-- 2. USERS (일반 유저 9명) - 실제 직장인 같은 이름과 부서
INSERT IGNORE INTO users (id, user_id, password, name, role, height, weight, gender, birth_date, avatar, department, current_points)
VALUES
    (201, 'user01', '$2a$12$xuPvMellTFfVLkWHAgj8uOK06sbIYpnB1jHdsokSfsbvEYn6yz2GS', '한동훈', 'MEMBER', 178.0, 75.0, 'M', '1995-02-10', NULL, '개발팀', 0),
    (202, 'user02', '$2a$12$xuPvMellTFfVLkWHAgj8uOK06sbIYpnB1jHdsokSfsbvEYn6yz2GS', '송아영', 'MEMBER', 162.0, 50.0, 'F', '1996-05-15', NULL, '마케팅팀', 0),
    (203, 'user03', '$2a$12$xuPvMellTFfVLkWHAgj8uOK06sbIYpnB1jHdsokSfsbvEYn6yz2GS', '윤재석', 'MEMBER', 170.0, 80.0, 'M', '1990-08-22', NULL, '인사팀', 0),
    (204, 'user04', '$2a$12$xuPvMellTFfVLkWHAgj8uOK06sbIYpnB1jHdsokSfsbvEYn6yz2GS', '임현주', 'MEMBER', 165.0, 55.0, 'F', '1992-12-01', NULL, '경영지원팀', 0),
    (205, 'user05', '$2a$12$xuPvMellTFfVLkWHAgj8uOK06sbIYpnB1jHdsokSfsbvEYn6yz2GS', '류승민', 'MEMBER', 182.0, 72.0, 'M', '1999-01-01', NULL, '영업팀', 0),
    (206, 'user06', '$2a$12$xuPvMellTFfVLkWHAgj8uOK06sbIYpnB1jHdsokSfsbvEYn6yz2GS', '신혜원', 'MEMBER', 158.0, 45.0, 'F', '1997-07-14', NULL, '디자인팀', 0),
    (207, 'user07', '$2a$12$xuPvMellTFfVLkWHAgj8uOK06sbIYpnB1jHdsokSfsbvEYn6yz2GS', '고영태', 'MEMBER', 174.0, 68.0, 'M', '1993-04-30', NULL, '기획팀', 0),
    (208, 'user08', '$2a$12$xuPvMellTFfVLkWHAgj8uOK06sbIYpnB1jHdsokSfsbvEYn6yz2GS', '안정호', 'MEMBER', 180.0, 85.0, 'M', '1991-11-11', NULL, '보안팀', 0),
    (209, 'user09', '$2a$12$xuPvMellTFfVLkWHAgj8uOK06sbIYpnB1jHdsokSfsbvEYn6yz2GS', '배수진', 'MEMBER', 169.0, 60.0, 'F', '1994-09-09', NULL, '재무팀', 0);

-- 3. DOCTOR_PROFILES (의사 상세 정보 매핑)
INSERT IGNORE INTO doctor_profiles (user_id, specialty, license_number, medical_experience, phone, birth_date, gender, address, created_at)
VALUES
    (101, '신경정신과', 'DOC-2024-101', 15, '010-1111-1111', '1980-01-15', '남성', '서울시 강남구', NOW()),
    (102, '내과',       'DOC-2024-102', 12, '010-2222-2222', '1982-03-20', '여성', '서울시 서초구', NOW()),
    (103, '외과',       'DOC-2024-103', 18, '010-3333-3333', '1979-11-12', '남성', '서울시 송파구', NOW()),
    (104, '피부과',     'DOC-2024-104', 8,  '010-4444-4444', '1985-07-07', '여성', '서울시 강동구', NOW()),
    (105, '흉부외과',   'DOC-2024-105', 20, '010-5555-5555', '1981-05-05', '남성', '서울시 마포구', NOW()),
    (106, '산부인과',   'DOC-2024-106', 10, '010-6666-6666', '1983-09-09', '여성', '서울시 용산구', NOW());