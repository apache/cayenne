package de.jexp.jequel.jdbc.valuehandler;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author mh14 @ jexp.de
 * @since 02.11.2007 20:35:52 (c) 2007 jexp.de
 */
public class ResultSetValueRowMapper<O> implements RowMapper {
    private final ValueRowMapper<O> valueRowMapper;
    private ValueRowMapperInvoker<O> valueRowMapperInvoker;

    public ResultSetValueRowMapper(final ValueRowMapper<O> valueRowMapper) {
        this.valueRowMapper = valueRowMapper;

    }

    public Object mapRow(final ResultSet rs, final int rowNum) throws SQLException {
        return getValueRowMapperInvoker(valueRowMapper, rs).mapValues(rs);
    }

    private ValueRowMapperInvoker<O> getValueRowMapperInvoker(final ValueRowMapper<O> valueRowMapper, final ResultSet rs) throws SQLException {
        if (valueRowMapperInvoker == null) {
            valueRowMapperInvoker = ValueRowMapperInvoker.createInvoker(valueRowMapper, rs);
        }
        return valueRowMapperInvoker;
    }
}