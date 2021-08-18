package org.apache.cayenne.unit.testcontainers;

import java.time.Duration;

import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.db2.DB2Adapter;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

public class Db2ContainerProvider extends TestContainerProvider {

    @Override
    public JdbcDatabaseContainer<?> startContainer(String version) {
        JdbcDatabaseContainer<?> container = super.startContainer(version);
        // need to wait to ensure Oracle DB has started
        try {
            Thread.sleep(40000);
        } catch (InterruptedException ignored) {
        }
        return container;
    }

    @Override
    JdbcDatabaseContainer<?> createContainer(DockerImageName dockerImageName) {
        return new Db2Container(dockerImageName)
                .withStartupTimeout(Duration.ofMinutes(15))
                .withDatabaseName("testdb")
                .acceptLicense();
    }

    @Override
    String getDockerImage() {
        return "ibmcom/db2";
    }

    @Override
    public Class<? extends JdbcAdapter> getAdapterClass() {
        return DB2Adapter.class;
    }
}
