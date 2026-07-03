package org.cleancoders.seatandroom.domain;

/**
 * 系统预设的教室布局模板。
 * 每种布局定义了固定的座位数量，教室创建后不可更改。
 */
public enum RoomLayout {
    SMALL("小教室", 40),
    MEDIUM("中教室", 60),
    LARGE("大教室", 100);

    private final String displayName;
    private final int seatCount;

    RoomLayout(String displayName, int seatCount) {
        this.displayName = displayName;
        this.seatCount = seatCount;
    }

    public String displayName() {
        return displayName;
    }

    public int seatCount() {
        return seatCount;
    }
}
