package cn.kunter.mybatis.pure.metadata;

import org.mybatis.dynamic.sql.SqlColumn;

import java.lang.reflect.Field;

/**
 * 列级元数据信息，缓存实体类中每个字段所映射的数据库列定义及行为特征
 */
public final class ColumnMetadata {

    private final String property;

    private final String columnName;

    private final SqlColumn<?> column;

    private final Class<?> javaType;

    private final boolean id;

    private final boolean insertFill;

    private final boolean updateFill;

    private final Field field;

    private final Object logicUnDeletedValue;

    private final Object logicDeletedValue;

    /**
     * 构建列元数据
     * @param property 属性名
     * @param columnName 列名
     * @param column DynamicSQL 列对象
     * @param javaType Java 类型
     * @param id 是否是主键
     * @param insertFill 是否在插入时填充
     * @param updateFill 是否在更新时填充
     * @param field Java 反射字段
     * @param logicUnDeletedValue 逻辑删除时的未删除值
     * @param logicDeletedValue 逻辑删除时的已删除值
     */
    public ColumnMetadata(String property, String columnName, SqlColumn<?> column, Class<?> javaType, boolean id,
                          boolean insertFill, boolean updateFill, Field field, Object logicUnDeletedValue,
                          Object logicDeletedValue) {
        this.property = property;
        this.columnName = columnName;
        this.column = column;
        this.javaType = javaType;
        this.id = id;
        this.insertFill = insertFill;
        this.updateFill = updateFill;
        this.field = field;
        this.logicUnDeletedValue = logicUnDeletedValue;
        this.logicDeletedValue = logicDeletedValue;
    }

    /**
     * @return 实体属性名
     */
    public String property() {
        return property;
    }

    /**
     * @return 数据库列名
     */
    public String columnName() {
        return columnName;
    }

    /**
     * @return DynamicSQL 列实例
     */
    public SqlColumn<?> column() {
        return column;
    }

    /**
     * @return Java 字段类型
     */
    public Class<?> javaType() {
        return javaType;
    }

    /**
     * @return 是否为主键
     */
    public boolean isId() {
        return id;
    }

    /**
     * @return 插入时是否自动填充
     */
    public boolean isInsertFill() {
        return insertFill;
    }

    /**
     * @return 更新时是否自动填充
     */
    public boolean isUpdateFill() {
        return updateFill;
    }

    /**
     * @return Java 反射 Field 实例
     */
    public Field field() {
        return field;
    }

    /**
     * @return 逻辑删除的未删除标识值
     */
    public Object logicUnDeletedValue() {
        return logicUnDeletedValue;
    }

    /**
     * @return 逻辑删除的已删除标识值
     */
    public Object logicDeletedValue() {
        return logicDeletedValue;
    }

}
