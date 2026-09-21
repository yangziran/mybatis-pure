package cn.kunter.mybatis.pure.core;

import cn.kunter.mybatis.pure.exception.MyBatisPureException;
import cn.kunter.mybatis.pure.metadata.ColumnMetadata;
import cn.kunter.mybatis.pure.metadata.EntityMetadata;
import cn.kunter.mybatis.pure.metadata.MapperMetadataRegistry;
import cn.kunter.mybatis.pure.spi.SpiManager;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.mybatis.dynamic.sql.*;
import org.mybatis.dynamic.sql.delete.render.DeleteStatementProvider;
import org.mybatis.dynamic.sql.insert.InsertDSL;
import org.mybatis.dynamic.sql.insert.MultiRowInsertDSL;
import org.mybatis.dynamic.sql.insert.render.InsertStatementProvider;
import org.mybatis.dynamic.sql.insert.render.MultiRowInsertStatementProvider;
import org.mybatis.dynamic.sql.render.RenderingStrategies;
import org.mybatis.dynamic.sql.select.CountDSL;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.mybatis.dynamic.sql.select.SelectModel;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.mybatis.dynamic.sql.update.UpdateDSL;
import org.mybatis.dynamic.sql.update.UpdateModel;
import org.mybatis.dynamic.sql.update.render.UpdateStatementProvider;
import org.mybatis.dynamic.sql.util.SqlProviderAdapter;
import org.mybatis.dynamic.sql.util.mybatis3.MyBatis3Utils;
import org.mybatis.dynamic.sql.where.WhereApplier;

import java.util.*;
import java.util.function.Consumer;

/**
 * 纯粹的 MyBatis Mapper 增强基座。 提供了防全表更新的防御性 API、自动织入逻辑删除/数据权限的全量条件钩子，以及选择性更新能力。
 * @param <T> 实体类型
 */
public interface BaseMapperPure<T> {

    // ==========================================
    // 0. Provider 原语 (严禁暴露给上层业务直接调用)
    // ==========================================

    java.util.regex.Pattern WHERE_PATTERN = java.util.regex.Pattern.compile("\\bwhere\\b",
            java.util.regex.Pattern.CASE_INSENSITIVE);

    /**
     * 判断 SQL 语句中是否包含独立的 WHERE 关键字（忽略大小写）
     */
    private static boolean containsWhereClause(String sql) {
        return sql != null && WHERE_PATTERN.matcher(sql).find();
    }

    /** 框架内部原语，业务代码禁止直接调用 */
    @InsertProvider(type = SqlProviderAdapter.class, method = "insert")
    int __executeInsert(InsertStatementProvider<T> statement);

    /** 框架内部原语，业务代码禁止直接调用 */
    @InsertProvider(type = SqlProviderAdapter.class, method = "insertMultiple")
    int __executeInsertMultiple(MultiRowInsertStatementProvider<T> statement);

    /** 框架内部原语，业务代码禁止直接调用 */
    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    List<T> __executeSelectMany(SelectStatementProvider statement);

    /** 框架内部原语，业务代码禁止直接调用 */
    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    Optional<T> __executeSelectOne(SelectStatementProvider statement);

    /** 框架内部原语，业务代码禁止直接调用 */
    @UpdateProvider(type = SqlProviderAdapter.class, method = "update")
    int __executeUpdate(UpdateStatementProvider statement);

    /** 框架内部原语，业务代码禁止直接调用 */
    @UpdateProvider(type = SqlProviderAdapter.class, method = "update")
    int __executeUpdateAll(UpdateStatementProvider statement);

    /** 框架内部原语，业务代码禁止直接调用 */
    @DeleteProvider(type = SqlProviderAdapter.class, method = "delete")
    int __executeDelete(DeleteStatementProvider statement);

    /** 框架内部原语，业务代码禁止直接调用 */
    @DeleteProvider(type = SqlProviderAdapter.class, method = "delete")
    int __executeDeleteAll(DeleteStatementProvider statement);

    // ==========================================
    // 1. Metadata 辅助方法
    // ==========================================

    /** 框架内部原语，业务代码禁止直接调用 */
    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    long __executeCount(SelectStatementProvider statement);

