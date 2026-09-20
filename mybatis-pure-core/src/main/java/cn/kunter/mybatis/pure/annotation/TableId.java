package cn.kunter.mybatis.pure.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记实体类的主键字段 用于支撑 updateById, deleteById, selectById 等内置基类方法
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TableId {

	/**
	 * @return 数据库中的列名。如果不指定，将使用属性名转换为下划线格式
	 */
	String value() default "";

	/**
	 * @return 是否为自增主键
	 */
	boolean autoIncrement() default false;

}
