package org.apache.cayenne.datasource;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ManagedPoolingDataSourceIT {

	private int poolSize;
	private Map<ExpiringConnection, Object> connections;
	private UnmanagedPoolingDataSource unmanagedPool;
	private ManagedPoolingDataSource managedPool;

	@Before
	public void before() throws SQLException {

		this.poolSize = 3;
		this.connections = new ConcurrentHashMap<>();

		DataSource mockDataSource = mock(DataSource.class);
		when(mockDataSource.getConnection()).thenAnswer(new Answer<Connection>() {

			@Override
			public Connection answer(InvocationOnMock invocation) throws Throwable {
				return createMockConnection();
			}
		});

		PoolingDataSourceParameters parameters = new PoolingDataSourceParameters();
		parameters.setMaxConnections(poolSize);
		parameters.setMinConnections(poolSize - 1);
		parameters.setMaxQueueWaitTime(500);
		parameters.setValidationQuery("SELECT 1");
		this.unmanagedPool = new UnmanagedPoolingDataSource(mockDataSource, parameters);

		this.managedPool = new ManagedPoolingDataSource(unmanagedPool, 10000);
	}

	private Connection createMockConnection() throws SQLException {
		ExpiringConnection connectionWrapper = new ExpiringConnection();
		connections.put(connectionWrapper, 1);
		return connectionWrapper.mockConnection;
	}

	@After
	public void after() {
		if (managedPool != null) {
			managedPool.close();
		}
	}

	private void expireConnections() throws SQLException {
		Iterator<ExpiringConnection> it = connections.keySet().iterator();
		while (it.hasNext()) {
			ExpiringConnection c = it.next();
			it.remove();
			c.expire();
		}
	}

	@Test
	public void testAbruptReset() throws SQLException {

		assertTrue(managedPool.poolSize() > 0);
		assertTrue(managedPool.availableSize() > 0);

		// make sure conn
		expireConnections();

		// CAY-2067 ... this should work on an invalid pool
		assertNotNull(managedPool.getConnection());
	}

	static class ExpiringConnection {

		private Connection mockConnection;
		private OngoingStubbing<Statement> createStatementMock;

		ExpiringConnection() throws SQLException {
			this.mockConnection = mock(Connection.class);
			this.createStatementMock = when(mockConnection.createStatement());

			createStatementMock.thenAnswer(new Answer<Statement>() {
				@Override
				public Statement answer(InvocationOnMock invocation) throws Throwable {

					ResultSet mockRs = mock(ResultSet.class);
					when(mockRs.next()).thenReturn(true, false, false, false);

					Statement mockStatement = mock(Statement.class);
					when(mockStatement.executeQuery(anyString())).thenReturn(mockRs);
					return mockStatement;
				}
			});
		}

		void expire() throws SQLException {
			createStatementMock.thenThrow(new SQLException("Expired"));
		}
	}
}
