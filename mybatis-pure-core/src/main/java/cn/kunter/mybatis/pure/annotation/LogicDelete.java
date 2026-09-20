package cn.kunter.mybatis.pure.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记该字段为逻辑删除字段 默认未删除值 (存在) 为 1，已删除值为 0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface LogicDelete {

	/**
	 * 未删除时的值
	 * @return 默认值为 1
	 */
	int unDeletedValue() default 1;

	/**
	 * 已删除时的值
	 * @return 默认值为 0
	 */
	int deletedValue() default 0;

}
