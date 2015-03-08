package de.jexp.jequel.execute;

import de.jexp.jequel.execute.spring.SpringExecutableStatementFactory;
import de.jexp.jequel.expression.Expressions;
import de.jexp.jequel.jdbc.beanprocessor.BeanRowHandler;
import de.jexp.jequel.jdbc.beanprocessor.BeanRowMapper;
import de.jexp.jequel.jdbc.valuehandler.ValueRowHandler;
import de.jexp.jequel.jdbc.valuehandler.ValueRowMapper;
import de.jexp.jequel.sql.DelegatingExecutableStatementFactory;
import de.jexp.jequel.sql.ExecutableStatementFactory;
import de.jexp.jequel.sql.Sql;
import junit.framework.TestCase;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static de.jexp.jequel.tables.TEST_TABLES.ARTICLE;

public abstract class AbstractStatementTest extends TestCase {
    protected static final ExecutableStatementFactory EXECUTABLE_STATEMENT_FACTORY =
            new DelegatingExecutableStatementFactory(new SpringExecutableStatementFactory());

    protected Sql articleSql = Sql.Select(ARTICLE.OID).from(ARTICLE).where(ARTICLE.OID.isNot(Expressions.NULL)).toSql();
    protected DataSource dataSource;
    protected ExecutableStatement executableStatement;

    public Sql getArticleSql() {
        return articleSql;
    }

    public void testSql() {
        assertEquals(getExpectedSql(), executableStatement.toString());
    }

    protected String getExpectedSql() {
        return "select ARTICLE.OID from ARTICLE where ARTICLE.OID is not NULL";
    }

    public void testGetValue() {
        assertEquals(10, executableStatement.getValue());
    }

    public void testMapRows() {
        assertEquals(Arrays.asList(10), executableStatement.mapRows(new IntegerRowMapper()));
    }

    public void testHandleRows() {
        final IntegerRowCallbackHandler integerRowCallbackHandler = new IntegerRowCallbackHandler();
        executableStatement.processRows(integerRowCallbackHandler);
        assertEquals(10, integerRowCallbackHandler.getResult());
    }

    public void testMapBeansSimple() {
        final Collection<Double> doubles = executableStatement.mapBeans(new BeanRowMapper<OidBean, Double>() {
            public Double mapBean(final OidBean oidBean) {
                return oidBean.getOid().doubleValue();
            }
        });
        assertEquals(1, doubles.size());
        assertEquals(10d, doubles.iterator().next());
    }

    public void testMapBeansToBean() {
        final Collection<DoubleBean> doubleBeans = executableStatement.mapBeans(new OidBeanRowMapper());
        assertEquals(1, doubleBeans.size());
        assertEquals(10d, doubleBeans.iterator().next().getSomeValue());
    }

    public void testMapValuesSimple() {
        final Collection<Double> doubleBeans = executableStatement.mapValues(new ValueRowMapper<Double>() {
            public Double mapValue(final int oid) {
                return (double) oid;
            }
        });
        assertEquals(10d, doubleBeans.iterator().next());
    }

    public void testHandleBeansSimple() {
        final Collection<Double> results = new ArrayList<Double>();
        executableStatement.handleBeans(new BeanRowHandler<OidBean>() {
            public void handleBean(final OidBean oidBean) {
                results.add(oidBean.getOid().doubleValue());
            }
        });
        assertEquals(1, results.size());
        assertEquals(10d, results.iterator().next());
    }

    public void testHandleValuesSimple() {
        final Collection<Double> results = new ArrayList<Double>();
        executableStatement.handleValues(new ValueRowHandler() {
            public void handleValue(final int oid) {
                results.add((double) oid);
            }
        });
        assertEquals(1, results.size());
        assertEquals(10d, results.iterator().next());
    }

    protected static class IntegerRowMapper implements RowMapper {
        public Integer mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return rs.getInt("OID");
        }
    }

    protected static class IntegerRowCallbackHandler implements RowCallbackHandler {
        public Integer result;

        public void processRow(final ResultSet rs) throws SQLException {
            result = rs.getInt("OID");
        }

        public int getResult() {
            return result;
        }
    }

    private static class OidBeanRowMapper implements BeanRowMapper<OidBean, DoubleBean> {
        public DoubleBean mapBean(final OidBean oidBean) {
            final double oidValue = oidBean.getOid().doubleValue();
            return new DoubleBean() {
                public Double getSomeValue() {
                    return oidValue;
                }
            };
        }
    }

    public interface OidBean {
        Number getOid();
    }

    public interface DoubleBean {
        Double getSomeValue();
    }
}