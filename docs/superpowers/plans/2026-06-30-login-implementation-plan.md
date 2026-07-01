# 登录功能实现计划

> **给执行者：** 使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 按任务逐个实现。步骤使用
`- [ ]` 复选框语法追踪。

**目标：** 实现 UC-02 LoginUseCase 全栈链路：domain → outbound → usecase → infrastructure → webapi，使用真实 JWT + BCrypt +
内存存储。

**架构：** Clean Architecture 分层，遵循项目的 Presenter 模式。LoginUseCase 是公开用例（无需认证），接收用户名密码，通过
UserRepository + PasswordEncoder 验证，通过 TokenService 生成 JWT + Presenter 返回结果。

**技术栈：** Java 17, Maven 多模块, Jersey 3.1.7 + HK2, jjwt 0.12.6, jbcrypt 0.4, JUnit Jupiter 5.10.2

## 全局约束

- Java 17+, DTO 和值对象使用 record
- Presenter 模式：接口定义在 usecase 层，ThreadLocal 实现放在 webapi 层
- HK2 绑定：多契约共享实例使用 `bind(instance).to(Contract.class)`
- 所有公开 API 响应为 JSON（`application/json`）
- 密码存储为 BCrypt 哈希；JWT 使用 HS256，24 小时过期
- InMemoryUserRepo 预置一个测试用户用于集成测试

---

### 任务 1：Maven 依赖配置

**文件：**

- 修改：`UserAndAuth/pom.xml`
- 修改：`Infrastructure/pom.xml`

**产出：**

- `jakarta.inject:jakarta.inject-api:2.0.1` 在 UserAndAuth 中可用
- `io.jsonwebtoken:jjwt-api:0.12.6` + 运行时依赖 在 Infrastructure 中可用
- `org.mindrot:jbcrypt:0.4` 在 Infrastructure 中可用

- [ ] **步骤 1：为 UserAndAuth/pom.xml 添加 jakarta.inject-api**

将 `UserAndAuth/pom.xml` 替换为以下内容：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.cleancoders</groupId>
        <artifactId>SeatReservationSystem</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>UserAndAuth</artifactId>

    <dependencies>
        <dependency>
            <groupId>jakarta.inject</groupId>
            <artifactId>jakarta.inject-api</artifactId>
            <version>2.0.1</version>
        </dependency>
    </dependencies>

</project>
```

- [ ] **步骤 2：为 Infrastructure/pom.xml 添加 jjwt + jbcrypt**

将 `Infrastructure/pom.xml` 替换为以下内容：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.cleancoders</groupId>
        <artifactId>SeatReservationSystem</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>Infrastructure</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>org.cleancoders</groupId>
            <artifactId>UserAndAuth</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.cleancoders</groupId>
            <artifactId>SeatAndRoom</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.cleancoders</groupId>
            <artifactId>Reservation</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.cleancoders</groupId>
            <artifactId>SystemTask</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.6</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.6</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.6</version>
            <scope>runtime</scope>
        </dependency>

        <!-- BCrypt -->
        <dependency>
            <groupId>org.mindrot</groupId>
            <artifactId>jbcrypt</artifactId>
            <version>0.4</version>
        </dependency>
    </dependencies>

</project>
```

- [ ] **步骤 3：验证依赖解析成功**

运行：`mvn dependency:resolve -pl UserAndAuth,Infrastructure -q`
预期：BUILD SUCCESS

- [ ] **步骤 4：提交**

```bash
git add UserAndAuth/pom.xml Infrastructure/pom.xml
git commit -m "build: 添加 jakarta.inject、jjwt、jbcrypt 依赖"
```

---

### 任务 2：领域实体（UserRole + User）

**文件：**

- 新建：`UserAndAuth/src/main/java/org/cleancoders/userandauth/domain/UserRole.java`
- 新建：`UserAndAuth/src/main/java/org/cleancoders/userandauth/domain/User.java`
- 新建：`UserAndAuth/src/test/java/org/cleancoders/userandauth/domain/UserTest.java`

**产出：**

- `UserRole` 枚举：`STUDENT`、`ADMIN`
- `User` record：`User(String id, String username, String password, UserRole role, String name, String email)`

- [ ] **步骤 1：编写 UserTest 测试**

新建 `UserAndAuth/src/test/java/org/cleancoders/userandauth/domain/UserTest.java`：

```java
package org.cleancoders.userandauth.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void userRecordShouldStoreAllFields() {
        User user = new User("u1", "alice", "hashedpw", UserRole.STUDENT, "Alice", "alice@example.com");

        assertEquals("u1", user.id());
        assertEquals("alice", user.username());
        assertEquals("hashedpw", user.password());
        assertEquals(UserRole.STUDENT, user.role());
        assertEquals("Alice", user.name());
        assertEquals("alice@example.com", user.email());
    }

    @Test
    void userRoleEnumShouldHaveStudentAndAdmin() {
        assertEquals(2, UserRole.values().length);
        assertEquals(UserRole.STUDENT, UserRole.valueOf("STUDENT"));
        assertEquals(UserRole.ADMIN, UserRole.valueOf("ADMIN"));
    }

    @Test
    void userRecordsWithSameFieldsShouldBeEqual() {
        User u1 = new User("u1", "alice", "pw", UserRole.STUDENT, "Alice", "a@b.com");
        User u2 = new User("u1", "alice", "pw", UserRole.STUDENT, "Alice", "a@b.com");
        assertEquals(u1, u2);
        assertEquals(u1.hashCode(), u2.hashCode());
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -pl UserAndAuth -Dtest=UserTest -q`
预期：编译失败（User、UserRole 尚未定义）

