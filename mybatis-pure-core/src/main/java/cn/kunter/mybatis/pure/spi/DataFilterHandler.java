package cn.kunter.mybatis.pure.spi;

import cn.kunter.mybatis.pure.metadata.EntityMetadata;
import org.mybatis.dynamic.sql.AndOrCriteriaGroup;
import java.util.List;

/**
 * 数据过滤扩展点 (SPI) 用于在查询、更新等操作中动态追加全局过滤条件（如逻辑删除、多租户等）
 */
public interface DataFilterHandler {

	/**
	 * 获取全局附加过滤条件
	 * @param metadata 实体元数据
	 * @return 全局附加过滤条件列表
	 */
	List<AndOrCriteriaGroup> getGlobalFilters(EntityMetadata metadata);

}
