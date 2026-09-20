package cn.kunter.mybatis.pure.metadata;

import org.mybatis.dynamic.sql.AliasableSqlTable;
import org.mybatis.dynamic.sql.BasicColumn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实体级元数据缓存结构
 */
public final class EntityMetadata {

    private final Class<?> entityType;

    private final AliasableSqlTable<?> table;

    private final Map<String, ColumnMetadata> propertyColumnMap = new ConcurrentHashMap<>();
    private final List<ColumnMetadata> insertColumns = new ArrayList<>();
    private final List<ColumnMetadata> updateColumns = new ArrayList<>();
    private final List<ColumnMetadata> insertFillColumns = new ArrayList<>();
    private final List<ColumnMetadata> updateFillColumns = new ArrayList<>();
    /**
     * 分类缓存以提升运行时性能
     */
    private ColumnMetadata idColumn;
    private ColumnMetadata logicDeleteColumn;

    /**
     * 构建实体元数据
     * @param entityType 实体 Java 类型
     * @param table 对应的 DynamicSQL 表对象
     */
    public EntityMetadata(Class<?> entityType, AliasableSqlTable<?> table) {
        this.entityType = entityType;
        this.table = table;
    }

    /**
     * 添加一列元数据
     * @param metadata 列元数据
     */
    public void addColumn(ColumnMetadata metadata) {
        propertyColumnMap.put(metadata.property(), metadata);

        if (metadata.isId()) {
            this.idColumn = metadata;
        }

        /* 除了主键自增和特定排除外，通常加入到 insertColumns */
        this.insertColumns.add(metadata);

        /* 主键一般不参与 update set 赋值 */
        if (!metadata.isId()) {
            this.updateColumns.add(metadata);
        }

        if (metadata.isInsertFill()) {
            this.insertFillColumns.add(metadata);
        }
        if (metadata.isUpdateFill()) {
            this.updateFillColumns.add(metadata);
        }
    }

    /**
     * 检查是否包含某属性
     * @param property 属性名
     * @return 是否包含
     */
    public boolean hasColumn(String property) {
        return propertyColumnMap.containsKey(property);
    }

    /**
     * @return 实体类型
     */
    public Class<?> entityType() {
        return entityType;
    }

    /**
     * @return 表对象
     */
    public AliasableSqlTable<?> table() {
        return table;
    }

    /**
     * 带别名的表对象 支持 MyBatis Dynamic SQL 2.0.0+ 的 withAlias 注：需要在运行时反射创建对应的 AliasableSqlTable
     * 子类，这里简化为通用方法，实际需适配
     * @param alias 别名
     * @return 抛出异常
     */
    public AliasableSqlTable<?> table(String alias) {
        throw new UnsupportedOperationException("Table aliasing is delegated to the specific AliasableSqlTable " +
                "implementation.");
    }

    /**
     * 获取指定属性的列元数据
     * @param property 属性名
     * @return 列元数据
     */
    public ColumnMetadata column(String property) {
        ColumnMetadata col = propertyColumnMap.get(property);
        if (col == null) {
            throw new IllegalArgumentException("Property '" + property + "' not found or excluded in entity " + entityType.getName());
        }
        return col;
    }

    /**
     * @return 主键列元数据（可选）
     */
    public Optional<ColumnMetadata> idColumn() {
        return Optional.ofNullable(idColumn);
    }

    /**
     * @return 所有列元数据列表
     */
    public List<ColumnMetadata> columns() {
        return new ArrayList<>(propertyColumnMap.values());
    }

    /**
     * 为 SelectProvider 返回全部 BasicColumn 数组，支持自动映射 这里我们让生成的列带有 as(property) 的别名，彻底摆脱对
     * MyBatis 原生 mapUnderscoreToCamelCase 的依赖
     * @return 查询列数组
     */
    public BasicColumn[] selectColumns() {
        return propertyColumnMap.values().stream().map(col -> col.column().as(col.property()))
                .toArray(BasicColumn[]::new);
    }

    /**
     * @return 用于插入的列集合
     */
    public List<ColumnMetadata> insertColumns() {
        return insertColumns;
    }

    /**
     * @return 用于更新的列集合
     */
    public List<ColumnMetadata> updateColumns() {
        return updateColumns;
    }

    /**
     * @return 插入时需填充的列集合
     */
    public List<ColumnMetadata> insertFillColumns() {
        return insertFillColumns;
    }

    /**
     * @return 更新时需填充的列集合
     */
    public List<ColumnMetadata> updateFillColumns() {
        return updateFillColumns;
    }

    /**
     * @return 逻辑删除列元数据（可选）
     */
    public Optional<ColumnMetadata> logicDeleteColumn() {
        return Optional.ofNullable(logicDeleteColumn);
    }

    /**
     * 设置逻辑删除列
     * @param metadata 逻辑删除列元数据
     */
    void setLogicDeleteColumn(ColumnMetadata metadata) {
        this.logicDeleteColumn = metadata;
    }

}
