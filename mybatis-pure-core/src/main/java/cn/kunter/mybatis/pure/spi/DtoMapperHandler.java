package cn.kunter.mybatis.pure.spi;

import java.util.List;

/**
 * Entity 和 DTO 互转 SPI 用于解耦具体转换工具（如 MapStruct, BeanUtils）
 */
public interface DtoMapperHandler {

    /**
     * 实体对象转换为 DTO 对象
     * @param source 源对象
     * @param targetClass 目标类型类
     * @param <T> 源类型
     * @param <D> 目标类型
     * @return 转换后的 DTO 对象
     */
    <T, D> D convert(T source, Class<D> targetClass);

    /**
     * 实体对象列表转换为 DTO 对象列表
     * @param sourceList 源对象列表
     * @param targetClass 目标类型类
     * @param <T> 源类型
     * @param <D> 目标类型
     * @return 转换后的 DTO 对象列表
     */
    <T, D> List<D> convertList(List<T> sourceList, Class<D> targetClass);

}
