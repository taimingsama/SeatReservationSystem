# UC-04 ListRoomsUseCase 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 实现 `GET /api/rooms` —— 一个公开端点,返回所有 `OPEN` 状态的自习室。

**架构：** Clean Architecture:`RoomRepository`(outbound 接口,SeatAndRoom)→ `ListRoomsUseCase`(公开,不继承认证基类)→ `WebApiRoomPresenter`(ThreadLocal)→ `RoomResource`(JAX-RS)。`StudyRoom.status` 由 `String` 迁移为 `RoomStatus` 枚举。`InMemoryRoomRepo` 实现仓储接口。

**技术栈：** Java 17,JAX-RS/Jersey 3.1.7,HK2 DI,JUnit 5,Jersey Test Framework(Jetty)。

## 全局约束

- Java 17(`maven.compiler.source/target=17`)。
- 模块包根:`org.cleancoders.seatandroom.{domain,outbound,usecase}`、`org.cleancoders.infrastructure.persistence`、`org.cleancoders.web.{resource,presenter,dto.room}`。
- 测试风格:手写 stub,不用 Mockito;每个测试文件自包含嵌套 stub 类。
- DTO 为 record,带 `@Schema` Swagger 注解。
- Presenter 继承 `WebApiPresenter`(含 ThreadLocal `current` 字段与 `getResponse()`);`@Singleton`。
- commit message 脚注:`Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`。
- `StudyRoom` 当前无生产代码引用,修改 `status` 类型安全。

---

## 文件结构

| 文件 | 动作 | 职责 |
|---|---|---|
| `SeatAndRoom/src/main/java/org/cleancoders/seatandroom/domain/RoomStatus.java` | 新增 | 枚举:OPEN, CLOSED, MAINTENANCE |
| `SeatAndRoom/src/main/java/org/cleancoders/seatandroom/domain/StudyRoom.java` | 修改 | `status: String` → `RoomStatus` |
| `SeatAndRoom/src/main/java/org/cleancoders/seatandroom/outbound/RoomRepository.java` | 新增 | 仓储接口:findByStatus, findById, save |
| `SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/ListRoomsUseCase.java` | 新增 | 公开用例:列出 OPEN 房间 |
| `SeatAndRoom/src/test/java/org/cleancoders/seatandroom/usecase/ListRoomsUseCaseTest.java` | 新增 | 单元测试,用 StubRoomRepo |
| `Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/InMemoryRoomRepo.java` | 新增 | 内存版 RoomRepository |
| `Infrastructure/src/test/java/org/cleancoders/infrastructure/persistence/InMemoryRoomRepoTest.java` | 新增 | InMemoryRoomRepo 单元测试 |
| `WebApi/src/main/java/org/cleancoders/web/dto/room/RoomResponse.java` | 新增 | 单个自习室响应 DTO record |
| `WebApi/src/main/java/org/cleancoders/web/dto/room/RoomListResponse.java` | 新增 | 列表包装 DTO,`rooms` 字段放 `List<RoomResponse>` |
| `WebApi/src/main/java/org/cleancoders/web/presenter/WebApiRoomPresenter.java` | 新增 | 实现 ListRoomsUseCase.Presenter,返回 RoomListResponse 包装体 |
| `WebApi/src/main/java/org/cleancoders/web/resource/RoomResource.java` | 新增 | JAX-RS `@Path("/rooms")` |
| `WebApi/src/main/java/org/cleancoders/web/binder/AppBinder.java` | 修改 | 绑定 UseCase/Presenter/Repo |
| `WebApi/src/test/java/org/cleancoders/web/resource/RoomResourceTest.java` | 新增 | Resource 单元测试 |
| `WebApi/src/test/java/org/cleancoders/web/resource/RoomResourceIntegrationTest.java` | 新增 | JerseyTest 集成测试 |

---

## 任务 1:RoomStatus 枚举 + StudyRoom 迁移

**文件：**
- 新增:`SeatAndRoom/src/main/java/org/cleancoders/seatandroom/domain/RoomStatus.java`
- 修改:`SeatAndRoom/src/main/java/org/cleancoders/seatandroom/domain/StudyRoom.java`

