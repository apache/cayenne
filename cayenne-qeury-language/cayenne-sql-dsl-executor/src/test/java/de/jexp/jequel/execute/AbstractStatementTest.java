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
import de.jexp.jequel.table.BaseTable;
import de.jexp.jequel.table.Field;
import junit.framework.TestCase;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractStatementTest extends TestCase {
    public static ARTICLE ARTICLE = new ARTICLE();

    public static class ARTICLE extends BaseTable<ARTICLE> {
        public Field<Integer> OID = integer().primaryKey();
        public Field NAME = string();
        public Field<Integer> ARTICLE_NO = integer();

        {
            initFields();
        }
    }

    protected static ExecutableStatementFactory EXECUTABLE_STATEMENT_FACTORY =
            new DelegatingExecutableStatementFactory(new SpringExecutableStatementFactory());

    protected Sql articleSql = Sql.Select(ARTICLE.OID).from(ARTICLE).where(ARTICLE.OID.isNotNull()).toSql();
    protected ExecutableStatement executableStatement;

    private DataSource dataSource;

    protected void setDataSource(ResultSet rs) {
        try {
            this.dataSource = mock(DataSource.class);
            Connection connection = mock(Connection.class);
            Statement statement = mock(Statement.class);
            when(statement.executeQuery(anyString())).thenReturn(rs);
            when(statement.getResultSet()).thenReturn(rs);

            when(statement.executeUpdate(anyString())).thenReturn(0);

            when(connection.createStatement()).thenReturn(statement);
            when(this.dataSource.getConnection()).thenReturn(connection);
            when(this.dataSource.getConnection(anyString(), anyString())).thenReturn(connection);


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

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
        IntegerRowCallbackHandler integerRowCallbackHandler = new IntegerRowCallbackHandler();
        executableStatement.processRows(integerRowCallbackHandler);
        assertEquals(10, integerRowCallbackHandler.getResult());
    }

    public void testMapBeansSimple() {
        Collection<Double> doubles = executableStatement.mapBeans(new BeanRowMapper<OidBean, Double>() {
            public Double mapBean(OidBean oidBean) {
                return oidBean.getOid().doubleValue();
            }
        });
        assertEquals(1, doubles.size());
        assertEquals(10d, doubles.iterator().next());
    }

    public void testMapBeansToBean() {
        Collection<DoubleBean> doubleBeans = executableStatement.mapBeans(new OidBeanRowMapper());
        assertEquals(1, doubleBeans.size());
        assertEquals(10d, doubleBeans.iterator().next().getSomeValue());
    }

    public void testMapValuesSimple() {
        Collection<Double> doubleBeans = executableStatement.mapValues(new ValueRowMapper<Double>() {
            public Double mapValue(int oid) {
                return (double) oid;
            }
        });
        assertEquals(10d, doubleBeans.iterator().next());
    }

    public void testHandleBeansSimple() {
        final Collection<Double> results = new ArrayList<Double>();
        executableStatement.handleBeans(new BeanRowHandler<OidBean>() {
            public void handleBean(OidBean oidBean) {
                results.add(oidBean.getOid().doubleValue());
            }
        });
        assertEquals(1, results.size());
        assertEquals(10d, results.iterator().next());
    }

    public void testHandleValuesSimple() {
        final Collection<Double> results = new ArrayList<Double>();
        executableStatement.handleValues(new ValueRowHandler() {
            public void handleValue(int oid) {
                results.add((double) oid);
            }
        });
        assertEquals(1, results.size());
        assertEquals(10d, results.iterator().next());
    }

    protected static class IntegerRowMapper implements RowMapper {
        public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
            return rs.getInt("OID");
        }
    }

    protected static class IntegerRowCallbackHandler implements RowCallbackHandler {
        public Integer result;

        public void processRow(ResultSet rs) throws SQLException {
            result = rs.getInt("OID");
        }

        public int getResult() {
            return result;
        }
    }

    private static class OidBeanRowMapper implements BeanRowMapper<OidBean, DoubleBean> {
        public DoubleBean mapBean(OidBean oidBean) {
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