package de.jexp.jequel.util;

public class ClassUtils {
    public static <T> T newInstance(String typeName) {
        try {
            return ((Class<T>) Class.forName(typeName)).newInstance();
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Could not instantiate " + typeName, e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Could not instantiate " + typeName, e);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not instantiate " + typeName, e);
        }
    }
}
