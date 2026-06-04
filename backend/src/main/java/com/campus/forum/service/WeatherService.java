package com.campus.forum.service;

import com.campus.forum.entity.vo.response.WeatherVO;

/**
 * 天气服务
 */
public interface WeatherService {

    /**
     * 根据经纬度获取天气信息
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @return 天气信息
     */
    WeatherVO fetchWeather(double longitude, double latitude);
}