    /**
     * 获取当前 Mapper 绑定的实体元数据
     * @return 实体元数据
     */
    default EntityMetadata metadata() {
        return MapperMetadataRegistry.get(this);
    }

    /**
     * 获取当前 Mapper 绑定的动态 SQL 表定义
     * @return 动态 SQL 表定义
     */
    default AliasableSqlTable<?> table() {
        return metadata().table();
    }

    // ==========================================
    // 1. 核心钩子：全量条件自动织入 (逻辑删除 / 数据权限等)
    // ==========================================

    /**
     * 获取当前 Mapper 绑定的所有查询列
     * @return 查询列数组
     */
    default BasicColumn[] columns() {
        return metadata().selectColumns();
    }

    // ==========================================
    // 2. 查询操作 (Select / Count)
    // ==========================================

    /**
     * 获取全局过滤条件（如逻辑删除、多租户等）
     * @return 过滤条件列表
     */
    @SuppressWarnings("unchecked")
    default List<AndOrCriteriaGroup> getGlobalFilters() {
        List<AndOrCriteriaGroup> criteria = new ArrayList<>();

        // 逻辑删除织入
        metadata().logicDeleteColumn().ifPresent(col -> {
            Object logicUnDeletedValue = col.logicUnDeletedValue();
            if (logicUnDeletedValue != null) {
                criteria.add(SqlBuilder.and((SqlColumn<Object>) col.column(),
                        SqlBuilder.isEqualTo(logicUnDeletedValue)));
            }
        });

        SpiManager.getDataFilterHandlers().forEach(h -> {
            List<AndOrCriteriaGroup> filters = h.getGlobalFilters(metadata());
            if (filters != null) criteria.addAll(filters);
        });
        return criteria;
    }

    /**
     * 根据条件查询多条记录
     * @param whereApplier WHERE 条件构造器
     * @return 实体列表
     */
    default List<T> select(Consumer<QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder> whereApplier) {
        var builder = SqlBuilder.select(columns()).from(table());
        builder.configureStatement(c -> c.setNonRenderingWhereClauseAllowed(true));
        var whereBuilder = builder.where();

        List<AndOrCriteriaGroup> filters = getGlobalFilters();
        if (filters != null && !filters.isEmpty()) {
            whereBuilder.and(filters);
        }

        if (whereApplier != null) {
            whereApplier.accept(whereBuilder);
        }
        return __executeSelectMany(builder.build().render(RenderingStrategies.MYBATIS3));
    }

    /**
     * 根据条件查询单条记录
     * @param whereApplier WHERE 条件构造器
     * @return 实体对象（Optional 包装）
     */
    default Optional<T> selectOne(Consumer<QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder> whereApplier) {
        var builder = SqlBuilder.select(columns()).from(table());
        builder.configureStatement(c -> c.setNonRenderingWhereClauseAllowed(true));
        var whereBuilder = builder.where();

        List<AndOrCriteriaGroup> filters = getGlobalFilters();
        if (filters != null && !filters.isEmpty()) {
            whereBuilder.and(filters);
        }

        if (whereApplier != null) {
            whereApplier.accept(whereBuilder);
        }
        return __executeSelectOne(builder.build().render(RenderingStrategies.MYBATIS3));
    }

    /**
     * 根据主键查询单条记录
     * @param id 主键值
     * @return 实体对象（Optional 包装）
     */
    @SuppressWarnings("unchecked")
    default Optional<T> selectById(Object id) {
        SqlColumn<Object> idCol = (SqlColumn<Object>) metadata().idColumn()
                .orElseThrow(() -> new IllegalArgumentException("No @TableId found for " + metadata().entityType()
                        .getName())).column();
        return selectOne(c -> c.and(idCol, SqlBuilder.isEqualTo(id)));
    }

    // ==========================================
    // 3. 插入操作 (Insert) - 集成审计
    // ==========================================

