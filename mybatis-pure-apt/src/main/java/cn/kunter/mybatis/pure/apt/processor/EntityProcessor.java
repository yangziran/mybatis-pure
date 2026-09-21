package cn.kunter.mybatis.pure.apt.processor;

import cn.kunter.mybatis.pure.apt.generator.SupportClassGenerator;
import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 实体处理器，用于处理带有 @Table 注解的实体类，并生成对应的 DynamicSqlSupport 类。
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("cn.kunter.mybatis.pure.annotation.Table")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class EntityProcessor extends AbstractProcessor {

    /**
     * 支持类生成器
     */
    private SupportClassGenerator supportClassGenerator;

    /**
     * 初始化处理器
     * @param processingEnv 处理环境
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.supportClassGenerator = new SupportClassGenerator(processingEnv);
    }

    /**
     * 处理注解
     * @param annotations 支持的注解集
     * @param roundEnv 当前和之前的处理环境信息
     * @return 是否声明了注解
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }

        TypeElement tableAnnotation = annotations.iterator().next();
        for (Element element : roundEnv.getElementsAnnotatedWith(tableAnnotation)) {
            if (element instanceof TypeElement) {
                TypeElement typeElement = (TypeElement) element;
                processEntity(typeElement);
            }
        }
        return true;
    }

    /**
     * 处理单个实体
     * @param typeElement 实体类型元素
     */
    private void processEntity(TypeElement typeElement) {
        List<VariableElement> fields = new ArrayList<>();
        extractFields(typeElement, fields);
        supportClassGenerator.generate(typeElement, fields);
    }

    /**
     * 提取实体字段，包括父类字段
     * @param typeElement 实体类型元素
     * @param fields 字段列表
     */
    private void extractFields(TypeElement typeElement, List<VariableElement> fields) {
        TypeMirror superclass = typeElement.getSuperclass();
        if (superclass instanceof DeclaredType) {
            TypeElement superElement = (TypeElement) ((DeclaredType) superclass).asElement();
            if (!superElement.getQualifiedName().toString().equals("java.lang.Object")) {
                // 递归提取父类字段
                extractFields(superElement, fields);
            }
        }

        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed instanceof VariableElement) {
                VariableElement field = (VariableElement) enclosed;
                Set<Modifier> modifiers = field.getModifiers();
                if (!modifiers.contains(Modifier.STATIC) && !modifiers.contains(Modifier.TRANSIENT)) {
                    javax.lang.model.element.AnnotationMirror fieldAnno = getAnnotationMirror(field, "cn.kunter" +
                            ".mybatis.pure.annotation.TableField");
                    if (fieldAnno != null && "false".equals(getAnnotationValue(fieldAnno, "exist"))) {
                        continue;
                    }
                    fields.add(field);
                }
            }
        }
    }

    private javax.lang.model.element.AnnotationMirror getAnnotationMirror(Element element, String annotationClassName) {
        for (javax.lang.model.element.AnnotationMirror m : element.getAnnotationMirrors()) {
            if (m.getAnnotationType().toString().equals(annotationClassName)) {
                return m;
            }
        }
        return null;
    }

    private String getAnnotationValue(javax.lang.model.element.AnnotationMirror annotationMirror, String key) {
        for (var entry : annotationMirror.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().toString().equals(key)) {
                return entry.getValue().getValue().toString();
            }
        }
        return null;
    }

}