- [ ] **步骤 3：创建 UserRole 枚举**

新建 `UserAndAuth/src/main/java/org/cleancoders/userandauth/domain/UserRole.java`：

```java
package org.cleancoders.userandauth.domain;

public enum UserRole {
    STUDENT,
    ADMIN
}
```

- [ ] **步骤 4：创建 User record**

新建 `UserAndAuth/src/main/java/org/cleancoders/userandauth/domain/User.java`：

```java
package org.cleancoders.userandauth.domain;

public record User(
    String id,
    String username,
    String password,
    UserRole role,
    String name,
    String email
) {}
```

- [ ] **步骤 5：运行测试验证通过**

运行：`mvn test -pl UserAndAuth -Dtest=UserTest`
预期：Tests run: 3, Failures: 0 — BUILD SUCCESS

- [ ] **步骤 6：提交**

```bash
git add UserAndAuth/src/main/java/org/cleancoders/userandauth/domain/UserRole.java \
        UserAndAuth/src/main/java/org/cleancoders/userandauth/domain/User.java \
        UserAndAuth/src/test/java/org/cleancoders/userandauth/domain/UserTest.java
git commit -m "feat: 添加 User record 和 UserRole 枚举"
```

---

### 任务 3：出站接口（Outbound）

**文件：**

- 新建：`UserAndAuth/src/main/java/org/cleancoders/userandauth/outbound/UserRepository.java`
- 新建：`UserAndAuth/src/main/java/org/cleancoders/userandauth/outbound/PasswordEncoder.java`
- 新建：`UserAndAuth/src/main/java/org/cleancoders/userandauth/outbound/TokenService.java`
- 新建：`UserAndAuth/src/test/java/org/cleancoders/userandauth/outbound/OutboundSignaturesTest.java`

**产出：**

- `UserRepository`：`findByUsername(String)` → `Optional<User>`、`findById(String)` → `Optional<User>`、`save(User)` →
  `User`
- `PasswordEncoder`：`encode(String)` → `String`、`matches(String, String)` → `boolean`
- `TokenService`：`generate(String userId, String username, String role)` → `String`

- [ ] **步骤 1：编写编译期签名验证测试**

新建 `UserAndAuth/src/test/java/org/cleancoders/userandauth/outbound/OutboundSignaturesTest.java`：

```java
package org.cleancoders.userandauth.outbound;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 编译期验证：确保出站接口具有预期的方法签名。
 * 这些测试同时也作为出站接口的文档。
 */
class OutboundSignaturesTest {

    @Test
    void userRepositoryShouldDefineExpectedMethods() {
        var methods = UserRepository.class.getDeclaredMethods();
        assertEquals(3, methods.length,
            "UserRepository 应定义 findByUsername、findById 和 save 三个方法");
    }

    @Test
    void passwordEncoderShouldDefineExpectedMethods() {
        var methods = PasswordEncoder.class.getDeclaredMethods();
        assertEquals(2, methods.length,
            "PasswordEncoder 应定义 encode 和 matches 两个方法");
    }

    @Test
    void tokenServiceShouldDefineGenerateMethod() {
        var methods = TokenService.class.getDeclaredMethods();
        assertEquals(1, methods.length,
            "TokenService 应定义 generate 方法");
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -pl UserAndAuth -Dtest=OutboundSignaturesTest -q`
预期：编译失败（接口尚未创建）

- [ ] **步骤 3：创建 UserRepository 接口**

新建 `UserAndAuth/src/main/java/org/cleancoders/userandauth/outbound/UserRepository.java`：

```java
package org.cleancoders.userandauth.outbound;

import org.cleancoders.userandauth.domain.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByUsername(String username);
    Optional<User> findById(String id);
    User save(User user);
}
```

- [ ] **步骤 4：创建 PasswordEncoder 接口**

新建 `UserAndAuth/src/main/java/org/cleancoders/userandauth/outbound/PasswordEncoder.java`：

```java
package org.cleancoders.userandauth.outbound;

public interface PasswordEncoder {
    String encode(String rawPassword);
    boolean matches(String rawPassword, String encodedPassword);
}
```

- [ ] **步骤 5：创建 TokenService 接口**

新建 `UserAndAuth/src/main/java/org/cleancoders/userandauth/outbound/TokenService.java`：

```java
package org.cleancoders.userandauth.outbound;

public interface TokenService {
    String generate(String userId, String username, String role);
}
```

- [ ] **步骤 6：运行测试验证通过**

运行：`mvn test -pl UserAndAuth -Dtest=OutboundSignaturesTest`
预期：Tests run: 3, Failures: 0 — BUILD SUCCESS

- [ ] **步骤 7：提交**

```bash
git add UserAndAuth/src/main/java/org/cleancoders/userandauth/outbound/ \
        UserAndAuth/src/test/java/org/cleancoders/userandauth/outbound/
git commit -m "feat: 添加出站接口 — UserRepository、PasswordEncoder、TokenService"
```

---

### 任务 4：LoginUseCase

**文件：**

- 新建：`UserAndAuth/src/main/java/org/cleancoders/userandauth/usecase/LoginUseCase.java`
- 新建：`UserAndAuth/src/test/java/org/cleancoders/userandauth/usecase/LoginUseCaseTest.java`

**依赖接口：**

- `User(String id, String username, String password, UserRole role, String name, String email)`
- `UserRepository.findByUsername(String)` → `Optional<User>`
- `PasswordEncoder.matches(String, String)` → `boolean`
- `TokenService.generate(String userId, String username, String role)` → `String`

**产出：**

