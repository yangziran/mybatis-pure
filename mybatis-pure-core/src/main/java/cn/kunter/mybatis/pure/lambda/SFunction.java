package cn.kunter.mybatis.pure.lambda;

import java.io.Serializable;
import java.util.function.Function;

/**
 * 支持序列化的 Function 接口，用于提取 Lambda 方法引用元数据
 */
@FunctionalInterface
public interface SFunction<T, R> extends Function<T, R>, Serializable {

}