**接口：**
- 产出:`RoomStatus` 枚举(值 `OPEN`、`CLOSED`、`MAINTENANCE`);`StudyRoom.status()` 返回 `RoomStatus`。

- [ ] **步骤 1:创建 `RoomStatus.java`**

```java
package org.cleancoders.seatandroom.domain;

/**
 * Status of a study room. Only OPEN rooms are listed to students.
 */
public enum RoomStatus
{
    OPEN,
    CLOSED,
    MAINTENANCE
}
```

- [ ] **步骤 2:把 `StudyRoom.status` 迁移为 `RoomStatus`**

整个文件替换为:

```java
package org.cleancoders.seatandroom.domain;

/**
 * A study room that contains seats.
 */
public record StudyRoom(
        String id,
        String name,
        String location,
        int capacity,
        RoomStatus status
)
{
}
```

- [ ] **步骤 3:验证 SeatAndRoom 可编译**

运行:`mvn -pl SeatAndRoom -am compile -q`
预期:BUILD SUCCESS(不存在对旧 `String status` 的引用)。

- [ ] **步骤 4:提交**

```bash
git add SeatAndRoom/src/main/java/org/cleancoders/seatandroom/domain/RoomStatus.java \
        SeatAndRoom/src/main/java/org/cleancoders/seatandroom/domain/StudyRoom.java
git commit -m "refactor(SeatAndRoom): StudyRoom.status 改用 RoomStatus 枚举

为 UC-04 按 OPEN 过滤做准备,与 SeatStatus/ReservationStatus 风格一致。

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## 任务 2:RoomRepository 接口

**文件：**
- 新增:`SeatAndRoom/src/main/java/org/cleancoders/seatandroom/outbound/RoomRepository.java`

**接口：**
- 消费:`StudyRoom`、`RoomStatus`(来自任务 1)。
- 产出:`RoomRepository`,含 `List<StudyRoom> findByStatus(RoomStatus)`、`Optional<StudyRoom> findById(String)`、`StudyRoom save(StudyRoom)`。

- [ ] **步骤 1:创建 `RoomRepository.java`**

```java
package org.cleancoders.seatandroom.outbound;

import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;

import java.util.List;
import java.util.Optional;

/**
 * Persistence contract for {@link StudyRoom} aggregates.
 */
public interface RoomRepository
{
    List<StudyRoom> findByStatus(RoomStatus status);

    Optional<StudyRoom> findById(String id);

    StudyRoom save(StudyRoom room);
}
```

- [ ] **步骤 2:验证编译**

运行:`mvn -pl SeatAndRoom -am compile -q`
预期:BUILD SUCCESS。

- [ ] **步骤 3:提交**

```bash
git add SeatAndRoom/src/main/java/org/cleancoders/seatandroom/outbound/RoomRepository.java
git commit -m "feat(SeatAndRoom): 新增 RoomRepository 接口

提供 findByStatus(UC-04)/findById(UC-05)/save(UC-06) 契约。

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## 任务 3:ListRoomsUseCase(TDD)

**文件：**
- 新增:`SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/ListRoomsUseCase.java`
- 测试:`SeatAndRoom/src/test/java/org/cleancoders/seatandroom/usecase/ListRoomsUseCaseTest.java`

**接口：**
- 消费:`RoomRepository.findByStatus(RoomStatus)`(任务 2)、`StudyRoom`/`RoomStatus`(任务 1)。
- 产出:`ListRoomsUseCase`,含 `execute(Request) → Output`;内嵌 `Request`(空 record)、`Output(List<StudyRoom> rooms)`、`Presenter.presentRooms(List<StudyRoom>)`。

- [ ] **步骤 1:写失败测试**

