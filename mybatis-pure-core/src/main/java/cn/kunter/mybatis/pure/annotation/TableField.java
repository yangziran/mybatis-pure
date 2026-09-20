package cn.kunter.mybatis.pure.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指定实体类字段对应的数据库列映射规则
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TableField {

    /**
     * 数据库列名，若为空则默认按照字段名驼峰转下划线
     */
    String value() default "";

    /**
     * 是否为数据库表有效字段，设为 false 时可完美替代 @Transient
     */
    boolean exist() default true;

    /**
     * 字段自动填充策略（默认不处理）
     */
    FieldFill fill() default FieldFill.DEFAULT;

}
