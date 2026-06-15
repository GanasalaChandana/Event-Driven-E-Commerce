package com.shopflow.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Redis cache for available stock levels.
 * Key: inventory:stock:{productId}   Value: available quantity (string)
 * TTL: 10 minutes — cache miss falls back to DB.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockCacheService {

    private final StringRedisTemplate redisTemplate;

    @Value("${inventory.stock.cache-ttl-seconds:600}")
    private long cacheTtlSeconds;

    private static final String KEY_PREFIX = "inventory:stock:";

    public void setStock(UUID productId, int availableQty) {
        String key = KEY_PREFIX + productId;
        redisTemplate.opsForValue().set(key, String.valueOf(availableQty), Duration.ofSeconds(cacheTtlSeconds));
        log.debug("Cache SET {} = {}", key, availableQty);
    }

    public Integer getStock(UUID productId) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + productId);
        if (value == null) return null;
        return Integer.parseInt(value);
    }

    public void evict(UUID productId) {
        redisTemplate.delete(KEY_PREFIX + productId);
    }
}
