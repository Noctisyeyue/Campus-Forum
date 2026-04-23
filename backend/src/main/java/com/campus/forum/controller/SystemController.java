package com.campus.forum.controller;

import com.campus.forum.mapper.TopicTypeMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final TopicTypeMapper topicTypeMapper;

    public SystemController(TopicTypeMapper topicTypeMapper) {
        this.topicTypeMapper = topicTypeMapper;
    }

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("project", "Campus Forum Backend");
        result.put("status", "ok");
        result.put("topicTypeCount", topicTypeMapper.selectCount(null));
        return result;
    }
}
