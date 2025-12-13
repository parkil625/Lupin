package com.example.demo.util;

/**
 * Redis 키 생성 유틸리티
 * 키 충돌 방지 및 관리를 위해 접두어를 사용
 */
public final class RedisKeyUtils {

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String FEED_LIKE_COUNT_PREFIX = "feed:like:";
    private static final String FEED_LIKE_DIRTY_SET = "feed:like:dirty";

    private RedisKeyUtils() {
    }

    /**
     * Refresh Token 저장용 키 생성
     * @param userId 사용자 ID
     * @return "refresh:{userId}" 형식의 키
     */
    public static String refreshToken(String userId) {
        return REFRESH_TOKEN_PREFIX + userId;
    }

    /**
     * Access Token 블랙리스트 키 생성
     * @param accessToken 블랙리스트에 추가할 액세스 토큰
     * @return "blacklist:{accessToken}" 형식의 키
     */
    public static String blacklist(String accessToken) {
        return BLACKLIST_PREFIX + accessToken;
    }

    /**
     * 피드 좋아요 카운트 캐시 키 생성
     * @param feedId 피드 ID
     * @return "feed:like:{feedId}" 형식의 키
     */
    public static String feedLikeCount(Long feedId) {
        return FEED_LIKE_COUNT_PREFIX + feedId;
    }

    /**
     * 피드 좋아요 Dirty Set 키 반환
     * DB 동기화가 필요한 피드 ID들을 저장하는 Set
     * @return "feed:like:dirty" 키
     */
    public static String feedLikeDirtySet() {
        return FEED_LIKE_DIRTY_SET;
    }
}
