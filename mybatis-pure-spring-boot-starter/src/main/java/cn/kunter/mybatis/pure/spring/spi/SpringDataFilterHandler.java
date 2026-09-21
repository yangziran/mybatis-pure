package cn.kunter.mybatis.pure.spring.spi;

import cn.kunter.mybatis.pure.metadata.EntityMetadata;
import cn.kunter.mybatis.pure.spi.DataFilterHandler;
import cn.kunter.mybatis.pure.spring.autoconfigure.MybatisPureContextBridge;
import org.mybatis.dynamic.sql.AndOrCriteriaGroup;

import java.util.Collections;
import java.util.List;

/**
 * 基于 Spring 容器的数据过滤处理器实现
 */
public class SpringDataFilterHandler implements DataFilterHandler {

    /**
     * 获取全局过滤条件
     * @param metadata 实体元数据
     * @return 过滤条件组列表
     */
    @Override
    public List<AndOrCriteriaGroup> getGlobalFilters(EntityMetadata metadata) {
        DataFilterHandler springBean = MybatisPureContextBridge.getBean(DataFilterHandler.class);
        if (springBean != null && springBean != this) {
            return springBean.getGlobalFilters(metadata);
        }
        return Collections.emptyList();
    }

}
