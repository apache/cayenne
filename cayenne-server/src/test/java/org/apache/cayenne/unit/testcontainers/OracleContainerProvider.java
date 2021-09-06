package org.apache.cayenne.unit.testcontainers;

import java.time.Duration;

import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.oracle.OracleAdapter;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.utility.DockerImageName;

public class OracleContainerProvider extends TestContainerProvider {

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
        return new OracleContainer(dockerImageName)
                .withStartupTimeout(Duration.ofMinutes(5))
                .withEnv("ORACLE_ALLOW_REMOTE", "true")
                .withEnv("ORACLE_DISABLE_ASYNCH_IO", "true");
    }

    @Override
    String getDockerImage() {
        return "oracleinanutshell/oracle-xe-11g";
    }

    @Override
    public Class<? extends JdbcAdapter> getAdapterClass() {
        return OracleAdapter.class;
    }
}
