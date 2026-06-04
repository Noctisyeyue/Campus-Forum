package com.campus.forum.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.campus.forum.entity.vo.response.WeatherVO;
import com.campus.forum.service.WeatherService;
import com.campus.forum.utils.Const;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

/**
 * 天气服务实现，调用和风天气 API（QWeather）获取实时天气和逐时预报
 */
@Slf4j
@Service
public class WeatherServiceImpl implements WeatherService {

    /** HTTP 请求客户端 */
    @Resource
    RestTemplate rest;

    /** Redis 操作模板，用于天气数据缓存 */
    @Resource
    StringRedisTemplate template;

    /** 和风天气 API Key（从 application.yml 读取） */
    @Value("${spring.weather.key}")
    String key;

    /** 和风天气 API Host（开发者专属域名） */
    @Value("${spring.weather.host}")
    String host;

    /**
     * 根据经纬度获取天气信息（优先从 Redis 缓存读取）
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @return 天气视图对象，失败返回 null（由 Controller 层静默降级）
     */
    @Override
    public WeatherVO fetchWeather(double longitude, double latitude) {
        return fetchFromCache(longitude, latitude);
    }

    /**
     * 从缓存获取天气数据，缓存未命中则调用 API 并写入缓存（有效期 1 小时）
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @return 天气视图对象，失败返回 null
     */
    private WeatherVO fetchFromCache(double longitude, double latitude) {
        // 1. 通过 GeoAPI 将经纬度转换为城市 ID
        JSONObject geo = this.fetchJson(
                "https://" + host + "/geo/v2/city/lookup?location=" + longitude + "," + latitude);
        if (geo == null) return null;
        // 获取数据（包含城市相关）
        JSONArray locations = geo.getJSONArray("location");
        if (locations == null || locations.isEmpty()) return null;
         // 取第一个城市
        JSONObject location = locations.getJSONObject(0);
        String id = location.getString("id");  // 拿到 "101031100"
        // 2. 尝试从 Redis 缓存读取
        String cacheKey = Const.FORUM_WEATHER_CACHE + id;
        String cache = template.opsForValue().get(cacheKey);
        if (cache != null)
            return JSONObject.parseObject(cache).to(WeatherVO.class);   // JSON字符串 → 对象，直接返回
        // 3. 缓存未命中，调用 API 获取
        WeatherVO vo = this.fetchFromAPI(id, location);
        if (vo == null) return null;
        // 4. 写入缓存，1 小时过期
        template.opsForValue().set(cacheKey, JSONObject.from(vo).toJSONString(), 1, TimeUnit.HOURS);
        return vo;
    }

    /**
     * 调用和风天气 API 获取实时天气和 24 小时预报（取前 5 条）
     *
     * @param id       城市 ID（由 GeoAPI 查询获得）
     * @param location 城市位置信息 JSON
     * @return 天气视图对象，失败返回 null
     */
    private WeatherVO fetchFromAPI(String id, JSONObject location) {
        // 创建一个空的天气对象
        WeatherVO vo = new WeatherVO();
        // 把城市信息放进去（名称、经纬度等，来自 GeoAPI 的结果）
        vo.setLocation(location);
        // 获取实时天气
        JSONObject now = this.fetchJson(
                "https://" + host + "/v7/weather/now?location=" + id);
        if (now == null) return null;
        vo.setNow(now.getJSONObject("now"));
        // 获取 24 小时逐时预报
        JSONObject hourly = this.fetchJson(
                "https://" + host + "/v7/weather/24h?location=" + id);
        if (hourly == null) return null;
        JSONArray hourlyData = hourly.getJSONArray("hourly");
        if (hourlyData == null || hourlyData.isEmpty()) return null;
        // 只保留前 5 条（页面只展示 5 个小时）
        vo.setHourly(new JSONArray(hourlyData.stream().limit(5).toList()));
        return vo;
    }

    /**
     * 发送 HTTP GET 请求并将 GZIP 响应解压为 JSON
     *
     * @param url 请求地址
     * @return 解析后的 JSON 对象，失败返回 null
     */
    private JSONObject fetchJson(String url) {
        try {
            // 通过请求头传递 API Key
            // 1. 创建一个空的请求头对象
            HttpHeaders headers = new HttpHeaders();
            // 2. 添加一个自定义请求头
            headers.set("X-QW-Api-Key", key);
            // 3. 把请求头包装成 HttpEntity（= 请求体 + 请求头）
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            return this.decompressStingToJson(rest.exchange(url, HttpMethod.GET, entity, byte[].class).getBody());
                                            //   ↑           ↑      ↑              ↑        ↑              ↑
                                            //   请求客户端  URL    GET方法     带头的请求  期望返回byte[]   取出响应体
        } catch (RestClientException e) {
            log.warn("天气API请求失败，静默降级: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 将 GZIP 压缩的字节数组解压并解析为 JSON
     * 和风天气 API 默认返回 GZIP 压缩数据
     *
     * @param data GZIP 压缩的字节数组
     * @return 解析后的 JSON 对象，失败返回 null
     */
    private JSONObject decompressStingToJson(byte[] data) {
        // 空数据直接返回
        if (data == null || data.length == 0) return null;
        // 准备一个输出流，用来存解压后的数据
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            // 把压缩的字节数组 → 包装成输入流 → 再包装成 GZIP 解压流
            GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data));
            // 用 1024 字节的缓冲区，循环读取解压后的数据
            byte[] buffer = new byte[1024];
            int read;
            while ((read = gzip.read(buffer)) != -1) // 每次读最多1024字节，读到末尾返回-1
                stream.write(buffer, 0, read);  // 把读到的写入输出流
            gzip.close();
            stream.close();
             // 输出流的内容 → 转字符串 → 解析为 JSON 对象
            return JSONObject.parseObject(stream.toString());
        } catch (IOException e) {
            log.warn("天气API请求失败，静默降级: {}", e.getMessage());
            return null;
        }
    }
}