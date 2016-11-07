package com.supercilex.robotscouter.util;

public class BaseHelper {
    protected static <T> String getTag(T clazz) {
        return clazz.getClass().getSimpleName();
    }
}
