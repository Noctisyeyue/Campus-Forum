package com.campus.forum.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * DTO 转 VO 基础接口，通过 Java 反射将同名字段从 DTO 自动复制到 VO
 */
public interface BaseData {

    /**
     * 将当前 DTO 转换为指定 VO 类型，转换完成后执行额外处理逻辑
     *
     * @param clazz    目标 VO 的 Class 对象
     * @param consumer 对 VO 的额外处理（如设置 DTO 中没有的字段）
     * @param <V>      VO 的类型
     * @return 填充完成的 VO 对象
     */
    default <V> V asViewObject(Class<V> clazz, Consumer<V> consumer) {
        V v = this.asViewObject(clazz);
        consumer.accept(v);
        return v;
    }

    /**
     * 将当前 DTO 转换为指定 VO 类型，按同名字段自动复制值
     *
     * @param clazz 目标 VO 的 Class 对象
     * @param <V>   VO 的类型
     * @return 字段已填充的 VO 对象
     */
    default <V> V asViewObject(Class<V> clazz) {
        try {
            // 获取 VO 类的所有字段 → [Field: id, Field: title]
            Field[] fields = clazz.getDeclaredFields();
            // 获取 VO 类的无参构造方法
            Constructor<V> constructor = clazz.getConstructor();
            // 用构造方法创建一个空的 VO 对象
            V v = constructor.newInstance();
            // 把当前对象的每个字段的值，拷贝到新创建的目标对象 v 的对应字段里
            Arrays.asList(fields).forEach(field -> convert(field, v));
            return v;
        } catch (ReflectiveOperationException exception) {
            Logger logger = LoggerFactory.getLogger(BaseData.class);
            logger.error("在VO与DTO转换时出现了一些错误", exception);
            throw new RuntimeException(exception.getMessage());
        }
    }

    /**
     * 将当前 DTO 中与目标 VO 同名字段的值复制到目标对象
     *
     * @param field  目标 VO 的某个字段
     * @param target 目标 VO 对象实例 被赋值的
     */
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
