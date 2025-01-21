package com.example.chosim.chosim.domain.maimu.enums;

public enum MaimuColor {
    GREEN,
    YELLOW,
    RED;

    public static MaimuColor fromString(String color) {
        try {
            return MaimuColor.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("잘못된 색상 값입니다: " + color);
        }
    }
}
