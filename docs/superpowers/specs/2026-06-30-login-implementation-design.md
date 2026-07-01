# 登录功能实现设计

## 概述

基于 `2026-06-30-webapi-design.md` 的架构规范，实现 UC-02 LoginUseCase 的完整链路：
domain → outbound → usecase → infrastructure → webapi。

## 范围

仅实现登录功能（`POST /api/auth/login`）。注册（UC-01）的 Request DTO 预留，但不实现业务逻辑。

## 创建文件清单

### UserAndAuth 模块

| 文件                              | 层        | 说明                                                            |
|---------------------------------|----------|---------------------------------------------------------------|
| `domain/User.java`              | domain   | 用户实体 record：id, username, password(hashed), role, name, email |
| `domain/UserRole.java`          | domain   | enum: STUDENT, ADMIN                                          |
| `outbound/UserRepository.java`  | outbound | 接口：findByUsername, findById, save                             |
| `outbound/PasswordEncoder.java` | outbound | 接口：encode(raw) → String, matches(raw, encoded) → boolean      |
| `outbound/TokenService.java`    | outbound | 接口：generate(userId, username, role) → String (JWT)            |
| `usecase/LoginUseCase.java`     | usecase  | 公开用例，内嵌 Input/Output/Presenter，不继承 AuthUseCase                |

### Infrastructure 模块

| 文件                                    | 说明                                     |
|---------------------------------------|----------------------------------------|
| `security/JjwtTokenService.java`      | TokenService 实现，使用 jjwt 库生成 JWT        |
| `security/BCryptPasswordEncoder.java` | PasswordEncoder 实现，使用 BCrypt           |
| `persistence/InMemoryUserRepo.java`   | UserRepository 内存实现（ConcurrentHashMap） |

### WebApi 模块

| 文件                                   | 说明                                                |
|--------------------------------------|---------------------------------------------------|
| `resource/AuthResource.java`         | POST /api/auth/login, POST /api/auth/register（骨架） |
| `presenter/WebApiAuthPresenter.java` | 实现 LoginUseCase.Presenter，ThreadLocal 持有 Response |
| `dto/LoginRequest.java`              | 请求 DTO：username, password                         |
| `dto/RegisterRequest.java`           | 请求 DTO：username, password, name, email（预留）        |

## 修改文件清单

| 文件                          | 修改内容                                   |
|-----------------------------|----------------------------------------|
| `UserAndAuth/pom.xml`       | 添加 jakarta.inject 依赖                   |
| `Infrastructure/pom.xml`    | 添加 jjwt、jbcrypt 依赖                     |
| `WebApi/pom.xml`            | 添加 jjwt 依赖（或通过 Infrastructure 传递）      |
| `WebApi/.../AppBinder.java` | 绑定 UseCase、Presenter、Infrastructure 实现 |
| `WebApi/.../AppConfig.java` | 注册 AuthResource                        |

## 数据流

```
POST /api/auth/login  {"username":"...", "password":"..."}
  → AuthResource.login(LoginRequest)
    → extractBasicAuth(authHeader) 或直接从 JSON body 取值
    → LoginUseCase.execute(new Request(username, password))
      → userRepo.findByUsername(username)
        → 未找到 → presenter.userNotFound()
      → passwordEncoder.matches(password, user.password())
        → 不匹配 → presenter.invalidCredentials()
      → tokenService.generate(user.id(), user.username(), user.role().name())
      → presenter.success(token, user)
    → return new Output(token)
  → presenter.getResponse()
    → 200: {"token":"eyJ...", "user":{"id":"...","username":"...","role":"...","name":"..."}}
    → 401: {"error":"Invalid credentials"}
    → 404: {"error":"User not found"}
```

## Presenter 接口

```java
public interface Presenter {
    void success(String token, User user);
    void invalidCredentials();
    void userNotFound();
}
```

## LoginUseCase 签名

```java
public class LoginUseCase {
    public record Request(String username, String password) {}
    public record Output(String token) {}
    public interface Presenter { ... }

    @Inject private UserRepository userRepo;
    @Inject private PasswordEncoder passwordEncoder;
    @Inject private TokenService tokenService;
    @Inject private Presenter presenter;

    public Output execute(Request request) { ... }
}
```

## 依赖注入绑定（AppBinder）

```java
// UseCase
bind(LoginUseCase.class).to(LoginUseCase.class);

// Presenter
bind(WebApiAuthPresenter.class)
    .to(LoginUseCase.Presenter.class)
    .in(Singleton.class);

// Infrastructure → Outbound
bind(JjwtTokenService.class).to(TokenService.class).in(Singleton.class);
bind(BCryptPasswordEncoder.class).to(PasswordEncoder.class).in(Singleton.class);
bind(InMemoryUserRepo.class).to(UserRepository.class).in(Singleton.class);
```

## 测试策略

- LoginUseCase 单元测试：mock Presenter + outbound 接口
- AuthResource 集成测试：JerseyTest + 真实 Infrastructure 绑定

## 依赖版本

| 库                                 | 版本     | 用途           |
|-----------------------------------|--------|--------------|
| io.jsonwebtoken:jjwt-api          | 0.12.6 | JWT 生成       |
| io.jsonwebtoken:jjwt-impl         | 0.12.6 | JWT 实现       |
| io.jsonwebtoken:jjwt-jackson      | 0.12.6 | JWT JSON 序列化 |
| org.mindrot:jbcrypt               | 0.4    | BCrypt 密码哈希  |
| jakarta.inject:jakarta.inject-api | 2.0.1  | @Inject 注解   |