-- 1. User (사용자)
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(10) NOT NULL,
    height DOUBLE,
    weight DOUBLE,
    gender VARCHAR(10),
    birth_date DATE,
    current_points BIGINT NOT NULL DEFAULT 0,
    monthly_points BIGINT NOT NULL DEFAULT 0,
    monthly_likes BIGINT NOT NULL DEFAULT 0,
    department VARCHAR(100),
    avatar VARCHAR(500),
    last_visit DATETIME,
    `condition` VARCHAR(255),
    status VARCHAR(20) DEFAULT 'WAITING',
    version BIGINT,

    INDEX idx_user_email (email),
    INDEX idx_user_monthly_points (monthly_points DESC),
    INDEX idx_user_monthly_likes (monthly_likes DESC)
);

-- 2. UserOAuth (소셜 로그인)
CREATE TABLE user_oauth (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    provider_email VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_provider_provider_id (provider, provider_id)
);

-- 3. UserPenalty (사용자 제재)
CREATE TABLE user_penalties (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    penalty_type VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    penalty_count INT NOT NULL DEFAULT 1,
    
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_user_penalty (user_id, penalty_type),
    INDEX idx_penalty_user (user_id),
    INDEX idx_penalty_expires (expires_at)
);

-- 4. DoctorProfile (의사 프로필)
CREATE TABLE doctor_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    specialty VARCHAR(100),
    license_number VARCHAR(50),
    medical_experience INT,
    phone VARCHAR(20),
    birth_date DATE,
    gender VARCHAR(10),
    address VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_doctor_profile_user (user_id)
);

-- 5. Feed (피드)
CREATE TABLE feeds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    writer_id BIGINT NOT NULL,
    activity VARCHAR(50) NOT NULL,
    duration VARCHAR(50),
    calories INT,
    stats TEXT,
    content TEXT,
    points BIGINT NOT NULL DEFAULT 0,
    likes_count INT NOT NULL DEFAULT 0,
    comments_count INT NOT NULL DEFAULT 0,
    version BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    
    FOREIGN KEY (writer_id) REFERENCES users(id),
    INDEX idx_feed_writer (writer_id),
    INDEX idx_feed_created (created_at DESC),
    INDEX idx_feed_activity (activity)
);

-- 6. FeedImage (피드 이미지)
CREATE TABLE feed_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    feed_id BIGINT NOT NULL,
    s3_key TEXT NOT NULL,
    img_type VARCHAR(10) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    
    FOREIGN KEY (feed_id) REFERENCES feeds(id),
    INDEX idx_feed_image_feed (feed_id)
);

-- 7. FeedLike (피드 좋아요)
CREATE TABLE feed_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    feed_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (feed_id) REFERENCES feeds(id),
    UNIQUE KEY uk_feed_like_user_feed (user_id, feed_id),
    INDEX idx_feed_like_feed (feed_id),
    INDEX idx_feed_like_user (user_id)
);

-- 8. Comment (댓글)
CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    writer_id BIGINT NOT NULL,
    feed_id BIGINT NOT NULL,
    parent_id BIGINT,
    content TEXT NOT NULL,
    likes_count INT NOT NULL DEFAULT 0,
    replies_count INT NOT NULL DEFAULT 0,
    version BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    
    FOREIGN KEY (writer_id) REFERENCES users(id),
    FOREIGN KEY (feed_id) REFERENCES feeds(id),
    FOREIGN KEY (parent_id) REFERENCES comments(id),
    INDEX idx_comment_feed (feed_id),
    INDEX idx_comment_writer (writer_id),
    INDEX idx_comment_parent (parent_id),
    INDEX idx_comment_created (created_at DESC)
);

-- 9. CommentLike (댓글 좋아요)
CREATE TABLE comment_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    comment_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (comment_id) REFERENCES comments(id),
    UNIQUE KEY uk_comment_like_user_comment (user_id, comment_id),
    INDEX idx_comment_like_comment (comment_id),
    INDEX idx_comment_like_user (user_id)
);

