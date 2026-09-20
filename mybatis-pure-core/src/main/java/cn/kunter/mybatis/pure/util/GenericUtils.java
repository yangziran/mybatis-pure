package cn.kunter.mybatis.pure.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 泛型工具类 提供泛型类型的解析与处理功能
 */
public class GenericUtils {

	/**
	 * 解析目标类的泛型参数类型
	 * @param clazz 当前类
	 * @param targetClass 目标父类或接口类
	 * @return 泛型参数类型数组，若未找到则返回 null
	 */
	public static Type[] resolveTypeArguments(Class<?> clazz, Class<?> targetClass) {
		for (Type type : clazz.getGenericInterfaces()) {
			if (type instanceof ParameterizedType) {
				ParameterizedType pType = (ParameterizedType) type;
				if (targetClass.isAssignableFrom((Class<?>) pType.getRawType())) {
					return pType.getActualTypeArguments();
				}
			}
			else if (type instanceof Class) {
				Type[] resolved = resolveTypeArguments((Class<?>) type, targetClass);
				if (resolved != null) {
					return resolved;
				}
			}
		}
		return null;
	}

}
