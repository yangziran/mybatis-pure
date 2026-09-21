package cn.kunter.mybatis.pure.lambda;

import cn.kunter.mybatis.pure.metadata.MetadataCache;
import org.mybatis.dynamic.sql.SqlColumn;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lambda 属性桥接器，将强类型的方法引用 (如 User::getName) 映射为底层 SqlColumn
 */
public final class LambdaSqlHelper {

    private static final ConcurrentHashMap<Class<?>, String> PROPERTY_CACHE = new ConcurrentHashMap<>();

    /**
     * 核心便利 API：自动推断类型 注意：如果子类直接调用了继承自父类的 getter，此处解析出的 class 可能是父类，请改用带 class 参数的重载方法。
     */
    @SuppressWarnings("unchecked")
    public static <T, R> SqlColumn<R> col(SFunction<T, R> getter) {
        SerializedLambda lambda = extract(getter);
        String className = lambda.getImplClass().replace('/', '.');
        try {
            Class<?> entityClass = Class.forName(className);
            String property = getProperty(getter, lambda);
            return (SqlColumn<R>) MetadataCache.get(entityClass).column(property).column();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot resolve entity class from lambda.", e);
        }
    }

    /**
     * 可靠底层路径 API：防止父类 getter 导致 entityClass 推断错误
     */
    @SuppressWarnings("unchecked")
    public static <T, R> SqlColumn<R> col(Class<T> entityType, SFunction<T, R> getter) {
        String property = getProperty(getter, null);
        return (SqlColumn<R>) MetadataCache.get(entityType).column(property).column();
    }

    private static String getProperty(SFunction<?, ?> getter, SerializedLambda lambdaOrNull) {
        return PROPERTY_CACHE.computeIfAbsent(getter.getClass(), clazz -> {
            SerializedLambda lambda = lambdaOrNull != null ? lambdaOrNull : extract(getter);
            return methodToProperty(lambda.getImplMethodName());
        });
    }

    private static SerializedLambda extract(Serializable lambda) {
        try {
            Method writeReplace = lambda.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            return (SerializedLambda) writeReplace.invoke(lambda);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract SerializedLambda. Please make sure the lambda is " +
                    "serializable and not obscured.", e);
        }
    }

    /**
     * 将 Getter 方法名转换为属性名。
     * @param name 方法名
     * @return 对应的属性名
     */
    private static String methodToProperty(String name) {
        if (name.startsWith("is")) {
            name = name.substring(2);
        } else if (name.startsWith("get") || name.startsWith("set")) {
            name = name.substring(3);
        } else {
            throw new IllegalArgumentException("Error parsing property name '" + name + "'.  Didn't start with 'is', "
                    + "'get' or 'set'.");
        }
        return lowercaseFirst(name);
    }

    /**
     * 将字符串首字母小写。
     * @param str 目标字符串
     * @return 首字母小写后的字符串
     */
    private static String lowercaseFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

}