```java
package org.cleancoders.seatandroom.usecase;

import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.outbound.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ListRoomsUseCaseTest
{

    private ListRoomsUseCase useCase;
    private StubRoomRepo roomRepo;
    private StubPresenter presenter;

    @BeforeEach
    void setUp()
    {
        roomRepo = new StubRoomRepo();
        presenter = new StubPresenter();
        useCase = new ListRoomsUseCase();
        useCase.roomRepo = roomRepo;
        useCase.presenter = presenter;
    }

    @Test
    void shouldReturnOnlyOpenRoomsAndPresentThem()
    {
        StudyRoom open1 = new StudyRoom("r1", "A", "L1", 10, RoomStatus.OPEN);
        StudyRoom closed = new StudyRoom("r2", "B", "L2", 10, RoomStatus.CLOSED);
        StudyRoom maint = new StudyRoom("r3", "C", "L3", 10, RoomStatus.MAINTENANCE);
        StudyRoom open2 = new StudyRoom("r4", "D", "L4", 10, RoomStatus.OPEN);
        roomRepo.add(open1, closed, maint, open2);

        var output = useCase.execute(new ListRoomsUseCase.Request());

        assertEquals(List.of(open1, open2), output.rooms());
        assertEquals(List.of(open1, open2), presenter.presentedRooms.get());
    }

    @Test
    void shouldReturnEmptyListWhenNoOpenRooms()
    {
        roomRepo.add(new StudyRoom("r1", "A", "L1", 10, RoomStatus.CLOSED));

        var output = useCase.execute(new ListRoomsUseCase.Request());

        assertTrue(output.rooms().isEmpty());
        assertTrue(presenter.presentedRooms.get().isEmpty());
    }

    @Test
    void shouldQueryRepoWithOpenStatusOnly()
    {
        roomRepo.add(new StudyRoom("r1", "A", "L1", 10, RoomStatus.OPEN));

        useCase.execute(new ListRoomsUseCase.Request());

        assertEquals(RoomStatus.OPEN, roomRepo.lastQueriedStatus.get());
    }

    // --- Stubs ---

    static class StubRoomRepo implements RoomRepository
    {
        private final java.util.Map<String, StudyRoom> rooms = new java.util.LinkedHashMap<>();
        final AtomicReference<RoomStatus> lastQueriedStatus = new AtomicReference<>();

        void add(StudyRoom... toAdd)
        {
            for (StudyRoom r : toAdd)
            {
                rooms.put(r.id(), r);
            }
        }

        @Override
        public List<StudyRoom> findByStatus(RoomStatus status)
        {
            lastQueriedStatus.set(status);
            return rooms.values().stream()
                    .filter(r -> r.status() == status)
                    .toList();
        }

        @Override
        public Optional<StudyRoom> findById(String id)
        {
            return Optional.ofNullable(rooms.get(id));
        }

        @Override
        public StudyRoom save(StudyRoom room)
        {
            rooms.put(room.id(), room);
            return room;
        }
    }

    static class StubPresenter implements ListRoomsUseCase.Presenter
    {
        final AtomicReference<List<StudyRoom>> presentedRooms = new AtomicReference<>();

        @Override
        public void presentRooms(List<StudyRoom> rooms)
        {
            presentedRooms.set(rooms);
        }
    }
}
```

- [ ] **步骤 2:运行测试,确认失败**

运行:`mvn -pl SeatAndRoom -am test -Dtest=ListRoomsUseCaseTest -q`
预期:编译错误 —— `ListRoomsUseCase` 不存在。

- [ ] **步骤 3:写最小实现**

```java
package org.cleancoders.seatandroom.usecase;

import jakarta.inject.Inject;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.outbound.RoomRepository;

import java.util.List;

/**
 * UC-04: 获取所有 OPEN 状态的自习室。
 * <p>
 * 公开用例（不继承 AuthUseCase，无认证要求）。
 */
public class ListRoomsUseCase
{

    @Inject
    RoomRepository roomRepo;

    @Inject
    Presenter presenter;

    public Output execute(Request request)
    {
        List<StudyRoom> rooms = roomRepo.findByStatus(RoomStatus.OPEN);
        presenter.presentRooms(rooms);
        return new Output(rooms);
    }

    public record Request()
    {
    }

    public record Output(List<StudyRoom> rooms)
    {
    }

    public interface Presenter
    {
        void presentRooms(List<StudyRoom> rooms);
    }
}
```

