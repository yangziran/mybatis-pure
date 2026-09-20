<div align="center">
  <h1>MyBatis-Pure</h1>
  <p><strong>一个基于 MyBatis Dynamic SQL 的现代化、高性能、强类型混合架构持久层增强框架。</strong></p>

  <!-- Badges -->
  <p>
    <a href="https://github.com/yangziran/mybatis-pure/packages"><img src="https://img.shields.io/badge/GitHub%20Packages-v1.0.0-blue.svg" alt="GitHub Packages"></a>
    <a href="https://github.com/mybatis/mybatis-3"><img src="https://img.shields.io/badge/MyBatis-3.5.19-green.svg" alt="MyBatis Version"></a>
    <a href="https://spring.io/projects/spring-boot"><img src="https://img.shields.io/badge/Spring%20Boot-4.1.x-brightgreen.svg" alt="Spring Boot Version"></a>
    <a href="https://github.com/mybatis/mybatis-dynamic-sql"><img src="https://img.shields.io/badge/MyBatis%20Dynamic%20SQL-2.0.x-blue.svg" alt="Dynamic SQL"></a>
    <a href="https://www.oracle.com/java/"><img src="https://img.shields.io/badge/JDK-17%2B-orange.svg" alt="JDK 17+"></a>
    <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License">
  </p>
</div>

---

## 🌟 为什么选择 MyBatis-Pure？

传统的 MyBatis 虽然灵活，但容易在重构时因为魔法字符串导致错误；而 MyBatis-Plus 等框架虽然功能丰富，但存在 `IService` 的过度侵入和运行期的隐性风险。

**MyBatis-Pure** 采用独特的**「混合架构 (Hybrid Architecture)」**，完美融合了 **APT 编译期生成（零开销、绝对类型安全）** 与 **运行期动态增强（防重守底、全自动转化）** 的双重优势。

它能够带给您：
- **🚫 零魔法字符串**：APT 自动在编译期为您生成强类型列名常量。
- **🛡️ 极致的安全防线**：拦截一切无 `WHERE` 条件的危险更新与删除，自动织入逻辑删除和数据权限约束。
- **⚡ 丝滑的增量更新**：只需一行 `updateById`，自动扫描非空字段生成 `Selective Update` 语句。
- **📦 双泛型中台基座**：`BaseMapperPure<Entity>` 动态推断，利用 `selectAsList(Dto.class)` 方法彻底抹平 Entity 到 DTO 的样板转换。
- **🔌 Spring 原生集成**：通过 SPI 与 Spring `ApplicationContext` 的底层握手，轻松扩展审计与数据过滤。

> 深度了解框架的设计思想：[MyBatis-Pure 核心架构演进与设计哲学](docs/architecture.md)

## 📖 快速上手 (Quick Start)

### 1. 配置 GitHub Packages 仓库
MyBatis-Pure 包托管在 GitHub Packages。请在您的 `pom.xml` 中添加以下仓库配置：

```xml
<repositories>
    <repository>
        <id>github</id>
        <name>GitHub Packages</name>
        <url>https://maven.pkg.github.com/yangziran/mybatis-pure</url>
    </repository>
</repositories>
```
> **注意：** 拉取 GitHub Packages 需要在本地的 `~/.m2/settings.xml` 中配置包含读取权限的 GitHub Personal Access Token (PAT)。

### 2. 引入依赖
```xml
<!-- 引入运行时增强基座与 Spring Boot 自动装配 -->
<dependency>
    <groupId>cn.kunter.mybatis.pure</groupId>
    <artifactId>mybatis-pure-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 引入编译期 APT 引擎 (仅在编译时生效，零运行时依赖) -->
<dependency>
    <groupId>cn.kunter.mybatis.pure</groupId>
    <artifactId>mybatis-pure-apt</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

### 2. 定义纯净的实体类 (Entity)
框架极度克制，无需继承任何基类，只需打上 `@Table` 相关注解即可：

```java
import cn.kunter.mybatis.pure.annotation.Table;
import cn.kunter.mybatis.pure.annotation.TableId;
import cn.kunter.mybatis.pure.annotation.LogicDelete;

@Table("sys_user")
public class User {
    @TableId(autoIncrement = true)
    private Long id;
    
    private String username;
    
    // 标记逻辑删除，框架将全自动织入该条件
    @LogicDelete
    private Integer isDeleted;
}
```
*编译代码后，APT 会自动为您在业务类的同级目录下的 `.support` 包中生成 `UserDynamicSqlSupport` 类，内含强类型的 `SqlColumn` 常量。*

### 3. 定义 Mapper 接口
继承 `BaseBizMapper<T, D>` 或者基础版的 `BaseMapperPure<T>`：

```java
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapperPure<User> {
    // 零手写！已自动拥有带全局安全拦截功能的全套 CRUD 与 DTO 转换能力
}
```

### 4. 极致优雅的业务调用

#### 4.1 强类型复杂查询
利用 APT 生成的常量配合 DSL，彻底告别拼写错误：
```java
import static com.yourproject.entity.support.UserDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    public List<UserDto> getActiveUsers(String targetName) {
        // 引擎自动完成：
        // 1. Where 强类型安全拼接
        // 2. 全自动追加 is_deleted = 1 逻辑删除约束
        // 3. User 查出后无缝转换为 UserDto 列表
        return userMapper.selectAsList(UserDto.class, c -> c
                .and(username, isLike("%" + targetName + "%"))
                .orderBy(id.descending())
        );
    }
}
```

#### 4.2 智能增量更新 (Selective Update)
不再需要手写繁杂的 `if-null` 判空拼接：
```java
public void updateUsername(Long userId, String newName) {
    User user = new User();
    user.setId(userId);
    user.setUsername(newName);
    
    // 引擎自动完成：
    // 1. 自动过滤掉未赋值的字段，仅拼接 set username = ?
    // 2. 自动加上 where id = ?
    // 3. 自动注入拦截约束，确保您无法更新已逻辑删除的数据
    userMapper.updateById(user);
}
```

## 🛠️ SPI 扩展桥接 (Spring 生态无缝对接)

MyBatis-Pure 的所有扩展点都支持通过简单的 Spring Bean 声明直接接管（框架底层已在 `ApplicationContext` 初始化时自动打通 SPI 桥接）：

1. **自动审计填充 (`AuditFillHandler`)**：全局接管插入与更新时刻的操作人、更新时间注入。
2. **全局数据过滤 (`DataFilterHandler`)**：轻松实现租户隔离，为所有查询/修改请求自动带上 `tenant_id = ?` 条件。
3. **自定义转换器 (`DtoMapperHandler`)**：支持使用 MapStruct 或 Orika 等接管 Entity 与 DTO 间的双向转化。

## 🎯 贡献与支持 (Contributing)
如果你发现任何 Bug 或有新的想法，欢迎提交 [Issues](https://github.com/yangziran/dynamic-sql-plus/issues) 或提出 PR。
如果 MyBatis-Pure 帮到了你，请给个 ⭐️ Star！

## 📄 开源协议 (License)
基于 [Apache License 2.0](LICENSE) 协议开源，请放心在商业项目中使用。

---
<div align="center">
  <i>Made with ❤️ by the MyBatis-Pure Community.</i>
</div>
