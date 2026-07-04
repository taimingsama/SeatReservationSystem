package org.cleancoders.infrastructure.persistence.testdata;

import org.cleancoders.infrastructure.persistence.InMemoryUserRepo;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.domain.UserRole;

/**
 * {@link InMemoryUserRepo} pre-seeded with test users.
 * Uses fixed IDs so {@link TestDataReservationRepo} can reference them.
 */
public class TestDataUserRepo extends InMemoryUserRepo {

    public static final String ADMIN = "user-admin";
    public static final String ZHANGSAN = "user-zhangsan";
    public static final String LISI = "user-lisi";
    public static final String WANGWU = "user-wangwu";
    public static final String ZHAOLIU = "user-zhaoliu";

    public TestDataUserRepo() {
        save(new User(ADMIN, "admin", "admin123", UserRole.ADMIN, "系统管理员", "admin@example.com"));
        save(new User(ZHANGSAN, "zhangsan", "pass123", UserRole.STUDENT, "张三", "zhangsan@example.com"));
        save(new User(LISI, "lisi", "pass123", UserRole.STUDENT, "李四", "lisi@example.com"));
        save(new User(WANGWU, "wangwu", "pass123", UserRole.STUDENT, "王五", "wangwu@example.com"));
        save(new User(ZHAOLIU, "zhaoliu", "pass123", UserRole.STUDENT, "赵六", "zhaoliu@example.com"));
    }
}
