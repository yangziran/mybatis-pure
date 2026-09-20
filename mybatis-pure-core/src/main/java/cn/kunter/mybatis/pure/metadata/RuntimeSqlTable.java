package cn.kunter.mybatis.pure.metadata;

import org.mybatis.dynamic.sql.AliasableSqlTable;

/**
 * 内部统一使用的带别名支持的 Table 定义
 */
public final class RuntimeSqlTable extends AliasableSqlTable<RuntimeSqlTable> {

    /**
     * 根据表名构建运行时的 SQL 表
     * @param tableName 数据库表名
     */
    public RuntimeSqlTable(String tableName) {
        super(tableName, RuntimeSqlTable::new);
    }

    /**
     * 默认构造函数，使用默认表名构建运行时的 SQL 表
     */
    public RuntimeSqlTable() {
        super("runtime_table", RuntimeSqlTable::new);
    }

}
