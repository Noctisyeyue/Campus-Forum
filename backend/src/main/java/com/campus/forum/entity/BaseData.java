package com.campus.forum.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * DTO 转 VO 基础接口，通过反射将同名字段从 DTO 复制到 VO
 */
public interface BaseData {

    /**
     * 将当前 DTO 转换为指定 VO 类型，并支持额外处理
     */
    default <V> V asViewObject(Class<V> clazz, Consumer<V> consumer) {
        V v = this.asViewObject(clazz);
        consumer.accept(v);
        return v;
    }

    /**
     * 将当前 DTO 转换为指定 VO 类型（按同名字段自动复制）
     */
    default <V> V asViewObject(Class<V> clazz) {
        try {
            Field[] fields = clazz.getDeclaredFields();
            Constructor<V> constructor = clazz.getConstructor();
            V v = constructor.newInstance();
            Arrays.asList(fields).forEach(field -> convert(field, v));
            return v;
        } catch (ReflectiveOperationException exception) {
            Logger logger = LoggerFactory.getLogger(BaseData.class);
            logger.error("在VO与DTO转换时出现了一些错误", exception);
            throw new RuntimeException(exception.getMessage());
        }
    }

    // 将当前对象中目标字段同名字段的值复制到目标对象
    private void convert(Field field, Object target) {
        try {
            Field source = this.getClass().getDeclaredField(field.getName());
            field.setAccessible(true);
            source.setAccessible(true);
            field.set(target, source.get(this));
        } catch (IllegalAccessException | NoSuchFieldException ignored) {
        }
    }
}
