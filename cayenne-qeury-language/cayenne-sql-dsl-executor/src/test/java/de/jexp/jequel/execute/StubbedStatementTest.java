package de.jexp.jequel.execute;

import de.jexp.jequel.jdbctest.ResultSetStub;

public class StubbedStatementTest extends AbstractStatementTest {
    protected void setUp() throws Exception {
        setDataSource(new ResultSetStub(new Object[]{10}, "OID"));
        executableStatement = EXECUTABLE_STATEMENT_FACTORY.createExecutableStatement(getDataSource(), articleSql);
    }

    protected void tearDown() throws Exception {
    }
}