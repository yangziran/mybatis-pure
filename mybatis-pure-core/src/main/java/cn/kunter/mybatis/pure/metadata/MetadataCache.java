package cn.kunter.mybatis.pure.metadata;

import cn.kunter.mybatis.pure.annotation.*;
import org.mybatis.dynamic.sql.SqlColumn;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * 核心元数据解析与缓存引擎 使用 ClassValue 高效缓存，杜绝 ConcurrentHashMap 的 ClassLoader 内存泄漏
 */
public final class MetadataCache {

    private static final ClassValue<EntityMetadata> CACHE = new ClassValue<>() {
        @Override
        protected EntityMetadata computeValue(Class<?> type) {
            return resolve(type);
        }
    };

    /**
     * 获取实体类型对应的元数据
     * @param entityType 实体类型
     * @return 实体元数据
     */
    public static EntityMetadata get(Class<?> entityType) {
        return CACHE.get(entityType);
    }

    /**
     * 解析实体类型的元数据
     * @param type 实体类型
     * @return 实体元数据
     */
    private static EntityMetadata resolve(Class<?> type) {
        Table tableAnnotation = type.getAnnotation(Table.class);
        String tableName = (tableAnnotation != null && !tableAnnotation.value()
                .isEmpty()) ? tableAnnotation.value() : camelToSnake(type.getSimpleName());

        RuntimeSqlTable table = new RuntimeSqlTable(tableName);
        EntityMetadata metadata = new EntityMetadata(type, table);

        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (metadata.hasColumn(field.getName())) continue;

                if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }

                TableField fieldAnnotation = field.getAnnotation(TableField.class);
                if (fieldAnnotation != null && !fieldAnnotation.exist()) continue;

                // P0-2 FIX: 解决反射访问限制
                field.setAccessible(true);

                TableId idAnnotation = field.getAnnotation(TableId.class);

                boolean isId = idAnnotation != null;
                String overrideColumnName = null;
                if (idAnnotation != null && !idAnnotation.value().isEmpty()) {
                    overrideColumnName = idAnnotation.value();
                }

                String columnName;
                if (fieldAnnotation != null && !fieldAnnotation.value().isEmpty()) {
                    columnName = fieldAnnotation.value();
                } else if (overrideColumnName != null) {
                    columnName = overrideColumnName;
                } else {
                    columnName = camelToSnake(field.getName());
                }

                SqlColumn<?> sqlColumn = table.column(columnName);

                boolean insertFill = false;
                boolean updateFill = false;

                if (fieldAnnotation != null) {
                    FieldFill fill = fieldAnnotation.fill();
                    insertFill = (fill == FieldFill.INSERT || fill == FieldFill.INSERT_UPDATE);
                    updateFill = (fill == FieldFill.UPDATE || fill == FieldFill.INSERT_UPDATE);
                }

                Object logicUnDeletedValue = null;
                Object logicDeletedValue = null;
                LogicDelete logicDelete = field.getAnnotation(LogicDelete.class);
                if (logicDelete != null) {
                    int val = logicDelete.unDeletedValue();
                    int delVal = logicDelete.deletedValue();
                    Class<?> fieldType = field.getType();
                    if (fieldType == Boolean.class || fieldType == boolean.class) {
                        logicUnDeletedValue = (val != 0);
                        logicDeletedValue = (delVal != 0);
                    } else if (fieldType == String.class) {
                        logicUnDeletedValue = String.valueOf(val);
                        logicDeletedValue = String.valueOf(delVal);
                    } else if (fieldType == Long.class || fieldType == long.class) {
                        logicUnDeletedValue = (long) val;
                        logicDeletedValue = (long) delVal;
                    } else if (fieldType == Byte.class || fieldType == byte.class) {
                        logicUnDeletedValue = (byte) val;
                        logicDeletedValue = (byte) delVal;
                    } else if (fieldType == Short.class || fieldType == short.class) {
                        logicUnDeletedValue = (short) val;
                        logicDeletedValue = (short) delVal;
                    } else {
                        logicUnDeletedValue = val;
                        logicDeletedValue = delVal;
                    }
                }

                ColumnMetadata colMeta = new ColumnMetadata(field.getName(), columnName, sqlColumn, field.getType(),
                        isId, insertFill, updateFill, field, logicUnDeletedValue, logicDeletedValue);

                metadata.addColumn(colMeta);

                if (logicDelete != null) {
                    metadata.setLogicDeleteColumn(colMeta);
                }
            }
        }

        return metadata;
    }

    /**
     * 将驼峰命名转换为下划线命名
     * @param str 驼峰命名字符串
     * @return 下划线命名字符串
     */
    // P0-4 FIX: 性能优化版本
    private static String camelToSnake(String str) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) result.append("_");
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

}
