package cn.kunter.mybatis.pure.metadata;

import cn.kunter.mybatis.pure.core.BaseMapperPure;
import cn.kunter.mybatis.pure.util.GenericUtils;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 负责从 MyBatis Mapper 代理对象上自动提取对应 Entity 的元数据缓存
 */
public final class MapperMetadataRegistry {

    private static final Map<Class<?>, EntityMetadata> CACHE = new ConcurrentHashMap<>();

    /**
     * 获取 Mapper 对应的实体元数据
     * @param mapperProxy Mapper 实例
     * @return 对应的实体元数据
     */
    public static EntityMetadata get(Object mapperProxy) {
        // 从 JDK 代理对象获取所有实现的接口
        Class<?>[] interfaces = mapperProxy.getClass().getInterfaces();
        for (Class<?> mapperInterface : interfaces) {
            // 我们寻找最终实现了 BaseMapperPure 的那个业务接口 (如 UserMapper)
            if (BaseMapperPure.class.isAssignableFrom(mapperInterface)) {
                EntityMetadata metadata = CACHE.computeIfAbsent(mapperInterface,
                        MapperMetadataRegistry::resolveMetadata);
                if (metadata != null) {
                    return metadata;
                }
            }
        }
        throw new IllegalArgumentException("Cannot resolve EntityMetadata for proxy: " + mapperProxy.getClass());
    }

    private static EntityMetadata resolveMetadata(Class<?> mapperInterface) {
        Type[] types = GenericUtils.resolveTypeArguments(mapperInterface, BaseMapperPure.class);
        if (types != null && types.length > 0 && types[0] instanceof Class<?> entityClass) {
            return MetadataCache.get(entityClass);
        }
        throw new cn.kunter.mybatis.pure.exception.MyBatisPureException("无法从 Mapper 接口解析实体泛型: " + mapperInterface.getName());
    }

}
