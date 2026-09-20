package cn.kunter.mybatis.pure.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指定实体类对应的数据库表名 若不指定，框架默认按照类名驼峰转下划线映射
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Table {

    /**
     * 数据库表名
     */
    String value() default "";

}
