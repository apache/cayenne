package de.jexp.jequel.jdbc.valuehandler;

/**
 *  Implement a method O mapValue(int oid, String name, BigDecimal price)
 */
public interface ValueRowMapper<O> extends ValueRowProcessor {
    String MAP_VALUE_METHOD = "mapValue";
}