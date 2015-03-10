package de.jexp.jequel.execute;


import de.jexp.jequel.jdbctest.ProxyTestResultSet;

import javax.sql.DataSource;
import java.util.Arrays;

public class ProxiedStatementTest extends AbstractStatementTest {
    protected void setUp() throws Exception {
        setDataSource(ProxyTestResultSet.createTestResultSet(Arrays.asList(10), "OID"));
        executableStatement = EXECUTABLE_STATEMENT_FACTORY.createExecutableStatement(getDataSource(), articleSql);
    }

    protected void tearDown() throws Exception {
    }
}
