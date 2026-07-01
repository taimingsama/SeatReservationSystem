package org.cleancoders.common.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserTest
{

    @Test
    void userRecordShouldStoreAllFields()
    {
        User user = new User("u1", "alice", "hashedpw", UserRole.STUDENT, "Alice", "alice@example.com");

        assertEquals("u1", user.id());
        assertEquals("alice", user.username());
        assertEquals("hashedpw", user.password());
        assertEquals(UserRole.STUDENT, user.role());
        assertEquals("Alice", user.name());
        assertEquals("alice@example.com", user.email());
    }

    @Test
    void userRoleEnumShouldHaveStudentAndAdmin()
    {
        assertEquals(2, UserRole.values().length);
        assertEquals(UserRole.STUDENT, UserRole.valueOf("STUDENT"));
        assertEquals(UserRole.ADMIN, UserRole.valueOf("ADMIN"));
    }

    @Test
    void userRecordsWithSameFieldsShouldBeEqual()
    {
        User u1 = new User("u1", "alice", "pw", UserRole.STUDENT, "Alice", "a@b.com");
        User u2 = new User("u1", "alice", "pw", UserRole.STUDENT, "Alice", "a@b.com");
        assertEquals(u1, u2);
        assertEquals(u1.hashCode(), u2.hashCode());
    }
}
