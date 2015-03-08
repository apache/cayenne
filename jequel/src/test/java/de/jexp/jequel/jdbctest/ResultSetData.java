package de.jexp.jequel.jdbctest;

import java.sql.ResultSetMetaData;

/**
 * @author mh14 @ jexp.de
 * @since 03.11.2007 14:01:00 (c) 2007 jexp.de
 */
public interface ResultSetData {
    <T> T get(Class<T> returnType, int columnIndex);

    <T> T get(Class<T> returnType, String columnName);

    boolean next();

    ResultSetMetaData getMetaData();

    String getColumnName(int col);

    int getColumnType(int col);

    Class getColumnClass(int col);

    int getColumnCount();
}
