package com.campus.forum.utils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存工具类，封装对象/列表的序列化缓存读写
 */
@Component
public class CacheUtils {

    @Resource
    StringRedisTemplate template;

    /**
     * 从缓存读取单个对象
     *
     * @param key      缓存 Key
     * @param dataType 目标类型
     * @return 缓存对象，Key 不存在返回 null
     */
    public <T> T takeFromCache(String key, Class<T> dataType) {
        String s = template.opsForValue().get(key);
        if (s == null) return null;
        return JSONObject.parseObject(s).to(dataType);
    }

    /**
     * 从缓存读取列表
     *
     * @param key      缓存 Key
     * @param itemType 列表元素类型
     * @return 缓存列表，Key 不存在返回 null
     */
    public <T> List<T> takeListFromCache(String key, Class<T> itemType) {
        String s = template.opsForValue().get(key);
        if (s == null) return null;
        return JSONArray.parseArray(s).toList(itemType);
    }

    /**
     * 将对象写入缓存
     *
     * @param key    缓存 Key
     * @param data   要缓存的对象
     * @param expire 过期时间（秒）
     */
    public <T> void saveToCache(String key, T data, long expire) {
        template.opsForValue().set(key, JSONObject.from(data).toJSONString(), expire, TimeUnit.SECONDS);
    }

    /**
     * 将列表写入缓存
     *
     * @param key    缓存 Key
     * @param list   要缓存的列表
     * @param expire 过期时间（秒）
     */
    public <T> void saveListToCache(String key, List<T> list, long expire) {
        template.opsForValue().set(key, JSONArray.from(list).toJSONString(), expire, TimeUnit.SECONDS);
    }

    /**
     * 按通配符模式批量删除缓存
     *
     * @param key 支持 * 通配符的 Key 模式
     */
    public void deleteCachePattern(String key) {
        Set<String> keys = Optional.ofNullable(template.keys(key)).orElse(Collections.emptySet());
        template.delete(keys);
    }

    /**
     * 删除指定 Key 的缓存
     *
     * @param key 精确的缓存 Key
     */
    public void deleteCache(String key) {
        template.delete(key);
    }
}
