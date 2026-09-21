# MyBatis-Pure 核心架构演进与设计哲学

## 1. 架构流派的碰撞：为何需要 MyBatis-Pure？

在现代 Java 后端开发中，持久层框架演进出了两大主流方向：

1. **基于字符串的动态 SQL** (如传统 MyBatis XML)：极其灵活，但丧失了编译期类型安全，重构时形同盲人摸象。
2. **基于全量代理的重型框架** (如 MyBatis-Plus)：提供了极其丰富的 `IService` 封装，但这种“越权”导致了业务层的严重侵入，且大量基于字符串列名的
   API（如 `QueryWrapper`）仍然存在拼写错误风险。

我们在前期孵化了 **[Dynamic SQL Plus](https://github.com/yangziran/dynamic-sql-plus)** 这一纯 APT 编译期方案，它追求 100%
的静态安全，却牺牲了部分业务开发的敏捷性。

为了打破这一僵局， **MyBatis-Pure** 诞生了。它选择了 **混合架构 (Hybrid Architecture)**，即： **「编译期的绝对强类型」 +
「运行期的极客防护网与动态代理」**。

---

## 2. 混合架构 (Hybrid Architecture) 深度解析

MyBatis-Pure 的架构主要由三大模块构成，它们各司其职，共同闭环了从编译期到运行期的全生命周期管理：

### 2.1 模块一：无感元数据引擎 (`mybatis-pure-apt`)

此模块基于 `JSR 269` (Pluggable Annotation Processing API) 构建，是在 Java 编译期间挂载的钩子。

- **工作机制**：在项目执行 `javac` 或 `mvn compile` 时，APT 处理器会自动嗅探所有被 `@Table` 标记的实体类。
- **输出产物**：它会在业务类同级生成 `.support` 包，并输出 `XxxDynamicSqlSupport` 类。
- **核心价值**：这些 Support 类提供了与数据库列严格映射的 MyBatis Dynamic SQL 原生强类型常量（`SqlColumn<T>`）。开发者在编写
  DSL 时再也不需要手写任何魔法字符串。此过程 **完全没有运行时反射带来的性能开销**。

### 2.2 模块二：防御级运行时基座 (`mybatis-pure-core`)

这是框架的心脏，提供 `BaseMapperPure<T>` ，支持方法级的动态 DTO 映射。

#### a. 智能元数据缓存 (`MetadataCache`)

在应用启动或实体首次被加载时，利用 `ClassValue` 进行了一次性的低开销反射，将类的字段类型、`@LogicDelete`
注解上的默认值等元数据永久缓存。告别了原生反射的 OOM 隐患与并发性能瓶颈。

#### b. 强安全防腐网 (`Safe API`)

我们重构了原生的 `DSLCompleter`：

- 对于 `update` / `delete`：如果在传入的 Lambda 闭包中没有发现 `where` 关键字，框架会在渲染阶段直接抛出
  `IllegalStateException` 拦截请求， **从底层彻底封杀“忘加条件导致全表被清空”的生产事故**。
- `updateById` 的选择性更新：抛弃了繁琐的 `set()`，通过读取 `MetadataCache` 提取非 `null` 字段，全自动为您拼接
  `Selective Update` 语句。

#### c. 全自动条件织入 (Global Filters)

提供 `DataFilterHandler` SPI，每当执行任何查询、修改或删除时，底层引擎会自动向现有的 DSL Builder 中通过 `.and(...)`
追加多租户、逻辑删除等全局约束， **对业务代码 100% 透明**。

### 2.3 模块三：Spring 生态原力桥接 (`mybatis-pure-spring-boot-starter`)

通过自动装配和基于 `ApplicationContextInitializer` 的高级生命周期介入，我们将纯 Java 的 SPI (Service Provider Interface)
与 Spring 的 IoC 容器无缝打通。

- **业务侧体验**：您只需要在 Spring 中写一个 `@Component` 并且实现 `AuditFillHandler`，我们的核心层在执行插入时，就能自动回调您的
  Spring Bean 进行数据的审计填充。

---

## 3. 设计原则总结 (Design Principles)

1. **零侵入，防越权**：所有的增强逻辑被严格约束在 `Mapper` 层，绝对不向外层蔓延，还给您一个纯净无依赖的 Domain/Service 体系。
2. **Fail-Fast (快速失败)**：不管是配置异常还是全表更新风险，绝不生吞错误，绝不静默处理。任何不符合预期的状态必须在启动期或触发瞬间抛出异常。
3. **极致性能**：剥离高频热点代码中的反射，采用 `SpiManager` 单例缓存与 `ClassValue` 缓存；通过
   `Collections.synchronizedMap(new WeakHashMap<>())` 完美解决 Lambda 代理类的卸载难题，保障 JVM 元空间安全。

*“将复杂的架构留给框架底座，将最优雅、最纯粹的编码体验还给开发者。”*
