package org.cleancoders.userandauth.domain;

/**
 * 用户聚合根。
 * <p>
 * 统计字段（reservationCount / studyHours / checkInCount / creditScore）
 * 初始值均为 0，信用分默认为 100。
 */
public record User(
        String id,
        String username,
        String password,
        UserRole role,
        String name,
        String email,
        int reservationCount,
        int studyHours,
        int checkInCount,
        int creditScore,
        boolean banned
)
{
    /** 向后兼容的辅助构造函数：统计字段默认为 0，信用分默认为 100，默认未封禁。 */
    public User(String id, String username, String password, UserRole role, String name, String email)
    {
        this(id, username, password, role, name, email, 0, 0, 0, 100, false);
    }
}
