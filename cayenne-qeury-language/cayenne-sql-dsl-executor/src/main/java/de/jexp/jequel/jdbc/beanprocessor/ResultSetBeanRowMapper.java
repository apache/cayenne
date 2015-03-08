package de.jexp.jequel.jdbc.beanprocessor;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author mh14 @ jexp.de
 * @since 02.11.2007 20:35:52 (c) 2007 jexp.de
 */
public class ResultSetBeanRowMapper<I, O> implements RowMapper {
    private final BeanRowMapper<I, O> beanRowMapper;
    private I beanWrapper;

    public ResultSetBeanRowMapper(final BeanRowMapper<I, O> beanRowMapper) {
        this.beanRowMapper = beanRowMapper;
    }

    public Object mapRow(final ResultSet rs, final int rowNum) throws SQLException {
        return beanRowMapper.mapBean(getBeanWrapper(rs));
    }

    protected I getBeanWrapper(final ResultSet rs) throws SQLException {
        if (beanWrapper == null) {
            beanWrapper = ResultSetBeanWrapper.createBeanMapperWrapper(rs, beanRowMapper);
        }
        return beanWrapper;
    }
}
