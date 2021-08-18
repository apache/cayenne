package org.apache.cayenne.unit.testcontainers;

import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.sqlserver.SQLServerAdapter;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

public class SqlServerContainerProvider extends TestContainerProvider {
    @Override
    JdbcDatabaseContainer<?> createContainer(DockerImageName dockerImageName) {
        return new MSSQLServerContainer<>(dockerImageName)
                .acceptLicense();
    }

    @Override
    String getDockerImage() {
        return "mcr.microsoft.com/mssql/server";
    }

    @Override
    public Class<? extends JdbcAdapter> getAdapterClass() {
        return SQLServerAdapter.class;
    }
}
