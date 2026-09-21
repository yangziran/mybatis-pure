package cn.kunter.mybatis.pure.apt.generator;

import com.squareup.javapoet.*;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.sql.JDBCType;
import java.util.List;

/**
 * 动态 SQL 支持类生成器，负责使用 JavaPoet 生成对应的 Support 类。
 */
public class SupportClassGenerator {

    /**
     * 编译处理环境
     */
    private final ProcessingEnvironment processingEnv;

    /**
     * 文件生成器
     */
    private final Filer filer;

    /**
     * 构造函数
     * @param processingEnv 处理环境
     */
    public SupportClassGenerator(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.filer = processingEnv.getFiler();
    }

    /**
     * 生成 Support 类
     * @param typeElement 实体类型元素
     * @param fields 字段元素列表
     */
    public void generate(TypeElement typeElement, List<VariableElement> fields) {
        String entityName = typeElement.getSimpleName().toString();
        String packageName = processingEnv.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString();

        // 推导 mapper.support 包名
        String supportPackage = deriveSupportPackage(packageName);
        String supportClassName = entityName + "DynamicSqlSupport";
        String tableNameClass = entityName + "Table";

        // 获取表名
        String tableName = getTableName(typeElement, entityName);
        String tableFieldName = camelToSnake(entityName);

        // 构建 AliasableSqlTable 内部类
        ClassName aliasableSqlTable = ClassName.get("org.mybatis.dynamic.sql", "AliasableSqlTable");
        ClassName tableClassName = ClassName.get(supportPackage, supportClassName, tableNameClass);

        TypeSpec.Builder tableClassBuilder = TypeSpec.classBuilder(tableNameClass)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .superclass(ParameterizedTypeName.get(aliasableSqlTable, tableClassName));

        // 生成内部类构造器
        MethodSpec constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
                .addStatement("super($S, $T::new)", tableName, tableClassName).build();
        tableClassBuilder.addMethod(constructor);

        // 生成列常量
        ClassName sqlColumnClass = ClassName.get("org.mybatis.dynamic.sql", "SqlColumn");
        for (VariableElement field : fields) {
            String fieldName = field.getSimpleName().toString();
            String columnName = getColumnName(field, fieldName);
            TypeName fieldType = TypeName.get(field.asType());
            if (fieldType.isPrimitive()) {
                fieldType = fieldType.box();
            }

            String jdbcType = inferJdbcType(fieldType);

            FieldSpec columnField = FieldSpec.builder(ParameterizedTypeName.get(sqlColumnClass, fieldType), fieldName
                            , Modifier.PUBLIC, Modifier.FINAL)
                    .initializer("column($S, $T.$L)", columnName, JDBCType.class, jdbcType).build();
            tableClassBuilder.addField(columnField);
        }

        // 构建外部 Support 类
        TypeSpec.Builder supportClassBuilder = TypeSpec.classBuilder(supportClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL).addType(tableClassBuilder.build());

        // 添加外部类的表单例
        FieldSpec tableInstance = FieldSpec.builder(tableClassName, tableFieldName, Modifier.PUBLIC, Modifier.STATIC,
                        Modifier.FINAL)
                .initializer("new $T()", tableClassName).build();
        supportClassBuilder.addField(tableInstance);

        // 添加外部类的列快捷引用
        for (VariableElement field : fields) {
            String fieldName = field.getSimpleName().toString();
            TypeName fieldType = TypeName.get(field.asType());
            if (fieldType.isPrimitive()) {
                fieldType = fieldType.box();
            }

            FieldSpec shortcutField = FieldSpec.builder(ParameterizedTypeName.get(sqlColumnClass, fieldType),
                            fieldName, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$L.$L", tableFieldName, fieldName).build();
            supportClassBuilder.addField(shortcutField);
        }

        JavaFile javaFile = JavaFile.builder(supportPackage, supportClassBuilder.build()).indent("    ").build();

        try {
            javaFile.writeTo(filer);
        } catch (IOException e) {
            processingEnv.getMessager()
                    .printMessage(Diagnostic.Kind.ERROR,
                            "Failed to generate " + supportClassName + ": " + e.getMessage());
        }
    }

    /**
     * 推导 Support 类的包名
     * @param entityPackage 实体包名
     * @return Support 类包名
     */
    private String deriveSupportPackage(String entityPackage) {
        if (entityPackage.endsWith(".entity")) {
            return entityPackage.substring(0, entityPackage.length() - 7) + ".mapper.support";
        } else if (entityPackage.endsWith(".model")) {
            return entityPackage.substring(0, entityPackage.length() - 6) + ".mapper.support";
        } else if (entityPackage.endsWith(".domain")) {
            return entityPackage.substring(0, entityPackage.length() - 7) + ".mapper.support";
        } else if (entityPackage.endsWith(".pojo")) {
            return entityPackage.substring(0, entityPackage.length() - 5) + ".mapper.support";
        }
        return entityPackage + ".mapper.support";
    }

    /**
     * 获取表名
     * @param typeElement 实体类型元素
     * @param entityName 实体类名
     * @return 表名
     */
    private String getTableName(TypeElement typeElement, String entityName) {
        AnnotationMirror tableAnno = getAnnotationMirror(typeElement, "cn.kunter.mybatis.pure.annotation.Table");
        if (tableAnno != null) {
            String val = getAnnotationValue(tableAnno, "value");
            if (val != null && !val.isEmpty()) return val;
        }
        return camelToSnake(entityName);
    }

    /**
     * 获取列名
     * @param field 字段元素
     * @param fieldName 字段名
     * @return 列名
     */
    private String getColumnName(VariableElement field, String fieldName) {
        AnnotationMirror fieldAnno = getAnnotationMirror(field, "cn.kunter.mybatis.pure.annotation.TableField");
        if (fieldAnno != null) {
            String val = getAnnotationValue(fieldAnno, "value");
            if (val != null && !val.isEmpty()) return val;
        }
        AnnotationMirror idAnno = getAnnotationMirror(field, "cn.kunter.mybatis.pure.annotation.TableId");
        if (idAnno != null) {
            String val = getAnnotationValue(idAnno, "value");
            if (val != null && !val.isEmpty()) return val;
        }
        return camelToSnake(fieldName);
    }

    /**
     * 推断 JDBC 类型
     * @param typeName 类型名
     * @return JDBC 类型字符串
     */
    private String inferJdbcType(TypeName typeName) {
        String name = typeName.toString();
        return switch (name) {
            case "java.lang.String" -> "VARCHAR";
            case "java.lang.Integer" -> "INTEGER";
            case "java.lang.Long" -> "BIGINT";
            case "java.util.Date", "java.time.LocalDateTime" -> "TIMESTAMP";
            case "java.sql.Date", "java.time.LocalDate" -> "DATE";
            case "java.time.LocalTime" -> "TIME";
            case "java.math.BigDecimal" -> "DECIMAL";
            case "java.lang.Boolean" -> "BIT";
            case "java.lang.Double" -> "DOUBLE";
            case "java.lang.Float" -> "REAL";
            case "java.lang.Byte" -> "TINYINT";
            case "java.lang.Short" -> "SMALLINT";
            case "byte[]" -> "VARBINARY";
            default -> "VARCHAR";
        };
    }

    /**
     * 驼峰命名转下划线命名
     * @param str 驼峰字符串
     * @return 下划线字符串
     */
    private String camelToSnake(String str) {
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

    /**
     * 获取注解镜像
     * @param element 元素
     * @param annotationClassName 注解全限定名
     * @return 注解镜像或 null
     */
    private AnnotationMirror getAnnotationMirror(Element element, String annotationClassName) {
        for (AnnotationMirror m : element.getAnnotationMirrors()) {
            if (m.getAnnotationType().toString().equals(annotationClassName)) {
                return m;
            }
        }
        return null;
    }

    /**
     * 获取注解值
     * @param annotationMirror 注解镜像
     * @param key 属性键
     * @return 属性值或 null
     */
    private String getAnnotationValue(AnnotationMirror annotationMirror, String key) {
        for (var entry : annotationMirror.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().toString().equals(key)) {
                return entry.getValue().getValue().toString();
            }
        }
        return null;
    }

}
