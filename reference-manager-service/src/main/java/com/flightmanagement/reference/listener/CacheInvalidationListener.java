package com.flightmanagement.reference.listener;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationListener {

    private final CacheManager cacheManager;

    @EventListener
    public void handleCacheInvalidation(CacheInvalidationEvent event) {
        try {
            var cache = cacheManager.getCache(event.getCacheName());
            if (cache != null) {
                if (event.getKey() != null) {
                    cache.evict(event.getKey());
                    log.debug("Evicted cache entry: {}:{}", event.getCacheName(), event.getKey());
                } else {
                    cache.clear();
                    log.debug("Cleared entire cache: {}", event.getCacheName());
                }
            }
        } catch (Exception e) {
            log.error("Failed to invalidate cache", e);
        }
    }

    @Getter
    public static class CacheInvalidationEvent {
        private final String cacheName;
        private final Object key;

        public CacheInvalidationEvent(String cacheName, Object key) {
            this.cacheName = cacheName;
            this.key = key;
        }

    }
}