package cn.kunter.mybatis.pure.annotation;

/**
 * 字段填充策略枚举 用于在实体执行 insert 或 update 时，触发对应审计字段的自动填充
 */
public enum FieldFill {

	/**
	 * 默认不处理
	 */
	DEFAULT,
	/**
	 * 插入时自动填充（例如：create_time, create_by）
	 */
	INSERT,
	/**
	 * 更新时自动填充（例如：update_time, update_by）
	 */
	UPDATE,
	/**
	 * 插入和更新时均自动填充
	 */
	INSERT_UPDATE

}
