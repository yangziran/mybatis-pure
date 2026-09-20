package cn.kunter.mybatis.pure.spring.spi;

import cn.kunter.mybatis.pure.spi.DtoMapperHandler;
import cn.kunter.mybatis.pure.spring.autoconfigure.SpringContextHolder;

import java.util.Collections;
import java.util.List;

/**
 * 基于 Spring 容器的 DTO 映射处理器实现
 */
public class SpringDtoMapperHandler implements DtoMapperHandler {

	/**
	 * 将源对象转换为目标类型的对象
	 * @param source 源对象
	 * @param targetClass 目标类型
	 * @param <S> 源类型泛型
	 * @param <T> 目标类型泛型
	 * @return 转换后的目标对象
	 */
	@Override
	public <S, T> T convert(S source, Class<T> targetClass) {
		DtoMapperHandler springBean = SpringContextHolder.getBean(DtoMapperHandler.class);
		if (springBean != null && springBean != this) {
			return springBean.convert(source, targetClass);
		}
		throw new IllegalStateException("未能从 Spring 容器中找到有效的 DtoMapperHandler Bean");
	}

	/**
	 * 将源对象列表转换为目标类型的对象列表
	 * @param sourceList 源对象列表
	 * @param targetClass 目标类型
	 * @param <S> 源类型泛型
	 * @param <T> 目标类型泛型
	 * @return 转换后的目标对象列表
	 */
	@Override
	public <S, T> List<T> convertList(List<S> sourceList, Class<T> targetClass) {
		DtoMapperHandler springBean = SpringContextHolder.getBean(DtoMapperHandler.class);
		if (springBean != null && springBean != this) {
			return springBean.convertList(sourceList, targetClass);
		}
		return Collections.emptyList();
	}

}
