package com.shoppilot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    private static final String TOKEN_KEY_PREFIX = "login:token:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<String, LocalToken> localTokens = new ConcurrentHashMap<>();

    public TokenService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveToken(String token, Long userId, long expirationSeconds) {
        try {
            redisTemplate.opsForValue().set(buildKey(token), userId, Duration.ofSeconds(expirationSeconds));
        } catch (RedisConnectionFailureException exception) {
            log.warn("Redis 未连接，登录 token 临时保存到本地内存。请启动 Redis 以获得完整会话能力。");
            localTokens.put(token, new LocalToken(userId, Instant.now().plusSeconds(expirationSeconds)));
        }
    }

    public boolean isTokenValid(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(token)));
        } catch (RedisConnectionFailureException exception) {
            LocalToken localToken = localTokens.get(token);
            if (localToken == null) {
                return false;
            }
            if (localToken.expiresAt().isBefore(Instant.now())) {
                localTokens.remove(token);
                return false;
            }
            return true;
        }
    }

    public void deleteToken(String token) {
        try {
            redisTemplate.delete(buildKey(token));
        } catch (RedisConnectionFailureException exception) {
            log.warn("Redis 未连接，从本地内存删除登录 token。");
        }
        localTokens.remove(token);
    }

    private String buildKey(String token) {
        return TOKEN_KEY_PREFIX + token;
    }

    private record LocalToken(Long userId, Instant expiresAt) {
    }
}
