package de.jexp.jequel.jdbc.valuehandler;


import java.sql.ResultSet;
import java.sql.SQLException;

public class ValueRowMapperInvoker<O> extends ValueRowProcessorInvoker {
    public ValueRowMapperInvoker(ValueRowMapper<O> valueRowMapper, ResultSet rs) throws SQLException {
        super(valueRowMapper, rs, ValueRowMapper.MAP_VALUE_METHOD);
    }

    public static <O> ValueRowMapperInvoker<O> createInvoker(ValueRowMapper<O> valueRowMapper, ResultSet rs) throws SQLException {
        return new ValueRowMapperInvoker<O>(valueRowMapper, rs);
    }

    public O mapValues(final ResultSet rs) throws SQLException {
        return (O) processRow(rs);
    }
}