- [ ] **步骤 4:运行测试,确认通过**

运行:`mvn -pl SeatAndRoom -am test -Dtest=ListRoomsUseCaseTest -q`
预期:Tests run: 3, Failures: 0, Errors: 0。

- [ ] **步骤 5:提交**

```bash
git add SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/ListRoomsUseCase.java \
        SeatAndRoom/src/test/java/org/cleancoders/seatandroom/usecase/ListRoomsUseCaseTest.java
git commit -m "feat(SeatAndRoom): 实现 UC-04 ListRoomsUseCase

公开用例,返回所有 OPEN 状态自习室。附 3 个单元测试。

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## 任务 4:InMemoryRoomRepo(TDD)

**文件：**
- 新增:`Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/InMemoryRoomRepo.java`
- 测试:`Infrastructure/src/test/java/org/cleancoders/infrastructure/persistence/InMemoryRoomRepoTest.java`

**接口：**
- 消费:`RoomRepository`(任务 2)、`StudyRoom`/`RoomStatus`(任务 1)。
- 产出:`InMemoryRoomRepo`(infrastructure 实现,`@Singleton`)。

- [ ] **步骤 1:写失败测试**

```java
package org.cleancoders.infrastructure.persistence;

import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryRoomRepoTest
{

    private InMemoryRoomRepo repo;

    @BeforeEach
    void setUp()
    {
        repo = new InMemoryRoomRepo();
    }

    @Test
    void saveShouldStoreAndReturnRoom()
    {
        StudyRoom room = new StudyRoom("r1", "A", "L1", 10, RoomStatus.OPEN);

        StudyRoom saved = repo.save(room);

        assertSame(room, saved);
        assertEquals(Optional.of(room), repo.findById("r1"));
    }

    @Test
    void findByIdShouldReturnEmptyWhenMissing()
    {
        assertTrue(repo.findById("nope").isEmpty());
    }

    @Test
    void findByStatusShouldFilterByStatus()
    {
        StudyRoom open = new StudyRoom("r1", "A", "L1", 10, RoomStatus.OPEN);
        StudyRoom closed = new StudyRoom("r2", "B", "L2", 10, RoomStatus.CLOSED);
        StudyRoom open2 = new StudyRoom("r3", "C", "L3", 10, RoomStatus.OPEN);
        repo.save(open);
        repo.save(closed);
        repo.save(open2);

        var result = repo.findByStatus(RoomStatus.OPEN);

        assertEquals(2, result.size());
        assertTrue(result.contains(open));
        assertTrue(result.contains(open2));
        assertFalse(result.contains(closed));
    }

    @Test
    void findByStatusShouldReturnEmptyWhenNoMatch()
    {
        repo.save(new StudyRoom("r1", "A", "L1", 10, RoomStatus.CLOSED));

        assertTrue(repo.findByStatus(RoomStatus.OPEN).isEmpty());
    }
}
```

- [ ] **步骤 2:运行测试,确认失败**

运行:`mvn -pl Infrastructure -am test -Dtest=InMemoryRoomRepoTest -q`
预期:编译错误 —— `InMemoryRoomRepo` 不存在。

- [ ] **步骤 3:写最小实现**

```java
package org.cleancoders.infrastructure.persistence;

import jakarta.inject.Singleton;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.outbound.RoomRepository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link RoomRepository}.
 */
@Singleton
public class InMemoryRoomRepo implements RoomRepository
{

    private final ConcurrentHashMap<String, StudyRoom> rooms = new ConcurrentHashMap<>();

    @Override
    public List<StudyRoom> findByStatus(RoomStatus status)
    {
        return rooms.values().stream()
                .filter(r -> r.status() == status)
                .toList();
    }

    @Override
    public Optional<StudyRoom> findById(String id)
    {
        return Optional.ofNullable(rooms.get(id));
    }

