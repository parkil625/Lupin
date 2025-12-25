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

-- 4. medicines (약, 효능, 주의 사항)
INSERT IGNORE INTO medicines (code, name, description, precautions) VALUES
-- [신경정신과: 우울증, 불안, 불면증]
('M001', '렉사프로정 10mg', 'SSRI 계열 우울증 및 불안장애 치료제', '갑작스러운 중단 금기, 초기 구역질 주의'),
('M002', '자나팜정 0.25mg', '불안장애, 공황장애, 우울증에 수반하는 불안', '졸음 유발 가능, 알코올 병용 금지, 운전 주의'),
('M003', '스틸녹스정 10mg', '비벤조디아제핀계 수면유도제', '취침 직전 복용, 4주 이상 장기 연용 주의'),
('M004', '아빌리파이정 5mg', '조현병 및 양극성 장애 치료제', '혈당 수치 변화 주의, 체중 증가 가능성'),

-- [내과: 고혈압, 당뇨, 위장관, 고지혈증]
('M005', '코자정 50mg', '안지오텐신Ⅱ 수용체 차단 고혈압약', '임산부 금기, 기립성 저혈압 주의'),
('M006', '다이아벡스정 500mg', '메트포르민 성분의 당뇨병 치료제', '식사와 함께 또는 식후 즉시 복용, 신장 기능 확인'),
('M007', '넥시움정 20mg', '역류성 식도염 및 위궤양 치료제(PPI)', '아침 식전 30분 복용 권장'),
('M008', '리피토정 10mg', '고콜레스테롤혈증 치료제(스타틴)', '저녁 식후 복용 권장, 근육통 발생 시 의사 상담'),

-- [외과: 진통제, 소염제, 항생제]
('M009', '트리돌캡슐 50mg', '중등도 및 심한 통증 조절(트라마돌)', '어지러움 및 졸음 주의, 변비 발생 가능'),
('M010', '세레브렉스캡슐 200mg', '골관절염 및 류마티스 관절염 소염진통제', '심혈관 질환자 주의, 위장장애 발생 시 식후 복용'),
('M011', '오그멘틴정 625mg', '페니실린계 복합 항생제', '페니실린 알레르기 환자 금기, 설사 유발 가능'),
('M012', '케토톱플라스타', '관절염 및 근육통 소염진통 파스', '부착 부위 발진 시 사용 중단, 광과민성 주의'),

-- [피부과: 알레르기, 여드름, 무좀]
('M013', '씨잘정 5mg', '알레르기 비염 및 두드러기 치료제', '졸음이 적으나 개인차 있음, 신장애 환자 용량 조절'),
('M014', '로아큐탄캡슐 10mg', '중증 여드름 치료제(이소트레티노인)', '임신 절대 금기(기형 유발), 입술 및 피부 건조 심함'),
('M015', '스포라녹스캡슐 100mg', '손발톱 무좀 및 진균 감염 치료제', '간 기능 검사 필요, 특정 약물과 병용 금기'),
('M016', '더모베이트연고', '강력한 스테로이드 외용제', '얼굴 사용 주의, 장기간 넓은 부위 사용 금지'),

-- [흉부외과: 협심증, 혈전방지]
('M017', '아스피린프로텍트정 100mg', '혈전 생성 억제 및 심혈관 질환 예방', '위출혈 위험 주의, 수술 전 복용 중단 필요'),
('M018', '니트로글리세린 설하정', '협심증 발작 시 응급 치료제', '혀 밑에 녹여 복용, 앉아서 복용(어지러움 주의)'),
('M019', '쿠마딘정 5mg', '와파린 성분의 항응고제', '비타민K(녹색 채소) 섭취량 일정하게 유지, 출혈 주의'),
('M020', '콩코르정 5mg', '심부전 및 고혈압 치료(베타차단제)', '천식 환자 주의, 갑작스러운 중단 시 반동 현상'),

-- [산부인과: 호르몬, 임신부 영양제, 질염]
('M021', '야즈정', '경구 피임약 및 월경전 불쾌장애 치료', '매일 같은 시간 복용, 흡연자(특히 35세 이상) 금기'),
('M022', '훼로바유서방정', '철 결핍성 빈혈 치료제', '변비 및 흑변 가능성, 공복 복용 권장(위장장애 시 식후)'),
('M023', '폴산정 1mg', '임신부 엽산 보충제', '임신 준비 기간부터 복용 권장'),
('M024', '카네스텐질정', '칸디다성 질염 치료제', '취침 전 질 내 깊숙이 삽입, 생리 기간 피해서 사용');