- `LoginUseCase.Request(String username, String password)`
- `LoginUseCase.Output(String token)`
- `LoginUseCase.Presenter` 接口：`success(String token, User user)`、`invalidCredentials()`、`userNotFound()`
- `LoginUseCase.execute(Request)` → `Output`

- [ ] **步骤 1：编写 LoginUseCaseTest（使用 Stub Presenter）**

新建 `UserAndAuth/src/test/java/org/cleancoders/userandauth/usecase/LoginUseCaseTest.java`：

```java
package org.cleancoders.userandauth.usecase;

import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.domain.UserRole;
import org.cleancoders.userandauth.outbound.PasswordEncoder;
import org.cleancoders.userandauth.outbound.TokenService;
import org.cleancoders.userandauth.outbound.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class LoginUseCaseTest {

    private LoginUseCase useCase;
    private StubUserRepo userRepo;
    private StubPasswordEncoder passwordEncoder;
    private StubTokenService tokenService;
    private StubPresenter presenter;

    // --- Stub 实现 ---

    static class StubUserRepo implements UserRepository {
        private final java.util.Map<String, User> users = new java.util.HashMap<>();

        void addUser(User user) {
            users.put(user.username(), user);
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return Optional.ofNullable(users.get(username));
        }

        @Override
        public Optional<User> findById(String id) {
            return users.values().stream().filter(u -> u.id().equals(id)).findFirst();
        }

        @Override
        public User save(User user) {
            users.put(user.username(), user);
            return user;
        }
    }

    static class StubPasswordEncoder implements PasswordEncoder {
        @Override
        public String encode(String rawPassword) {
            return "encoded:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String encodedPassword) {
            return encodedPassword.equals("encoded:" + rawPassword);
        }
    }

    static class StubTokenService implements TokenService {
        @Override
        public String generate(String userId, String username, String role) {
            return "jwt:" + userId + ":" + username + ":" + role;
        }
    }

    static class StubPresenter implements LoginUseCase.Presenter {
        AtomicReference<String> successToken = new AtomicReference<>();
        AtomicReference<User> successUser = new AtomicReference<>();
        boolean invalidCredentialsCalled = false;
        boolean userNotFoundCalled = false;

        @Override
        public void success(String token, User user) {
            successToken.set(token);
            successUser.set(user);
        }

        @Override
        public void invalidCredentials() {
            invalidCredentialsCalled = true;
        }

        @Override
        public void userNotFound() {
            userNotFoundCalled = true;
        }
    }

    @BeforeEach
    void setUp() {
        userRepo = new StubUserRepo();
        passwordEncoder = new StubPasswordEncoder();
        tokenService = new StubTokenService();
        presenter = new StubPresenter();

        useCase = new LoginUseCase();
        useCase.userRepo = userRepo;
        useCase.passwordEncoder = passwordEncoder;
        useCase.tokenService = tokenService;
        useCase.presenter = presenter;
    }

    // --- 正常流程 ---

    @Test
    void shouldReturnTokenOnValidCredentials() {
        userRepo.addUser(new User("u1", "alice", "encoded:secret", UserRole.STUDENT, "Alice", "a@b.com"));

        var output = useCase.execute(new LoginUseCase.Request("alice", "secret"));

        assertNotNull(output);
        assertEquals("jwt:u1:alice:STUDENT", output.token());
        assertEquals("jwt:u1:alice:STUDENT", presenter.successToken.get());
        assertNotNull(presenter.successUser.get());
        assertEquals("alice", presenter.successUser.get().username());
    }

    @Test
    void shouldReturnTokenForAdminUser() {
        userRepo.addUser(new User("u2", "bob", "encoded:adminpw", UserRole.ADMIN, "Bob", "bob@b.com"));

        var output = useCase.execute(new LoginUseCase.Request("bob", "adminpw"));

        assertNotNull(output);
        assertEquals("jwt:u2:bob:ADMIN", output.token());
    }

    // --- 异常：用户不存在 ---

    @Test
    void shouldCallUserNotFoundWhenUsernameMissing() {
        var output = useCase.execute(new LoginUseCase.Request("nobody", "any"));

        assertNull(output);
        assertTrue(presenter.userNotFoundCalled);
        assertFalse(presenter.invalidCredentialsCalled);
        assertNull(presenter.successToken.get());
    }

    // --- 异常：密码错误 ---

    @Test
    void shouldCallInvalidCredentialsWhenPasswordWrong() {
        userRepo.addUser(new User("u1", "alice", "encoded:correct", UserRole.STUDENT, "Alice", "a@b.com"));

        var output = useCase.execute(new LoginUseCase.Request("alice", "wrong"));

        assertNull(output);
        assertTrue(presenter.invalidCredentialsCalled);
        assertFalse(presenter.userNotFoundCalled);
        assertNull(presenter.successToken.get());
    }

    // --- 边界：空密码 ---

    @Test
    void shouldRejectEmptyPassword() {
        userRepo.addUser(new User("u1", "alice", "encoded:secret", UserRole.STUDENT, "Alice", "a@b.com"));

        var output = useCase.execute(new LoginUseCase.Request("alice", ""));

        assertNull(output);
        assertTrue(presenter.invalidCredentialsCalled);
    }
}
```

（注意：测试通过包级私有字段直接赋值 `.userRepo` 等——`@Inject` 在纯单元测试中不会触发。）

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -pl UserAndAuth -Dtest=LoginUseCaseTest -q`
预期：编译失败（LoginUseCase 尚未创建）

- [ ] **步骤 3：创建 LoginUseCase**

新建 `UserAndAuth/src/main/java/org/cleancoders/userandauth/usecase/LoginUseCase.java`：

```java
package org.cleancoders.userandauth.usecase;

