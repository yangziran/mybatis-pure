package cn.kunter.mybatis.pure.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.ServiceLoader;

/**
 * SPI 管理器 负责加载和管理所有的 SPI 扩展点实现
 */
public final class SpiManager {

	/**
	 * 数据过滤处理器列表
	 */
	private static final List<DataFilterHandler> DATA_FILTER_HANDLERS = new ArrayList<>();

	/**
	 * 审计字段自动填充处理器列表
	 */
	private static final List<AuditFillHandler> AUDIT_FILL_HANDLERS = new ArrayList<>();

	/**
	 * DTO 转换处理器
	 */
	private static DtoMapperHandler dtoMapperHandler;

	static {
		ServiceLoader.load(DataFilterHandler.class).forEach(DATA_FILTER_HANDLERS::add);
		ServiceLoader.load(AuditFillHandler.class).forEach(AUDIT_FILL_HANDLERS::add);
		ServiceLoader.load(DtoMapperHandler.class).findFirst().ifPresent(h -> dtoMapperHandler = h);
	}

	/**
	 * 获取所有的不可变的数据过滤处理器列表
	 * @return 数据过滤处理器列表
	 */
	public static List<DataFilterHandler> getDataFilterHandlers() {
		return Collections.unmodifiableList(DATA_FILTER_HANDLERS);
	}

	/**
	 * 获取所有的不可变的审计字段自动填充处理器列表
	 * @return 审计字段自动填充处理器列表
	 */
	public static List<AuditFillHandler> getAuditFillHandlers() {
		return Collections.unmodifiableList(AUDIT_FILL_HANDLERS);
	}

	/**
	 * 获取 DTO 转换处理器
	 * @return DTO 转换处理器
	 * @throws IllegalStateException 若未找到 DTO 转换处理器的实现类
	 */
	public static DtoMapperHandler getDtoMapperHandler() {
		if (dtoMapperHandler == null) {
			throw new IllegalStateException("未找到 DtoMapperHandler 的 SPI 实现，无法进行 DTO 转换。");
		}
		return dtoMapperHandler;
	}

}
