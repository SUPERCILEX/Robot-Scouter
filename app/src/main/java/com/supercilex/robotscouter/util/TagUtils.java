package com.supercilex.robotscouter.util;

public class TagUtils {
    public static <T> String getTag(T clazz) {
        return clazz.getClass().getSimpleName();
    }
}
