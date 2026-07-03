package org.cleancoders.infrastructure.persistence.testdata;

import org.cleancoders.infrastructure.persistence.InMemoryUserRepo;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.domain.UserRole;

/**
 * {@link InMemoryUserRepo} pre-seeded with test users.
 */
public class TestDataUserRepo extends InMemoryUserRepo {

    public TestDataUserRepo() {
        // Admin user
        save(new User(null, "admin", "admin123", UserRole.ADMIN, "系统管理员", "admin@example.com"));
        // Student users
        save(new User(null, "zhangsan", "pass123", UserRole.STUDENT, "张三", "zhangsan@example.com"));
        save(new User(null, "lisi", "pass123", UserRole.STUDENT, "李四", "lisi@example.com"));
        save(new User(null, "wangwu", "pass123", UserRole.STUDENT, "王五", "wangwu@example.com"));
        save(new User(null, "zhaoliu", "pass123", UserRole.STUDENT, "赵六", "zhaoliu@example.com"));
    }
}