    /**
     * 根据条件统计记录数
     * @param whereApplier WHERE 条件构造器
     * @return 记录数
     */
    default long count(Consumer<CountDSL<SelectModel>.CountWhereBuilder> whereApplier) {
        var builder = SqlBuilder.countFrom(table());
        builder.configureStatement(c -> c.setNonRenderingWhereClauseAllowed(true));
        var whereBuilder = builder.where();

        List<AndOrCriteriaGroup> filters = getGlobalFilters();
        if (filters != null && !filters.isEmpty()) {
            whereBuilder.and(filters);
        }

        if (whereApplier != null) {
            whereApplier.accept(whereBuilder);
        }
        return __executeCount(builder.build().render(RenderingStrategies.MYBATIS3));
    }

    /**
     * 插入单条记录，自动触发审计填充
     * @param entity 实体对象
     * @return 影响行数
     */
    default int insert(T entity) {
        SpiManager.getAuditFillHandlers().forEach(h -> h.fillInsert(entity, metadata()));
        return MyBatis3Utils.insert(this::__executeInsert, entity, table(), (InsertDSL<T> c) -> {
            for (ColumnMetadata colMeta : metadata().columns()) {
                try {
                    Object value = colMeta.field().get(entity);
                    if (value != null) {
                        c.map((SqlColumn<Object>) colMeta.column()).toProperty(colMeta.field().getName());
                    }
                } catch (Exception e) {
                    throw new MyBatisPureException("无法读取实体字段值: " + colMeta.property(), e);
                }
            }
            return c;
        });
    }

    // ==========================================
    // 4. 更新操作 (Update) - 强安全网守护
    // ==========================================

    /**
     * 批量插入多条记录，自动触发审计填充
     * @param entities 实体对象集合
     * @return 影响行数
     */
    default int insertBatch(Collection<? extends T> entities) {
        if (entities == null || entities.isEmpty()) return 0;
        entities.forEach(entity -> {
            SpiManager.getAuditFillHandlers().forEach(h -> h.fillInsert(entity, metadata()));
        });
        @SuppressWarnings("unchecked") Collection<T> castedEntities = (Collection<T>) entities;
        return MyBatis3Utils.insertMultiple(this::__executeInsertMultiple, castedEntities, table(),
                (MultiRowInsertDSL<T> c) -> {
            for (ColumnMetadata colMeta : metadata().columns()) {
                c.map((SqlColumn<Object>) colMeta.column()).toProperty(colMeta.field().getName());
            }
            return c;
        });
    }

    /**
     * 根据主键选择性更新记录（为空的字段不更新），自动触发审计填充
     * @param entity 实体对象
     * @return 影响行数
     */
    @SuppressWarnings("unchecked")
    default int updateById(T entity) {
        SpiManager.getAuditFillHandlers().forEach(h -> h.fillUpdate(entity, metadata()));
        SqlColumn<Object> idCol = (SqlColumn<Object>) metadata().idColumn()
                .orElseThrow(() -> new IllegalArgumentException("No @TableId found for " + metadata().entityType()
                        .getName())).column();

        Object idValue = null;
        try {
            idValue = metadata().idColumn().get().field().get(entity);
        } catch (IllegalAccessException e) {
            throw new MyBatisPureException("无法读取实体主键值", e);
        }

        if (idValue == null) {
            throw new IllegalArgumentException("ID value cannot be null for updateById");
        }

        var builder = SqlBuilder.update(table());

        // 自动构建 SET (Selective Update)
        for (ColumnMetadata colMeta : metadata().columns()) {
            if (colMeta.isId()) continue;
            try {
                Object val = colMeta.field().get(entity);
                if (val != null) {
                    builder.set((SqlColumn<Object>) colMeta.column()).equalTo(val);
                }
            } catch (IllegalAccessException e) {
                throw new MyBatisPureException("无法读取实体字段值: " + colMeta.property(), e);
            }
        }

        var whereBuilder = builder.where(idCol, SqlBuilder.isEqualTo(idValue));

        List<AndOrCriteriaGroup> filters = getGlobalFilters();
        if (filters != null && !filters.isEmpty()) {
            whereBuilder.and(filters);
        }

        UpdateStatementProvider provider = builder.build().render(RenderingStrategies.MYBATIS3);
        return __executeUpdate(provider);
    }