    @Override
    public StudyRoom save(StudyRoom room)
    {
        rooms.put(room.id(), room);
        return room;
    }
}
```

- [ ] **步骤 4:运行测试,确认通过**

运行:`mvn -pl Infrastructure -am test -Dtest=InMemoryRoomRepoTest -q`
预期:Tests run: 4, Failures: 0, Errors: 0。

- [ ] **步骤 5:提交**

```bash
git add Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/InMemoryRoomRepo.java \
        Infrastructure/src/test/java/org/cleancoders/infrastructure/persistence/InMemoryRoomRepoTest.java
git commit -m "feat(Infrastructure): 新增 InMemoryRoomRepo 实现 RoomRepository

@Singleton, ConcurrentHashMap 存储。附 4 个单元测试。

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## 任务 5:RoomResponse / RoomListResponse DTO + WebApiRoomPresenter

**文件：**
- 新增:`WebApi/src/main/java/org/cleancoders/web/dto/room/RoomResponse.java`
- 新增:`WebApi/src/main/java/org/cleancoders/web/dto/room/RoomListResponse.java`
- 新增:`WebApi/src/main/java/org/cleancoders/web/presenter/WebApiRoomPresenter.java`

**接口：**
- 消费:`ListRoomsUseCase.Presenter.presentRooms(List<StudyRoom>)`(任务 3)、`StudyRoom`/`RoomStatus`(任务 1)、`WebApiPresenter`(已有基类,含 ThreadLocal `current` + `getResponse()`)。
- 产出:`RoomResponse` 单条 DTO;`RoomListResponse` 列表包装 DTO(`rooms` 字段);`WebApiRoomPresenter`(继承 `WebApiPresenter`,实现 `ListRoomsUseCase.Presenter`,返回 `RoomListResponse` 包装体)。

- [ ] **步骤 1:创建 `RoomResponse.java`**

```java
package org.cleancoders.web.dto.room;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cleancoders.seatandroom.domain.RoomStatus;

@Schema(description = "自习室信息")
public record RoomResponse(
        @Schema(description = "自习室ID", example = "r1")
        String id,
        @Schema(description = "名称", example = "A 自习室")
        String name,
        @Schema(description = "位置", example = "1号楼2层")
        String location,
        @Schema(description = "容量", example = "10")
        int capacity,
        @Schema(description = "状态", example = "OPEN", allowableValues = {"OPEN", "CLOSED", "MAINTENANCE"})
        RoomStatus status
)
{
}
```

- [ ] **步骤 2:创建 `RoomListResponse.java`**

响应体是一个包装对象 `{"rooms": [...]}`,而非裸数组。

```java
package org.cleancoders.web.dto.room;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "自习室列表响应")
public record RoomListResponse(
        @Schema(description = "OPEN 状态自习室列表(可为空)")
        List<RoomResponse> rooms
)
{
}
```

- [ ] **步骤 3:创建 `WebApiRoomPresenter.java`**

注意:`ListRoomsUseCase` 是公开用例(无 auth 分支),所以这里直接继承 `WebApiPresenter` —— 而非 `WebApiCommonPresenter`(后者会继承一堆 auth 相关接口实现,这里用不上)。响应体用 `RoomListResponse` 包装,而非裸数组。

```java
package org.cleancoders.web.presenter;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.usecase.ListRoomsUseCase;
import org.cleancoders.web.dto.room.RoomListResponse;
import org.cleancoders.web.dto.room.RoomResponse;

import java.util.List;

/**
 * WebApi presenter for {@link ListRoomsUseCase}. Public use case — no auth
 * branches, so extends {@link WebApiPresenter} directly rather than
 * {@link WebApiCommonPresenter}. Response body is a {@link RoomListResponse}
 * wrapper object ({"rooms": [...]}) rather than a bare array.
 */
@Singleton
public class WebApiRoomPresenter extends WebApiPresenter implements
        ListRoomsUseCase.Presenter
{

    @Override
    public void presentRooms(List<StudyRoom> rooms)
    {
        List<RoomResponse> dtos = rooms.stream()
                .map(r -> new RoomResponse(r.id(), r.name(), r.location(), r.capacity(), r.status()))
                .toList();
        current.set(Response.ok(new RoomListResponse(dtos)).build());
    }
}
```

