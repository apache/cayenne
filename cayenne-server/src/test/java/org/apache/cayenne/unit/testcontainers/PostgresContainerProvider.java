package org.apache.cayenne.unit.testcontainers;

import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.postgres.PostgresAdapter;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class PostgresContainerProvider extends TestContainerProvider {
    @Override
    JdbcDatabaseContainer<?> createContainer(DockerImageName dockerImageName) {
        return new PostgreSQLContainer<>(dockerImageName);
    }

    @Override
    String getDockerImage() {
        return "postgres:9";
    }

    @Override
    public Class<? extends JdbcAdapter> getAdapterClass() {
        return PostgresAdapter.class;
    }
}