    /**
     * 根据条件更新记录，防全表更新
     * @param applier 更新条件构造器
     * @return 影响行数
     * @throws IllegalStateException 如果没有 WHERE 条件
     */
    default int update(Consumer<UpdateDSL<UpdateModel>> applier) {
        var builder = SqlBuilder.update(table());
        if (applier != null) applier.accept(builder);

        List<AndOrCriteriaGroup> filters = getGlobalFilters();
        if (filters != null && !filters.isEmpty()) {
            builder.where().and(filters);
        }

        UpdateStatementProvider provider = builder.build().render(RenderingStrategies.MYBATIS3);
        if (!containsWhereClause(provider.getUpdateStatement())) {
            throw new IllegalStateException("【安全阻断】常规 update 操作必须包含 where 条件。若确需全表更新，请显式调用 updateAll 方法！");
        }
        return __executeUpdate(provider);
    }

    // ==========================================
    // 5. 删除操作 (Delete) - 强安全网守护
    // ==========================================

    /**
     * 全表更新记录
     * @param applier 更新条件构造器
     * @return 影响行数
     */
    default int updateAll(Consumer<UpdateDSL<UpdateModel>> applier) {
        var builder = SqlBuilder.update(table());
        if (applier != null) applier.accept(builder);

        List<AndOrCriteriaGroup> filters = getGlobalFilters();
        if (filters != null && !filters.isEmpty()) {
            builder.where().and(filters);
        }
        return __executeUpdateAll(builder.build().render(RenderingStrategies.MYBATIS3));
    }

    /**
     * 根据主键删除记录，支持逻辑删除
     * @param id 主键值
     * @return 影响行数
     */
    @SuppressWarnings("unchecked")
    default int deleteById(Object id) {
        SqlColumn<Object> idCol = (SqlColumn<Object>) metadata().idColumn()
                .orElseThrow(() -> new IllegalArgumentException("No @TableId found for " + metadata().entityType()
                        .getName())).column();

        if (metadata().logicDeleteColumn().isPresent()) {
            var colMeta = metadata().logicDeleteColumn().get();
            var builder = SqlBuilder.update(table());
            builder.set((SqlColumn<Object>) colMeta.column()).equalTo(colMeta.logicDeletedValue());
            var whereBuilder = builder.where(idCol, SqlBuilder.isEqualTo(id));

            List<AndOrCriteriaGroup> filters = getGlobalFilters();
            if (filters != null && !filters.isEmpty()) {
                whereBuilder.and(filters);
            }
            return __executeUpdate(builder.build().render(RenderingStrategies.MYBATIS3));
        } else {
            var builder = SqlBuilder.deleteFrom(table());
            var whereBuilder = builder.where(idCol, SqlBuilder.isEqualTo(id));
            List<AndOrCriteriaGroup> filters = getGlobalFilters();
            if (filters != null && !filters.isEmpty()) {
                whereBuilder.and(filters);
            }
            return __executeDelete(builder.build().render(RenderingStrategies.MYBATIS3));
        }
    }

    /**
     * 根据条件删除记录，防全表删除，支持逻辑删除
     * @param whereApplier WHERE 条件构造器
     * @return 影响行数
     * @throws IllegalStateException 如果没有 WHERE 条件
     */
    default int delete(WhereApplier whereApplier) {
        if (metadata().logicDeleteColumn().isPresent()) {
            var colMeta = metadata().logicDeleteColumn().get();
            var builder = SqlBuilder.update(table());
            builder.set((SqlColumn<Object>) colMeta.column()).equalTo(colMeta.logicDeletedValue());

            var whereBuilder = builder.where();
            if (whereApplier != null) {
                whereBuilder = builder.applyWhere(whereApplier);
            }

            List<AndOrCriteriaGroup> filters = getGlobalFilters();
            if (filters != null && !filters.isEmpty()) {
                whereBuilder.and(filters);
            }

            UpdateStatementProvider provider = builder.build().render(RenderingStrategies.MYBATIS3);
            if (!containsWhereClause(provider.getUpdateStatement())) {
                throw new IllegalStateException("【安全阻断】常规 delete 操作必须包含 where 条件。若确需全表清空，请显式调用 deleteAll 方法！");
            }
            return __executeUpdate(provider);
        } else {
            var builder = SqlBuilder.deleteFrom(table());
            builder.configureStatement(c -> c.setNonRenderingWhereClauseAllowed(true));

            var whereBuilder = builder.where();
            if (whereApplier != null) {
                whereBuilder = builder.applyWhere(whereApplier);
            }

            List<AndOrCriteriaGroup> filters = getGlobalFilters();
            if (filters != null && !filters.isEmpty()) {
                whereBuilder.and(filters);
            }

            DeleteStatementProvider provider = builder.build().render(RenderingStrategies.MYBATIS3);
            if (!containsWhereClause(provider.getDeleteStatement())) {
                throw new IllegalStateException("【安全阻断】常规 delete 操作必须包含 where 条件。若确需全表清空，请显式调用 deleteAll 方法！");
            }
            return __executeDelete(provider);
        }
    }

