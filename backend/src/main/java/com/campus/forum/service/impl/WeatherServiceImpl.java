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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

/**
 * 天气服务实现，调用和风天气 API
 * <p>
 * 失败兜底策略：
 * - API 请求超时或报错时返回 null，由 Controller 层返回"天气服务暂时不可用"
 * - 不弹窗报错，不白屏，静默降级
 * </p>
 */
@Slf4j
@Service
public class WeatherServiceImpl implements WeatherService {

    @Resource
    RestTemplate rest;

    @Resource
    StringRedisTemplate template;

    @Value("${spring.weather.key}")
    String key;

    public WeatherVO fetchWeather(double longitude, double latitude) {
        return fetchFromCache(longitude, latitude);
    }

    private WeatherVO fetchFromCache(double longitude, double latitude) {
        JSONObject geo = this.fetchJson(
                "https://geoapi.qweather.com/v2/city/lookup?location=" + longitude + "," + latitude + "&key=" + key);
        if (geo == null) return null;
        JSONArray locations = geo.getJSONArray("location");
        if (locations == null || locations.isEmpty()) return null;
        JSONObject location = locations.getJSONObject(0);
        int id = location.getInteger("id");
        String cacheKey = Const.FORUM_WEATHER_CACHE + id;
        String cache = template.opsForValue().get(cacheKey);
        if (cache != null)
            return JSONObject.parseObject(cache).to(WeatherVO.class);
        WeatherVO vo = this.fetchFromAPI(id, location);
        if (vo == null) return null;
        template.opsForValue().set(cacheKey, JSONObject.from(vo).toJSONString(), 1, TimeUnit.HOURS);
        return vo;
    }

    private WeatherVO fetchFromAPI(int id, JSONObject location) {
        WeatherVO vo = new WeatherVO();
        vo.setLocation(location);
        JSONObject now = this.fetchJson(
                "https://devapi.qweather.com/v7/weather/now?location=" + id + "&key=" + key);
        if (now == null) return null;
        vo.setNow(now.getJSONObject("now"));
        JSONObject hourly = this.fetchJson(
                "https://devapi.qweather.com/v7/weather/24h?location=" + id + "&key=" + key);
        if (hourly == null) return null;
        JSONArray hourlyData = hourly.getJSONArray("hourly");
        if (hourlyData == null || hourlyData.isEmpty()) return null;
        vo.setHourly(new JSONArray(hourlyData.stream().limit(5).toList()));
        return vo;
    }

    private JSONObject fetchJson(String url) {
        try {
            return this.decompressStingToJson(rest.getForObject(url, byte[].class));
        } catch (RestClientException e) {
            log.warn("天气API请求失败，静默降级: {}", e.getMessage());
            return null;
        }
    }

    // 解压 GZIP 响应并解析为 JSON
    private JSONObject decompressStingToJson(byte[] data) {
        if (data == null || data.length == 0) return null;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data));
            byte[] buffer = new byte[1024];
            int read;
            while ((read = gzip.read(buffer)) != -1)
                stream.write(buffer, 0, read);
            gzip.close();
            stream.close();
            return JSONObject.parseObject(stream.toString());
        } catch (IOException e) {
            log.warn("天气API请求失败，静默降级: {}", e.getMessage());
            return null;
        }
    }
}