import jakarta.inject.Inject;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.outbound.PasswordEncoder;
import org.cleancoders.userandauth.outbound.TokenService;
import org.cleancoders.userandauth.outbound.UserRepository;

public class LoginUseCase {

    public record Request(String username, String password) {}
    public record Output(String token) {}

    public interface Presenter {
        void success(String token, User user);
        void invalidCredentials();
        void userNotFound();
    }

    @Inject UserRepository userRepo;
    @Inject PasswordEncoder passwordEncoder;
    @Inject TokenService tokenService;
    @Inject Presenter presenter;

    public Output execute(Request request) {
        var user = userRepo.findByUsername(request.username());
        if (user.isEmpty()) {
            presenter.userNotFound();
            return null;
        }

        var u = user.get();
        if (!passwordEncoder.matches(request.password(), u.password())) {
            presenter.invalidCredentials();
            return null;
        }

        String token = tokenService.generate(u.id(), u.username(), u.role().name());
        presenter.success(token, u);
        return new Output(token);
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`mvn test -pl UserAndAuth -Dtest=LoginUseCaseTest`
预期：Tests run: 6, Failures: 0 — BUILD SUCCESS

- [ ] **步骤 5：提交**

```bash
git add UserAndAuth/src/main/java/org/cleancoders/userandauth/usecase/LoginUseCase.java \
        UserAndAuth/src/test/java/org/cleancoders/userandauth/usecase/LoginUseCaseTest.java
git commit -m "feat: 添加 LoginUseCase（Presenter 模式）"
```

---

### 任务 5：基础设施（Infrastructure）实现

**文件：**

- 新建：`Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/InMemoryUserRepo.java`
- 新建：`Infrastructure/src/main/java/org/cleancoders/infrastructure/security/BCryptPasswordEncoder.java`
- 新建：`Infrastructure/src/main/java/org/cleancoders/infrastructure/security/JjwtTokenService.java`
- 新建：`Infrastructure/src/test/java/org/cleancoders/infrastructure/security/BCryptPasswordEncoderTest.java`
- 新建：`Infrastructure/src/test/java/org/cleancoders/infrastructure/security/JjwtTokenServiceTest.java`
- 新建：`Infrastructure/src/test/java/org/cleancoders/infrastructure/persistence/InMemoryUserRepoTest.java`

**依赖接口：**

- 来自 UserAndAuth 的 `UserRepository`、`PasswordEncoder`、`TokenService` 接口
- 来自 UserAndAuth domain 的 `User`、`UserRole`

**产出：**

- `InMemoryUserRepo` — 基于 ConcurrentHashMap，线程安全，save 时若 id 为空则自动生成 UUID
- `BCryptPasswordEncoder` — 委托给 `org.mindrot.jbcrypt.BCrypt`
- `JjwtTokenService` — HS256 JWT，sub=userId，claims 含 username+role，24 小时过期

- [ ] **步骤 1：编写基础设施单元测试**

新建 `Infrastructure/src/test/java/org/cleancoders/infrastructure/persistence/InMemoryUserRepoTest.java`：

```java
package org.cleancoders.infrastructure.persistence;

import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryUserRepoTest {

    private InMemoryUserRepo repo;

    @BeforeEach
    void setUp() {
        repo = new InMemoryUserRepo();
    }

    @Test
    void shouldSaveAndFindByUsername() {
        User user = new User("u1", "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com");
        repo.save(user);

        var found = repo.findByUsername("alice");
        assertTrue(found.isPresent());
        assertEquals("alice", found.get().username());
        assertEquals("hashed", found.get().password());
    }

    @Test
    void shouldReturnEmptyForUnknownUsername() {
        var found = repo.findByUsername("nobody");
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldSaveAndFindById() {
        User user = new User("u1", "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com");
        repo.save(user);

        var found = repo.findById("u1");
        assertTrue(found.isPresent());
    }

    @Test
    void shouldGenerateIdWhenNull() {
        User user = new User(null, "bob", "hashed", UserRole.ADMIN, "Bob", "bob@b.com");
        User saved = repo.save(user);

        assertNotNull(saved.id());
        var found = repo.findById(saved.id());
        assertTrue(found.isPresent());
    }
}
```

新建 `Infrastructure/src/test/java/org/cleancoders/infrastructure/security/BCryptPasswordEncoderTest.java`：

```java
package org.cleancoders.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BCryptPasswordEncoderTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void shouldEncodePassword() {
        String encoded = encoder.encode("mysecret");
        assertNotNull(encoded);
        assertNotEquals("mysecret", encoded);
    }

    @Test
    void shouldMatchSamePassword() {
        String encoded = encoder.encode("mysecret");
        assertTrue(encoder.matches("mysecret", encoded));
    }

    @Test
    void shouldNotMatchDifferentPassword() {
        String encoded = encoder.encode("mysecret");
        assertFalse(encoder.matches("wrong", encoded));
    }

    @Test
    void shouldProduceDifferentHashesForSameInput() {
        String h1 = encoder.encode("same");
        String h2 = encoder.encode("same");
        assertNotEquals(h1, h2, "BCrypt 盐值应使每次哈希结果不同");
        assertTrue(encoder.matches("same", h1));
        assertTrue(encoder.matches("same", h2));
    }
}
```

新建 `Infrastructure/src/test/java/org/cleancoders/infrastructure/security/JjwtTokenServiceTest.java`：

```java
package org.cleancoders.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JjwtTokenServiceTest {

    private final JjwtTokenService service = new JjwtTokenService();

    @Test
    void shouldGenerateNonEmptyToken() {
        String token = service.generate("u1", "alice", "STUDENT");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void shouldGenerateTokenWithThreeDotSegments() {
        String token = service.generate("u1", "alice", "STUDENT");
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT 应包含 header.payload.signature 三段");
    }

    @Test
    void shouldGenerateDifferentTokensForDifferentUsers() {
        String t1 = service.generate("u1", "alice", "STUDENT");
        String t2 = service.generate("u2", "bob", "ADMIN");
        assertNotEquals(t1, t2);
    }

    @Test
    void shouldGenerateDeterministicTokensForSameInput() {
        String t1 = service.generate("u1", "alice", "STUDENT");
        String t2 = service.generate("u1", "alice", "STUDENT");
        assertEquals(t1, t2, "相同输入在同一时刻应产生相同 token（确定性）");
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -pl Infrastructure -q`
预期：编译失败（实现类尚未创建）

- [ ] **步骤 3：创建 InMemoryUserRepo**

新建 `Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/InMemoryUserRepo.java`：

```java
package org.cleancoders.infrastructure.persistence;

import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.outbound.UserRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserRepo implements UserRepository {

    private final Map<String, User> byId = new ConcurrentHashMap<>();
    private final Map<String, String> byUsername = new ConcurrentHashMap<>();

    @Override
    public Optional<User> findByUsername(String username) {
        String id = byUsername.get(username);
        if (id == null) return Optional.empty();
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public User save(User user) {
        String id = user.id() != null ? user.id() : UUID.randomUUID().toString();
        User saved = new User(id, user.username(), user.password(), user.role(), user.name(), user.email());
        byId.put(id, saved);
        byUsername.put(saved.username(), id);
        return saved;
    }
}
```

- [ ] **步骤 4：创建 BCryptPasswordEncoder**

新建 `Infrastructure/src/main/java/org/cleancoders/infrastructure/security/BCryptPasswordEncoder.java`：

```java
package org.cleancoders.infrastructure.security;

import org.cleancoders.userandauth.outbound.PasswordEncoder;
import org.mindrot.jbcrypt.BCrypt;

public class BCryptPasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        try {
            return BCrypt.checkpw(rawPassword, encodedPassword);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
```

- [ ] **步骤 5：创建 JjwtTokenService**

新建 `Infrastructure/src/main/java/org/cleancoders/infrastructure/security/JjwtTokenService.java`：

```java
package org.cleancoders.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.cleancoders.userandauth.outbound.TokenService;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JjwtTokenService implements TokenService {

    private static final String SECRET = "this-is-a-test-secret-key-for-jwt-signing-256bit!!";
    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000L;

    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    @Override
    public String generate(String userId, String username, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId)
                .claim("username", username)
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(now + EXPIRATION_MS))
                .signWith(key)
                .compact();
    }
}
```

- [ ] **步骤 6：运行测试验证通过**

运行：`mvn test -pl Infrastructure`
预期：Tests run: 11, Failures: 0 — BUILD SUCCESS

- [ ] **步骤 7：提交**

```bash
git add Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/InMemoryUserRepo.java \
        Infrastructure/src/main/java/org/cleancoders/infrastructure/security/BCryptPasswordEncoder.java \
        Infrastructure/src/main/java/org/cleancoders/infrastructure/security/JjwtTokenService.java \
        Infrastructure/src/test/
git commit -m "feat: 添加 Infrastructure 实现 — InMemoryUserRepo、BCryptPasswordEncoder、JjwtTokenService"
```

---

### 任务 6：WebApi DTO 和 Presenter

**文件：**

- 新建：`WebApi/src/main/java/org/cleancoders/web/dto/LoginRequest.java`
- 新建：`WebApi/src/main/java/org/cleancoders/web/dto/RegisterRequest.java`
- 新建：`WebApi/src/main/java/org/cleancoders/web/presenter/WebApiAuthPresenter.java`
- 新建：`WebApi/src/test/java/org/cleancoders/web/presenter/WebApiAuthPresenterTest.java`

**依赖接口：**

- `LoginUseCase.Presenter` 接口（success、invalidCredentials、userNotFound）
- 来自 UserAndAuth domain 的 `User`、`UserRole`

**产出：**

- `LoginRequest(String username, String password)` — JAX-RS JSON 请求体 DTO
- `RegisterRequest(String username, String password, String name, String email)` — 骨架 DTO
- `WebApiAuthPresenter` — 基于 ThreadLocal，@Singleton，`getResponse()` → `Response`

- [ ] **步骤 1：编写 WebApiAuthPresenterTest**

新建 `WebApi/src/test/java/org/cleancoders/web/presenter/WebApiAuthPresenterTest.java`：

```java
package org.cleancoders.web.presenter;

import jakarta.ws.rs.core.Response;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebApiAuthPresenterTest {

    private WebApiAuthPresenter presenter;

    @BeforeEach
    void setUp() {
        presenter = new WebApiAuthPresenter();
    }

    @Test
    void successShouldReturn200WithTokenAndUserJson() {
        User user = new User("u1", "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com");
        presenter.success("jwt.token.here", user);

        Response response = presenter.getResponse();
        assertEquals(200, response.getStatus());

        @SuppressWarnings("unchecked")
        var entity = (java.util.Map<String, Object>) response.getEntity();
        assertEquals("jwt.token.here", entity.get("token"));

        @SuppressWarnings("unchecked")
        var userMap = (java.util.Map<String, Object>) entity.get("user");
        assertEquals("u1", userMap.get("id"));
        assertEquals("alice", userMap.get("username"));
        assertEquals("STUDENT", userMap.get("role"));
        assertEquals("Alice", userMap.get("name"));
        assertEquals("a@b.com", userMap.get("email"));
    }

    @Test
    void invalidCredentialsShouldReturn401() {
        presenter.invalidCredentials();

        Response response = presenter.getResponse();
        assertEquals(401, response.getStatus());

        @SuppressWarnings("unchecked")
        var entity = (java.util.Map<String, Object>) response.getEntity();
        assertEquals("Invalid credentials", entity.get("error"));
    }

    @Test
    void userNotFoundShouldReturn404() {
        presenter.userNotFound();

        Response response = presenter.getResponse();
        assertEquals(404, response.getStatus());

        @SuppressWarnings("unchecked")
        var entity = (java.util.Map<String, Object>) response.getEntity();
        assertEquals("User not found", entity.get("error"));
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -pl WebApi -Dtest=WebApiAuthPresenterTest -q`
预期：编译失败（WebApiAuthPresenter 尚未创建）

- [ ] **步骤 3：创建 LoginRequest DTO**

新建 `WebApi/src/main/java/org/cleancoders/web/dto/LoginRequest.java`：

```java
package org.cleancoders.web.dto;

public record LoginRequest(String username, String password) {}
```

- [ ] **步骤 4：创建 RegisterRequest DTO**

新建 `WebApi/src/main/java/org/cleancoders/web/dto/RegisterRequest.java`：

```java
package org.cleancoders.web.dto;

public record RegisterRequest(String username, String password, String name, String email) {}
```

- [ ] **步骤 5：创建 WebApiAuthPresenter**

新建 `WebApi/src/main/java/org/cleancoders/web/presenter/WebApiAuthPresenter.java`：

```java
package org.cleancoders.web.presenter;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.usecase.LoginUseCase;

import java.util.Map;

@Singleton
public class WebApiAuthPresenter implements LoginUseCase.Presenter {

    private final ThreadLocal<Response> current = new ThreadLocal<>();

    @Override
    public void success(String token, User user) {
        current.set(Response.ok(Map.of(
                "token", token,
                "user", Map.of(
                        "id", user.id(),
                        "username", user.username(),
                        "role", user.role().name(),
                        "name", user.name(),
                        "email", user.email()
                )
        )).build());
    }

    @Override
    public void invalidCredentials() {
        current.set(Response.status(401).entity(Map.of(
                "error", "Invalid credentials"
        )).build());
    }

    @Override
    public void userNotFound() {
        current.set(Response.status(404).entity(Map.of(
                "error", "User not found"
        )).build());
    }

    public Response getResponse() {
        return current.get();
    }
}
```

- [ ] **步骤 6：运行测试验证通过**

运行：`mvn test -pl WebApi -Dtest=WebApiAuthPresenterTest`
预期：Tests run: 3, Failures: 0 — BUILD SUCCESS

- [ ] **步骤 7：提交**

```bash
git add WebApi/src/main/java/org/cleancoders/web/dto/LoginRequest.java \
        WebApi/src/main/java/org/cleancoders/web/dto/RegisterRequest.java \
        WebApi/src/main/java/org/cleancoders/web/presenter/WebApiAuthPresenter.java \
        WebApi/src/test/java/org/cleancoders/web/presenter/WebApiAuthPresenterTest.java
git commit -m "feat: 添加 WebApi DTO 和 WebApiAuthPresenter"
```

---

### 任务 7：AuthResource

**文件：**

- 新建：`WebApi/src/main/java/org/cleancoders/web/resource/AuthResource.java`
- 新建：`WebApi/src/test/java/org/cleancoders/web/resource/AuthResourceTest.java`

**依赖接口：**

- `LoginUseCase.execute(Request)` → `Output`
- `LoginUseCase.Request(String username, String password)`
- `WebApiAuthPresenter.getResponse()` → `Response`
- `LoginRequest`、`RegisterRequest` DTO

**产出：**

- `POST /api/auth/login` — 委托给 LoginUseCase，返回 presenter 响应
- `POST /api/auth/register` — 返回 501 Not Implemented

- [ ] **步骤 1：编写 AuthResource 单元测试**

新建 `WebApi/src/test/java/org/cleancoders/web/resource/AuthResourceTest.java`：

```java
package org.cleancoders.web.resource;

import jakarta.ws.rs.core.Response;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.domain.UserRole;
import org.cleancoders.userandauth.usecase.LoginUseCase;
import org.cleancoders.web.dto.LoginRequest;
import org.cleancoders.web.dto.RegisterRequest;
import org.cleancoders.web.presenter.WebApiAuthPresenter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthResourceTest {

    private AuthResource resource;
    private WebApiAuthPresenter presenter;
    private boolean executeCalled;
    private LoginUseCase.Request lastRequest;
    private LoginUseCase.Output outputToReturn;

    @BeforeEach
    void setUp() {
        presenter = new WebApiAuthPresenter();
        executeCalled = false;
        lastRequest = null;

        resource = new AuthResource();
        // 手动注入 stub（包级私有字段）
        resource.presenter = presenter;
        resource.loginUseCase = new LoginUseCase() {
            @Override
            public Output execute(Request request) {
                executeCalled = true;
                lastRequest = request;
                return outputToReturn;
            }
        };
    }

    @Test
    void loginShouldDelegateToUseCase() {
        outputToReturn = new LoginUseCase.Output("test.jwt.token");
        presenter.success("test.jwt.token",
                new User("u1", "alice", "pw", UserRole.STUDENT, "Alice", "a@b.com"));

        Response response = resource.login(new LoginRequest("alice", "secret"));

        assertTrue(executeCalled);
        assertEquals("alice", lastRequest.username());
        assertEquals("secret", lastRequest.password());
        assertEquals(200, response.getStatus());
    }

    @Test
    void loginShouldReturn401OnBadCredentials() {
        outputToReturn = null;
        presenter.invalidCredentials();

        Response response = resource.login(new LoginRequest("alice", "wrong"));

        assertEquals(401, response.getStatus());
    }

    @Test
    void registerShouldReturn501() {
        Response response = resource.register(new RegisterRequest("u", "p", "n", "e"));
        assertEquals(501, response.getStatus());
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -pl WebApi -Dtest=AuthResourceTest -q`
预期：编译失败（AuthResource 尚未创建）

- [ ] **步骤 3：创建 AuthResource**

新建 `WebApi/src/main/java/org/cleancoders/web/resource/AuthResource.java`：

```java
package org.cleancoders.web.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cleancoders.userandauth.usecase.LoginUseCase;
import org.cleancoders.web.dto.LoginRequest;
import org.cleancoders.web.dto.RegisterRequest;
import org.cleancoders.web.presenter.WebApiAuthPresenter;

import java.util.Map;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject LoginUseCase loginUseCase;
    @Inject WebApiAuthPresenter presenter;

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        loginUseCase.execute(new LoginUseCase.Request(request.username(), request.password()));
        return presenter.getResponse();
    }

    @POST
    @Path("/register")
    public Response register(RegisterRequest request) {
        return Response.status(501).entity(Map.of("error", "Not implemented")).build();
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`mvn test -pl WebApi -Dtest=AuthResourceTest`
预期：Tests run: 3, Failures: 0 — BUILD SUCCESS

- [ ] **步骤 5：提交**

```bash
git add WebApi/src/main/java/org/cleancoders/web/resource/AuthResource.java \
        WebApi/src/test/java/org/cleancoders/web/resource/AuthResourceTest.java
git commit -m "feat: 添加 AuthResource — POST /api/auth/login 和 /api/auth/register"
```

---

### 任务 8：装配 AppBinder 和 AppConfig

**文件：**

- 修改：`WebApi/src/main/java/org/cleancoders/web/binder/AppBinder.java`
- 修改：`WebApi/src/main/java/org/cleancoders/web/AppConfig.java`

**依赖接口：**

- 前序任务创建的所有类

**产出：**

- HK2 绑定所有 UseCase、Presenter 和 Infrastructure 实现
- Presenter 使用实例绑定，确保 LoginUseCase 和 AuthResource 共享同一个 ThreadLocal 实例
- AppConfig 注册 AuthResource

- [ ] **步骤 1：更新 AppBinder（使用实例绑定确保 Presenter 共享）**

将 `WebApi/src/main/java/org/cleancoders/web/binder/AppBinder.java` 替换为：

```java
package org.cleancoders.web.binder;

import org.cleancoders.infrastructure.persistence.InMemoryUserRepo;
import org.cleancoders.infrastructure.security.BCryptPasswordEncoder;
import org.cleancoders.infrastructure.security.JjwtTokenService;
import org.cleancoders.userandauth.outbound.PasswordEncoder;
import org.cleancoders.userandauth.outbound.TokenService;
import org.cleancoders.userandauth.outbound.UserRepository;
import org.cleancoders.userandauth.usecase.LoginUseCase;
import org.cleancoders.web.presenter.WebApiAuthPresenter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * HK2 依赖注入绑定器。
 * 将 Infrastructure 模块的出站接口实现绑定到业务模块定义的接口上。
 *
 * 绑定规则：
 * - bind(Implementation.class).to(Interface.class); — 每次注入创建新实例（PerLookup）
 * - bind(instance).to(Contract.class); — 共享同一实例
 */
public class AppBinder extends AbstractBinder {

    @Override
    protected void configure() {
        // === UserAndAuth UseCases ===
        bind(LoginUseCase.class).to(LoginUseCase.class);

        // === Presenters（实例绑定：LoginUseCase 和 AuthResource 共享同一 ThreadLocal） ===
        WebApiAuthPresenter presenterInstance = new WebApiAuthPresenter();
        bind(presenterInstance).to(WebApiAuthPresenter.class);
        bind(presenterInstance).to(LoginUseCase.Presenter.class);

        // === Infrastructure → Outbound ===
        bind(InMemoryUserRepo.class).to(UserRepository.class).in(Singleton.class);
        bind(BCryptPasswordEncoder.class).to(PasswordEncoder.class).in(Singleton.class);
        bind(JjwtTokenService.class).to(TokenService.class).in(Singleton.class);

        // === SeatAndRoom ===
        // bind(InMemorySeatRepo.class).to(SeatRepository.class);

        // === Reservation ===
        // bind(InMemoryReservationRepo.class).to(ReservationRepository.class);

        // === SystemTask ===
        // bind(InMemoryTaskRepo.class).to(TaskRepository.class);
    }
}
```

- [ ] **步骤 2：更新 AppConfig 注册 AuthResource**

修改 `WebApi/src/main/java/org/cleancoders/web/AppConfig.java`：

在文件顶部添加 import：

```java
import org.cleancoders.web.resource.AuthResource;
```

将 `getClasses()` 方法中注册资源的部分改为：

```java
        // Resources
        classes.add(HealthResource.class);
        classes.add(AuthResource.class);
```

- [ ] **步骤 3：验证完整模块编译通过**

运行：`mvn compile -pl WebApi -q`
预期：BUILD SUCCESS

- [ ] **步骤 4：提交**

```bash
git add WebApi/src/main/java/org/cleancoders/web/binder/AppBinder.java \
        WebApi/src/main/java/org/cleancoders/web/AppConfig.java
git commit -m "feat: 装配 AppBinder 和 AppConfig，绑定登录功能所有组件"
```

---

### 任务 9：端到端集成测试

**文件：**

- 新建：`WebApi/src/test/java/org/cleancoders/web/resource/AuthResourceIntegrationTest.java`

**依赖接口：**

- 前序所有任务的完整装配（AppConfig 注册所有组件）
- InMemoryUserRepo 初始为空；测试手动预置用户用于登录

**产出：**

- 基于 JerseyTest 的集成测试，真实请求 `POST /api/auth/login`

- [ ] **步骤 1：编写集成测试**

新建 `WebApi/src/test/java/org/cleancoders/web/resource/AuthResourceIntegrationTest.java`：

```java
package org.cleancoders.web.resource;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cleancoders.infrastructure.persistence.InMemoryUserRepo;
import org.cleancoders.infrastructure.security.BCryptPasswordEncoder;
import org.cleancoders.infrastructure.security.JjwtTokenService;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.domain.UserRole;
import org.cleancoders.userandauth.outbound.PasswordEncoder;
import org.cleancoders.userandauth.outbound.TokenService;
import org.cleancoders.userandauth.outbound.UserRepository;
import org.cleancoders.userandauth.usecase.LoginUseCase;
import org.cleancoders.web.filter.CorsFilter;
import org.cleancoders.web.presenter.WebApiAuthPresenter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 登录端点端到端集成测试。
 *
 * 使用独立 ResourceConfig + 自定义 Binder 预置测试用户，
 * 不依赖真实的 AppBinder，保证测试隔离。
 */
class AuthResourceIntegrationTest extends JerseyTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final InMemoryUserRepo userRepo = new InMemoryUserRepo();

    @Override
    protected Application configure() {
        // 预置一个测试用户
        String hashedPw = encoder.encode("testpass");
        userRepo.save(new User("test-uuid", "testuser", hashedPw,
                UserRole.STUDENT, "Test User", "test@example.com"));

        // 共享 Presenter 实例（LoginUseCase 和 AuthResource 需共享同一 ThreadLocal）
        WebApiAuthPresenter presenterInstance = new WebApiAuthPresenter();

        ResourceConfig config = new ResourceConfig();
        config.register(AuthResource.class);
        config.register(CorsFilter.class);
        config.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(userRepo).to(UserRepository.class);
                bind(new BCryptPasswordEncoder()).to(PasswordEncoder.class);
                bind(new JjwtTokenService()).to(TokenService.class);
                bind(LoginUseCase.class).to(LoginUseCase.class);
                bind(presenterInstance).to(WebApiAuthPresenter.class);
                bind(presenterInstance).to(LoginUseCase.Presenter.class);
            }
        });
        return config;
    }

    @BeforeEach
    void setUp() throws Exception {
        super.setUp();
    }

    @AfterEach
    void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void shouldLoginSuccessfullyWithValidCredentials() {
        Map<String, String> body = Map.of("username", "testuser", "password", "testpass");
        Response response = target("/api/auth/login")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body));

        assertEquals(200, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);
        assertNotNull(entity.get("token"));
        assertNotNull(entity.get("user"));

        @SuppressWarnings("unchecked")
        Map<String, Object> userMap = (Map<String, Object>) entity.get("user");
        assertEquals("testuser", userMap.get("username"));
        assertEquals("STUDENT", userMap.get("role"));
    }

    @Test
    void shouldReturn401ForWrongPassword() {
        Map<String, String> body = Map.of("username", "testuser", "password", "wrongpass");
        Response response = target("/api/auth/login")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body));

        assertEquals(401, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);
        assertEquals("Invalid credentials", entity.get("error"));
    }

    @Test
    void shouldReturn404ForUnknownUser() {
        Map<String, String> body = Map.of("username", "nobody", "password", "any");
        Response response = target("/api/auth/login")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body));

        assertEquals(404, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);
        assertEquals("User not found", entity.get("error"));
    }

    @Test
    void shouldReturn501ForRegister() {
        Map<String, String> body = Map.of(
                "username", "newuser",
                "password", "pass",
                "name", "New",
                "email", "new@example.com"
        );
        Response response = target("/api/auth/register")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body));

        assertEquals(501, response.getStatus());
    }

    @Test
    void loginResponseShouldHaveJsonContentType() {
        Map<String, String> body = Map.of("username", "testuser", "password", "testpass");
        Response response = target("/api/auth/login")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body));

        assertTrue(response.getHeaderString("Content-Type").startsWith(MediaType.APPLICATION_JSON));
    }
}
```

- [ ] **步骤 2：运行集成测试验证通过**

运行：`mvn test -pl WebApi -Dtest=AuthResourceIntegrationTest`
预期：Tests run: 5, Failures: 0 — BUILD SUCCESS

- [ ] **步骤 3：运行全部测试**

运行：`mvn test -pl UserAndAuth,Infrastructure,WebApi`
预期：三个模块所有测试通过 — BUILD SUCCESS

- [ ] **步骤 4：提交**

```bash
git add WebApi/src/test/java/org/cleancoders/web/resource/AuthResourceIntegrationTest.java
git commit -m "test: 添加登录端点端到端集成测试"
```

---

## 实现后验证

所有任务完成后，运行完整构建：

```bash
mvn clean test -pl UserAndAuth,Infrastructure,WebApi
```

预期：三个模块共约 28 个测试全部通过。