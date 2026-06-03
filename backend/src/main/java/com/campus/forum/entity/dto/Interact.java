package com.campus.forum.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

/**
 * 互动记录（点赞/收藏），不直接映射数据库表
 */
@Data
@AllArgsConstructor
public class Interact {
    Integer tid;            // 帖子ID
    Integer uid;            // 用户ID
    Date time;              // 互动时间
    String type;            // 互动类型（like/collect）

    /**
     * 生成 tid:uid 格式的 Key
     *
     * @return Redis Hash 的 field key
     */
    public String toKey() {
        return tid + ":" + uid;
    }

    /**
     * 从 tid:uid 格式字符串解析为 Interact 对象
     *
     * @param str  tid:uid 格式字符串
     * @param type 互动类型（like/collect）
     * @return 解析后的 Interact 对象
     */
    public static Interact parseInteract(String str, String type) {
        String[] keys = str.split(":");
        return new Interact(Integer.parseInt(keys[0]), Integer.parseInt(keys[1]), new Date(), type);
    }
}
