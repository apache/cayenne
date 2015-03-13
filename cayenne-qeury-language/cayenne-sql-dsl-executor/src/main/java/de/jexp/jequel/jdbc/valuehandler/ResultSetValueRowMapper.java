package de.jexp.jequel.jdbc.valuehandler;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ResultSetValueRowMapper<O> implements RowMapper {
    private final ValueRowMapper<O> valueRowMapper;
    private ValueRowMapperInvoker<O> valueRowMapperInvoker;

    public ResultSetValueRowMapper(ValueRowMapper<O> valueRowMapper) {
        this.valueRowMapper = valueRowMapper;

    }

    public Object mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return getValueRowMapperInvoker(valueRowMapper, resultSet).mapValues(resultSet);
    }

    private ValueRowMapperInvoker<O> getValueRowMapperInvoker(ValueRowMapper<O> valueRowMapper, ResultSet rs) throws SQLException {
        if (valueRowMapperInvoker == null) {
            valueRowMapperInvoker = ValueRowMapperInvoker.createInvoker(valueRowMapper, rs);
        }
        return valueRowMapperInvoker;
    }
}