- [ ] **步骤 4:验证 WebApi 可编译**

运行:`mvn -pl WebApi -am compile -q`
预期:BUILD SUCCESS。

- [ ] **步骤 5:提交**

```bash
git add WebApi/src/main/java/org/cleancoders/web/dto/room/RoomResponse.java \
        WebApi/src/main/java/org/cleancoders/web/dto/room/RoomListResponse.java \
        WebApi/src/main/java/org/cleancoders/web/presenter/WebApiRoomPresenter.java
git commit -m "feat(WebApi): 新增 RoomResponse/RoomListResponse DTO 与 WebApiRoomPresenter

公开用例 presenter,直接继承 WebApiPresenter(无 auth 分支)。
响应体为 RoomListResponse 包装对象({\"rooms\":[...]}),非裸数组。

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## 任务 6:RoomResource(TDD —— 单元测试)

**文件：**
- 新增:`WebApi/src/main/java/org/cleancoders/web/resource/RoomResource.java`
- 测试:`WebApi/src/test/java/org/cleancoders/web/resource/RoomResourceTest.java`

**接口：**
- 消费:`ListRoomsUseCase`(任务 3)、`WebApiRoomPresenter`(任务 5)。
- 产出:`RoomResource` JAX-RS 端点 `GET /rooms`。

- [ ] **步骤 1:写失败测试**

模式对齐 `ReservationResourceTest`:直接 new Resource,注入一个匿名 `ListRoomsUseCase` 子类(记录调用、返回预设 `Output`),预先 stage presenter 响应,断言 HTTP 行为。

```java
package org.cleancoders.web.resource;

import jakarta.ws.rs.core.Response;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.usecase.ListRoomsUseCase;
import org.cleancoders.web.presenter.WebApiRoomPresenter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoomResourceTest
{

    private RoomResource resource;
    private WebApiRoomPresenter presenter;
    private boolean executeCalled;
    private ListRoomsUseCase.Output outputToReturn;

    @BeforeEach
    void setUp()
    {
        presenter = new WebApiRoomPresenter();
        executeCalled = false;
        outputToReturn = null;

        resource = new RoomResource();
        resource.presenter = presenter;
        resource.listRoomsUseCase = new ListRoomsUseCase()
        {
            @Override
            public Output execute(Request request)
            {
                executeCalled = true;
                return outputToReturn;
            }
        };
    }

    @Test
    void listShouldDelegateToUseCase()
    {
        outputToReturn = new ListRoomsUseCase.Output(List.of());

        resource.listRooms();

        assertTrue(executeCalled);
    }

    @Test
    void listShouldReturn200WithRooms()
    {
        StudyRoom open = new StudyRoom("r1", "A", "L1", 10, RoomStatus.OPEN);
        outputToReturn = new ListRoomsUseCase.Output(List.of(open));
        presenter.presentRooms(List.of(open));

        Response response = resource.listRooms();

        assertEquals(200, response.getStatus());
        assertNotNull(response.getEntity());
    }

    @Test
    void listShouldReturn200AndEmptyBodyWhenNoRooms()
    {
        outputToReturn = new ListRoomsUseCase.Output(List.of());
        presenter.presentRooms(List.of());

        Response response = resource.listRooms();

        assertEquals(200, response.getStatus());
        assertNotNull(response.getEntity());
    }
}
```

- [ ] **步骤 2:运行测试,确认失败**

运行:`mvn -pl WebApi -am test -Dtest=RoomResourceTest -q`
预期:编译错误 —— `RoomResource` 不存在。

- [ ] **步骤 3:写最小实现**

```java
package org.cleancoders.web.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cleancoders.seatandroom.usecase.ListRoomsUseCase;
import org.cleancoders.web.dto.room.RoomListResponse;
import org.cleancoders.web.presenter.WebApiRoomPresenter;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Room", description = "自习室相关接口")
public class RoomResource
{

