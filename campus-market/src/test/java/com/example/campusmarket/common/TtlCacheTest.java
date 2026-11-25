package com.example.campusmarket.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 热点接口缓存的基础行为：未过期命中、过期失效、clear 立即失效。
 */
class TtlCacheTest {

    @Test
    void returnsValueBeforeExpiry() {
        TtlCache<String> cache = new TtlCache<>(10_000L, 2);
        cache.put("k", "v");
        assertEquals("v", cache.get("k"));
    }

    @Test
    void expiresAfterTtl() throws InterruptedException {
        TtlCache<String> cache = new TtlCache<>(30L, 2);
        cache.put("k", "v");
        Thread.sleep(60L);
        assertNull(cache.get("k"));
    }

    @Test
    void clearDropsAllEntries() {
        TtlCache<String> cache = new TtlCache<>(10_000L, 2);
        cache.put("k", "v");
        cache.clear();
        assertNull(cache.get("k"));
    }

    @Test
    void evictsWhenOverMaxSize() {
        TtlCache<String> cache = new TtlCache<>(10_000L, 2);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");
        assertNull(cache.get("a"));
        assertEquals("3", cache.get("c"));
    }
}
