package cn.kunter.mybatis.pure.spring.spi;

import cn.kunter.mybatis.pure.metadata.EntityMetadata;
import cn.kunter.mybatis.pure.spi.AuditFillHandler;
import cn.kunter.mybatis.pure.spring.autoconfigure.SpringContextHolder;

/**
 * 基于 Spring 容器的审计字段填充处理器实现
 */
public class SpringAuditFillHandler implements AuditFillHandler {

    /**
     * 填充插入时的审计字段
     * @param entity 实体对象
     * @param metadata 实体元数据
     */
    @Override
    public void fillInsert(Object entity, EntityMetadata metadata) {
        AuditFillHandler springBean = SpringContextHolder.getBean(AuditFillHandler.class);
        if (springBean != null && springBean != this) {
            springBean.fillInsert(entity, metadata);
        }
    }

    /**
     * 填充更新时的审计字段
     * @param entity 实体对象
     * @param metadata 实体元数据
     */
    @Override
    public void fillUpdate(Object entity, EntityMetadata metadata) {
        AuditFillHandler springBean = SpringContextHolder.getBean(AuditFillHandler.class);
        if (springBean != null && springBean != this) {
            springBean.fillUpdate(entity, metadata);
        }
    }

}
