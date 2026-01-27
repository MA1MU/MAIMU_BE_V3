package com.example.chosim.chosim.domain.maimu.enums;

public enum MaimuColor {
    GREEN,
    YELLOW,
    RED;

    public static MaimuColor fromString(String color) {
        try {
            return MaimuColor.valueOf(color.toUpperCase()); //valueOf(): 이름이 정확하게 일치하는 Enum상수 찾기
            // return값 예시 :  MaimuColor.GREEN
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("잘못된 색상 값입니다: " + color);
        }
    }
}