    // ==========================================
    // 6. DTO 转换快捷查询 (DTO Mapping)
    // ==========================================

    /**
     * 全表删除记录，支持逻辑删除
     * @param whereApplier WHERE 条件构造器
     * @return 影响行数
     */
    default int deleteAll(WhereApplier whereApplier) {
        if (metadata().logicDeleteColumn().isPresent()) {
            var colMeta = metadata().logicDeleteColumn().get();
            var builder = SqlBuilder.update(table());
            builder.set((SqlColumn<Object>) colMeta.column()).equalTo(colMeta.logicDeletedValue());

            var whereBuilder = builder.where();
            if (whereApplier != null) {
                whereBuilder = builder.applyWhere(whereApplier);
            }

            List<AndOrCriteriaGroup> filters = getGlobalFilters();
            if (filters != null && !filters.isEmpty()) {
                whereBuilder.and(filters);
            }
            return __executeUpdateAll(builder.build().render(RenderingStrategies.MYBATIS3));
        } else {
            var builder = SqlBuilder.deleteFrom(table());
            builder.configureStatement(c -> c.setNonRenderingWhereClauseAllowed(true));

            var whereBuilder = builder.where();
            if (whereApplier != null) {
                whereBuilder = builder.applyWhere(whereApplier);
            }

            List<AndOrCriteriaGroup> filters = getGlobalFilters();
            if (filters != null && !filters.isEmpty()) {
                whereBuilder.and(filters);
            }
            return __executeDeleteAll(builder.build().render(RenderingStrategies.MYBATIS3));
        }
    }

    /**
     * 根据条件查询单条记录并转换为指定 DTO 类型
     * @param targetClass 目标 DTO 类
     * @param whereApplier WHERE 条件构造器
     * @param <R> 目标 DTO 泛型
     * @return DTO 对象（Optional 包装）
     */
    default <R> Optional<R> selectOneAs(Class<R> targetClass,
                                        Consumer<QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder> whereApplier) {
        Optional<T> entityOpt = this.selectOne(whereApplier);
        return entityOpt.map(entity -> SpiManager.getDtoMapperHandler().convert(entity, targetClass));
    }

    /**
     * 根据条件查询多条记录并转换为指定 DTO 类型列表
     * @param targetClass 目标 DTO 类
     * @param whereApplier WHERE 条件构造器
     * @param <R> 目标 DTO 泛型
     * @return DTO 列表
     */
    default <R> List<R> selectAsList(Class<R> targetClass,
                                     Consumer<QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder> whereApplier) {
        List<T> entities = this.select(whereApplier);
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return SpiManager.getDtoMapperHandler().convertList(entities, targetClass);
    }

    /**
     * 根据主键查询单条记录并转换为指定 DTO 类型
     * @param id 主键值
     * @param targetClass 目标 DTO 类
     * @param <R> 目标 DTO 泛型
     * @return DTO 对象（Optional 包装）
     */
    default <R> Optional<R> selectByIdAs(Object id, Class<R> targetClass) {
        Optional<T> entityOpt = this.selectById(id);
        return entityOpt.map(entity -> SpiManager.getDtoMapperHandler().convert(entity, targetClass));
    }

}
