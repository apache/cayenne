package de.jexp.jequel.execute;


import de.jexp.jequel.jdbctest.ProxyTestResultSet;
import de.jexp.jequel.jdbctest.TestDataSource;

import java.sql.ResultSet;
import java.util.Arrays;

/**
 * @author mh14 @ jexp.de
 * @since 03.11.2007 14:39:47 (c) 2007 jexp.de
 */
public class ProxiedStatementTest extends AbstractStatementTest {
    protected void setUp() throws Exception {
        final ResultSet rs = ProxyTestResultSet.createTestResultSet(Arrays.asList(10), "OID");
        dataSource = new TestDataSource(rs);
        executableStatement = EXECUTABLE_STATEMENT_FACTORY.createExecutableStatement(dataSource, articleSql);
    }

    protected void tearDown() throws Exception {
    }
}
