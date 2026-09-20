package cn.kunter.mybatis.pure.spring.autoconfigure;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Spring 上下文持有者，用于非 Spring 托管的类获取 Bean
 */
public class SpringContextHolder implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	private static ApplicationContext applicationContext;

	/**
	 * 初始化 Spring 上下文
	 * @param applicationContext 可配置的应用上下文
	 */
	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		if (SpringContextHolder.applicationContext == null) {
			SpringContextHolder.applicationContext = applicationContext;
		}
	}

	/**
	 * 获取当前的 Spring 应用上下文
	 * @return 应用上下文
	 */
	public static ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	/**
	 * 从 Spring 容器中获取指定类型的 Bean
	 * @param clazz Bean 的类型
	 * @param <T> Bean 类型泛型
	 * @return 对应的 Bean 实例，如果未找到或发生异常则返回 null
	 */
	public static <T> T getBean(Class<T> clazz) {
		if (applicationContext == null) {
			return null;
		}
		try {
			return applicationContext.getBean(clazz);
		}
		catch (Exception e) {
			return null;
		}
	}

}