    @Inject
    ListRoomsUseCase listRoomsUseCase;

    @Inject
    WebApiRoomPresenter presenter;

    @GET
    @Operation(summary = "获取所有 OPEN 状态的自习室 (UC-04)", description = "公开接口,返回当前状态为 OPEN 的全部自习室。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回 OPEN 状态自习室列表(可为空)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = RoomListResponse.class)))
    })
    public Response listRooms()
    {
        listRoomsUseCase.execute(new ListRoomsUseCase.Request());
        return presenter.getResponse();
    }
}
```

说明:Resource 里不需要 `List<RoomResponse>` 的 import(响应实体由 presenter 构建)。若编译器提示 import 未使用可移除 —— 但 `@Schema(implementation = RoomResponse.class)` 的引用使其仍被使用。

- [ ] **步骤 4:运行测试,确认通过**

运行:`mvn -pl WebApi -am test -Dtest=RoomResourceTest -q`
预期:Tests run: 3, Failures: 0, Errors: 0。

- [ ] **步骤 5:提交**

```bash
git add WebApi/src/main/java/org/cleancoders/web/resource/RoomResource.java \
        WebApi/src/test/java/org/cleancoders/web/resource/RoomResourceTest.java
git commit -m "feat(WebApi): 新增 RoomResource GET /api/rooms (UC-04)

公开端点,委托 ListRoomsUseCase。附 3 个单元测试。

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## 任务 7:AppBinder 绑定

**文件：**
- 修改:`WebApi/src/main/java/org/cleancoders/web/binder/AppBinder.java`

**接口：**
- 消费:`ListRoomsUseCase`、`WebApiRoomPresenter`、`InMemoryRoomRepo`、`RoomRepository`(任务 3–6)。

- [ ] **步骤 1:在 `AppBinder.configure()` 中添加绑定**

添加 import(按现有 import 块字母序插入):

```java
import org.cleancoders.infrastructure.persistence.InMemoryRoomRepo;
import org.cleancoders.seatandroom.outbound.RoomRepository;
import org.cleancoders.seatandroom.usecase.ListRoomsUseCase;
import org.cleancoders.web.presenter.WebApiRoomPresenter;
```

在 `configure()` 内,SeatAndRoom 段之后(Reservation UseCases 段之前)添加:

```java
        // === SeatAndRoom UseCases ===
        bind(ListRoomsUseCase.class).to(ListRoomsUseCase.class);

        // === SeatAndRoom Presenters ===
        WebApiRoomPresenter roomPresenterInstance = new WebApiRoomPresenter();
        bind(roomPresenterInstance).to(WebApiRoomPresenter.class);
        bind(roomPresenterInstance).to(ListRoomsUseCase.Presenter.class);

        // === SeatAndRoom Repositories ===
        bind(InMemoryRoomRepo.class).to(RoomRepository.class).in(Singleton.class);
```

- [ ] **步骤 2:验证 WebApi 可编译**

运行:`mvn -pl WebApi -am compile -q`
预期:BUILD SUCCESS。

- [ ] **步骤 3:提交**

