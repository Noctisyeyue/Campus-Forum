package com.campus.forum.controller;

import com.campus.forum.entity.RestBean;
import com.campus.forum.entity.dto.TopicType;
import com.campus.forum.entity.vo.response.TopicTypeVO;
import com.campus.forum.mapper.TopicTypeMapper;
import com.campus.forum.service.TopicService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员分类管理控制器，提供分类增删改查功能
 */
@RestController
@RequestMapping("/api/admin/types")
public class AdminTypeController {

    @Resource
    TopicService topicService;

    @Resource
    TopicTypeMapper topicTypeMapper;

    /**
     * 获取全部分类列表
     * @return 分类列表
     */
    @GetMapping
    public RestBean<List<TopicTypeVO>> listTypes() {
        return RestBean.success(topicService.listTypes()
                .stream()
                .map(type -> type.asViewObject(TopicTypeVO.class))
                .toList());
    }

    /**
     * 新增分类
     * @param name 分类名称
     * @param desc 分类描述
     * @param color 标签颜色
     * @return 新增的分类
     */
    @PostMapping
    public RestBean<TopicTypeVO> createType(@RequestParam String name,
                                             @RequestParam(required = false) String desc,
                                             @RequestParam(required = false) String color) {
        TopicType type = new TopicType();
        type.setName(name);
        type.setDesc(desc);
        type.setColor(color);
        type.setSystemKey(null);
        topicTypeMapper.insert(type);
        return RestBean.success(type.asViewObject(TopicTypeVO.class));
    }

    /**
     * 编辑分类
     * @param id 分类ID
     * @param name 分类名称
     * @param desc 分类描述
     * @param color 标签颜色
     * @return 操作结果
     */
    @PutMapping("/{id}")
    public RestBean<Void> updateType(@PathVariable int id,
                                      @RequestParam String name,
                                      @RequestParam(required = false) String desc,
                                      @RequestParam(required = false) String color) {
        TopicType type = topicTypeMapper.selectById(id);
        if (type == null) return RestBean.failure(404, "分类不存在");
        if (type.getSystemKey() == null || type.getSystemKey().isBlank()) {
            type.setName(name);
            type.setDesc(desc);
        } else if (!type.getName().equals(name) || !java.util.Objects.equals(type.getDesc(), desc)) {
            return RestBean.failure(400, "系统分类只允许修改颜色");
        }
        type.setColor(color);
        topicTypeMapper.updateById(type);
        return RestBean.success();
    }

    /**
     * 删除分类
     * @param id 分类ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public RestBean<Void> deleteType(@PathVariable int id) {
        TopicType type = topicTypeMapper.selectById(id);
        if (type == null) return RestBean.failure(404, "分类不存在");
        if (type.getSystemKey() != null && !type.getSystemKey().isBlank()) {
            return RestBean.failure(400, "系统分类不允许删除");
        }
        topicTypeMapper.deleteById(id);
        return RestBean.success();
    }
}
