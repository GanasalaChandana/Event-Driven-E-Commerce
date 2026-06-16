package com.shopflow.inventory.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class StockCacheService {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Value("${inventory.stock.cache-ttl-seconds:600}")
    private long cacheTtlSeconds;

    private static final String KEY_PREFIX = "inventory:stock:";

    public void setStock(UUID productId, int availableQty) {
        if (redisTemplate == null) return;
        String key = KEY_PREFIX + productId;
        redisTemplate.opsForValue().set(key, String.valueOf(availableQty), Duration.ofSeconds(cacheTtlSeconds));
        log.debug("Cache SET {} = {}", key, availableQty);
    }

    public Integer getStock(UUID productId) {
        if (redisTemplate == null) return null;
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + productId);
        if (value == null) return null;
        return Integer.parseInt(value);
    }

    public void evict(UUID productId) {
        if (redisTemplate == null) return;
        redisTemplate.delete(KEY_PREFIX + productId);
    }
}