```bash
git add WebApi/src/main/java/org/cleancoders/web/binder/AppBinder.java
git commit -m "feat(WebApi): AppBinder 绑定 ListRoomsUseCase/WebApiRoomPresenter/InMemoryRoomRepo

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## 任务 8:RoomResourceIntegrationTest(JerseyTest)

**文件：**
- 新增:`WebApi/src/test/java/org/cleancoders/web/resource/RoomResourceIntegrationTest.java`

**接口：**
- 消费:`RoomResource`、`ListRoomsUseCase`、`WebApiRoomPresenter`、`InMemoryRoomRepo`(任务 3–7)。用 `JerseyTest` + `ResourceConfig` + 内联 `AbstractBinder`(对齐 `AuthResourceIntegrationTest`)。

- [ ] **步骤 1:写集成测试**

```java
package org.cleancoders.web.resource;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cleancoders.infrastructure.persistence.InMemoryRoomRepo;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.outbound.RoomRepository;
import org.cleancoders.seatandroom.usecase.ListRoomsUseCase;
import org.cleancoders.web.presenter.WebApiRoomPresenter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RoomResourceIntegrationTest extends JerseyTest
{

    private InMemoryRoomRepo roomRepo;

    @Override
    protected Application configure()
    {
        roomRepo = new InMemoryRoomRepo();
        WebApiRoomPresenter presenterInstance = new WebApiRoomPresenter();

        ResourceConfig config = new ResourceConfig();
        config.register(RoomResource.class);
        config.register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bind(roomRepo).to(RoomRepository.class);
                bind(ListRoomsUseCase.class).to(ListRoomsUseCase.class);
                bind(presenterInstance).to(WebApiRoomPresenter.class);
                bind(presenterInstance).to(ListRoomsUseCase.Presenter.class);
            }
        });
        return config;
    }

    @BeforeEach
    public void setUp() throws Exception
    {
        super.setUp();
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    @Test
    void shouldReturn200WithOnlyOpenRooms()
    {
        roomRepo.save(new StudyRoom("r1", "A", "L1", 10, RoomStatus.OPEN));
        roomRepo.save(new StudyRoom("r2", "B", "L2", 10, RoomStatus.CLOSED));
        roomRepo.save(new StudyRoom("r3", "C", "L3", 10, RoomStatus.OPEN));
        roomRepo.save(new StudyRoom("r4", "D", "L4", 10, RoomStatus.MAINTENANCE));

        Response response = target("/rooms").request(MediaType.APPLICATION_JSON).get();

        assertEquals(200, response.getStatus());
        // 响应体是包装对象 {"rooms": [...]},从中取 rooms 数组
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rooms = (List<Map<String, Object>>) body.get("rooms");
        assertEquals(2, rooms.size());
        List<String> ids = rooms.stream().map(m -> (String) m.get("id")).toList();
        assertTrue(ids.contains("r1"));
        assertTrue(ids.contains("r3"));
        assertFalse(ids.contains("r2"));
        assertFalse(ids.contains("r4"));
    }

    @Test
    void shouldReturn200WithEmptyArrayWhenNoOpenRooms()
    {
        roomRepo.save(new StudyRoom("r1", "A", "L1", 10, RoomStatus.CLOSED));

        Response response = target("/rooms").request(MediaType.APPLICATION_JSON).get();

        assertEquals(200, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        @SuppressWarnings("unchecked")
        List<?> rooms = (List<?>) body.get("rooms");
        assertTrue(rooms.isEmpty());
    }
}
```

- [ ] **步骤 2:运行测试,确认通过**

运行:`mvn -pl WebApi -am test -Dtest=RoomResourceIntegrationTest -q`
预期:Tests run: 2, Failures: 0, Errors: 0。

(若测试以启动错误失败,检查 `RoomResource` 已注册、内联 `AbstractBinder` 内四项绑定齐全。)

- [ ] **步骤 3:提交**

```bash
git add WebApi/src/test/java/org/cleancoders/web/resource/RoomResourceIntegrationTest.java
git commit -m "test(WebApi): RoomResource 集成测试 (JerseyTest)

端到端验证 GET /api/rooms 只返回 OPEN 房间、空列表场景。

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## 任务 9:全量构建验证

**文件:**无。

- [ ] **步骤 1:跑完整多模块测试套件**

运行:`mvn -am test -q`
预期:BUILD SUCCESS;所有模块编译通过、所有测试通过。

- [ ] **步骤 2:若有模块编译失败**

可能原因:某处现有引用把 `StudyRoom.status` 当 `String`。全仓搜索:

运行:`grep -rn "StudyRoom" --include=*.java`,检查任何读取 `.status()` 时按 `String` 处理的文件,改为按 `RoomStatus` 处理。

- [ ] **步骤 3:无需提交(仅验证)**

若全部通过,功能完成。若需修复,用清晰 message 提交所做调整。
