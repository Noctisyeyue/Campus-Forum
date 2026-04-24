package com.campus.forum.entity.vo.response;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

/**
 * 天气信息响应
 */
@Data
public class WeatherVO {
    JSONObject location;    // 位置信息
    JSONObject now;         // 当前天气
    JSONArray hourly;       // 逐小时预报
}
