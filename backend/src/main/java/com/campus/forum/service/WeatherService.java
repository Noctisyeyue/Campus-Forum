package com.campus.forum.service;

import com.campus.forum.entity.vo.response.WeatherVO;

/**
 * 天气服务
 */
public interface WeatherService {
    WeatherVO fetchWeather(double longitude, double latitude);
}
