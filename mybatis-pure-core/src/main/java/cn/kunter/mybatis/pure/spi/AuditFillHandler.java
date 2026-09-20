package cn.kunter.mybatis.pure.spi;

import cn.kunter.mybatis.pure.metadata.EntityMetadata;

/**
 * 审计字段自动填充扩展点 (SPI) 不在 Core 中依赖任何 Spring 或 Security 框架，由业务系统提供实现。
 */
public interface AuditFillHandler {

	/**
	 * 插入时触发
	 * @param entity 实体对象
	 * @param metadata 实体元数据
	 */
	void fillInsert(Object entity, EntityMetadata metadata);

	/**
	 * 更新时触发（例如 updateById）
	 * @param entity 实体对象
	 * @param metadata 实体元数据
	 */
	void fillUpdate(Object entity, EntityMetadata metadata);

}
