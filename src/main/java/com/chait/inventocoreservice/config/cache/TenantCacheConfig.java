package com.chait.inventocoreservice.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class TenantCacheConfig {

    public static final String CACHE_NAME = "tenants";
    static final long   L1_TTL_SECONDS = 60;
    static final long   L2_TTL_MINUTES = 5;
    static final int    L1_MAX_ENTRIES = 500;

    @Bean
    public CaffeineCacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(CACHE_NAME);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(L1_TTL_SECONDS, TimeUnit.SECONDS)
                .maximumSize(L1_MAX_ENTRIES)
                .recordStats());
        return manager;
    }

    @Bean
    public CacheManager redissonCacheManager(RedissonClient redissonClient) {
        return new RedissonSpringCacheManager(
                redissonClient,
                Map.of(CACHE_NAME, new CacheConfig(
                        TimeUnit.MINUTES.toMillis(L2_TTL_MINUTES), // TTL
                        TimeUnit.MINUTES.toMillis(L2_TTL_MINUTES)  // max idle
                ))
        );
    }

    @Primary
    @Bean
    public CacheManager cacheManager(
            CaffeineCacheManager caffeineCacheManager,
            CacheManager redissonCacheManager) {

        CompositeCacheManager composite = new CompositeCacheManager(
                caffeineCacheManager,
                redissonCacheManager
        );
        composite.setFallbackToNoOpCache(false);
        return composite;
    }
}