-- 10. Notification (알림)
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    ref_id VARCHAR(255),
    created_at DATETIME NOT NULL,
    
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_notification_user (user_id),
    INDEX idx_notification_user_read (user_id, is_read),
    INDEX idx_notification_created (created_at DESC),
    INDEX idx_notification_type (type)
);

-- 11. Report (신고)
CREATE TABLE reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    
    FOREIGN KEY (reporter_id) REFERENCES users(id),
    UNIQUE KEY uk_report_target_reporter (target_type, target_id, reporter_id),
    INDEX idx_report_target (target_type, target_id),
    INDEX idx_report_reporter (reporter_id)
);

-- 12. Outbox (이벤트 발행)
CREATE TABLE outbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    processed_at DATETIME,
    error_message VARCHAR(1000),
    
    INDEX idx_outbox_status (status),
    INDEX idx_outbox_created (created_at),
    INDEX idx_outbox_aggregate (aggregate_type, aggregate_id)
);

-- 13. Auction (경매)
CREATE TABLE auctions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    item_image VARCHAR(500),
    current_price BIGINT NOT NULL DEFAULT 0,
    start_time DATETIME NOT NULL,
    regular_end_time DATETIME NOT NULL,
    overtime_started BOOLEAN NOT NULL DEFAULT FALSE,
    overtime_end_time DATETIME,
    overtime_seconds INT NOT NULL DEFAULT 30,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    winner_id BIGINT,
    winning_bid BIGINT,
    total_bids INT NOT NULL DEFAULT 0,
    version BIGINT,
    
    INDEX idx_auction_status (status),
    INDEX idx_auction_start_time (start_time),
    INDEX idx_auction_end_time (regular_end_time)
);

-- 14. AuctionBid (경매 입찰)
CREATE TABLE auction_bids (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    auction_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    bid_amount BIGINT NOT NULL,
    bid_time DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT,
    
    INDEX idx_bid_auction (auction_id),
    INDEX idx_bid_user (user_id),
    INDEX idx_bid_status (status),
    INDEX idx_bid_time (bid_time),
    INDEX idx_bid_auction_user (auction_id, user_id)
);

-- 15. ChatMessage (채팅 메시지)
CREATE TABLE chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id VARCHAR(100) NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    time DATETIME NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    
    FOREIGN KEY (sender_id) REFERENCES users(id),
    INDEX idx_chat_room (room_id),
    INDEX idx_chat_room_sent (room_id, time DESC),
    INDEX idx_chat_sender (sender_id),
    INDEX idx_chat_unread (room_id, is_read)
);

-- 16. Appointment (예약)
CREATE TABLE appointments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    date DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    reason TEXT,
    version BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    
    FOREIGN KEY (patient_id) REFERENCES users(id),
    FOREIGN KEY (doctor_id) REFERENCES users(id),
    INDEX idx_appointment_patient (patient_id),
    INDEX idx_appointment_doctor (doctor_id),
    INDEX idx_appointment_date (date),
    INDEX idx_appointment_status (status)
);

-- 17. Prescription (처방전)
CREATE TABLE prescriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    name VARCHAR(255),
    diagnosis TEXT,
    instructions TEXT,
    date DATE NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    
    FOREIGN KEY (patient_id) REFERENCES users(id),
    FOREIGN KEY (doctor_id) REFERENCES users(id),
    INDEX idx_prescription_patient (patient_id),
    INDEX idx_prescription_doctor (doctor_id),
    INDEX idx_prescription_date (date DESC)
);

-- 18. PrescriptionMed (처방 약품)
CREATE TABLE prescription_meds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prescription_id BIGINT NOT NULL,
    medicine_name VARCHAR(255) NOT NULL,
    dosage VARCHAR(100),
    frequency VARCHAR(255),
    
    FOREIGN KEY (prescription_id) REFERENCES prescriptions(id),
    INDEX idx_prescription_med_prescription (prescription_id)
);