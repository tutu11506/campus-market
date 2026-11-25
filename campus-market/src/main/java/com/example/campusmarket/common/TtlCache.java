package com.example.campusmarket.common;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 进程内 TTL + LRU 缓存，用于热点只读接口。
 * 单实例部署够用；将来多实例部署或需要跨节点一致时，再换成 Redis。
 */
public class TtlCache<V> {

    private final long ttlMillis;
    private final Map<String, Entry<V>> store;

    public TtlCache(long ttlMillis, int maxSize) {
        this.ttlMillis = ttlMillis;
        this.store = new LinkedHashMap<String, Entry<V>>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, TtlCache.Entry<V>> eldest) {
                return size() > maxSize;
            }
        };
    }

    /** 命中且未过期时返回缓存值，否则返回 null */
    public synchronized V get(String key) {
        Entry<V> entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() - entry.createdAt > ttlMillis) {
            store.remove(key);
            return null;
        }
        return entry.value;
    }

    public synchronized void put(String key, V value) {
        store.put(key, new Entry<>(value, System.currentTimeMillis()));
    }

    /** 数据发生写入后主动清空，避免等 TTL 自然过期 */
    public synchronized void clear() {
        store.clear();
    }

    private static final class Entry<V> {
        private final V value;
        private final long createdAt;

        private Entry(V value, long createdAt) {
            this.value = value;
            this.createdAt = createdAt;
        }
    }
}
