package org.apache.cayenne.unit.testcontainers;

import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.mysql.MySQLAdapter;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

public class MariaDbContainerProvider extends TestContainerProvider {

    @Override
    JdbcDatabaseContainer<?> createContainer(DockerImageName dockerImageName) {
        return new MariaDBContainer<>(dockerImageName);
    }

    @Override
    String getDockerImage() {
        return "mariadb:10.3";
    }

    @Override
    public Class<? extends JdbcAdapter> getAdapterClass() {
        return MySQLAdapter.class;
    }
}
