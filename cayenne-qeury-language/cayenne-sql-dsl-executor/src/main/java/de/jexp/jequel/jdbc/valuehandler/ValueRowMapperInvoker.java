package de.jexp.jequel.jdbc.valuehandler;


import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author mh14 @ jexp.de
 * @since 03.11.2007 08:03:09 (c) 2007 jexp.de
 */
@SuppressWarnings({"unchecked"})
public class ValueRowMapperInvoker<O> extends ValueRowProcessorInvoker {
    public ValueRowMapperInvoker(final ValueRowMapper<O> valueRowMapper, final ResultSet rs) throws SQLException {
        super(valueRowMapper, rs, ValueRowMapper.MAP_VALUE_METHOD);
    }

    public static <O> ValueRowMapperInvoker<O> createInvoker(final ValueRowMapper<O> valueRowMapper, final ResultSet rs) throws SQLException {
        return new ValueRowMapperInvoker<O>(valueRowMapper, rs);
    }

    public O mapValues(final ResultSet rs) throws SQLException {
        return (O) processRow(rs);
    }
}
