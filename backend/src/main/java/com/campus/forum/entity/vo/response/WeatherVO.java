package com.campus.forum.entity.vo.response;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

/**
 * 天气信息响应，数据来源于和风天气 API
 */
@Data
public class WeatherVO {

    /**
     * 城市位置信息，示例字段：
     * name(城市名), id(LocationID), lat(纬度), lon(经度),
     * adm2(上级行政区), adm1(所属省份), country(国家)
     */
    JSONObject location;

    /**
     * 当前实时天气，示例字段：
     * temp(温度℃), text(天气描述), icon(图标代码),
     * feelsLike(体感温度), humidity(湿度%), windDir(风向),
     * windScale(风力等级), vis(能见度km), pressure(气压hPa)
     */
    JSONObject now;

    /**
     * 逐小时预报（最多5条），每条示例字段：
     * fxTime(预报时间), temp(温度℃), text(天气描述), icon(图标代码),
     * humidity(湿度%), pop(降水概率%), windDir(风向), windScale(风力)
     */
    JSONArray hourly;
}
