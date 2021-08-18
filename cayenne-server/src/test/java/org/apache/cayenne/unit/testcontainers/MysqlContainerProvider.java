package org.apache.cayenne.unit.testcontainers;

import java.util.Calendar;

import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.mysql.MySQLAdapter;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

public class MysqlContainerProvider extends TestContainerProvider {

    @Override
    JdbcDatabaseContainer<?> createContainer(DockerImageName dockerImageName) {
        return new MySQLContainer<>(dockerImageName)
                .withUrlParam("useUnicode", "true")
                .withUrlParam("characterEncoding", "UTF-8")
                .withUrlParam("generateSimpleParameterMetadata", "true")
                .withUrlParam("useLegacyDatetimeCode", "false")
                .withUrlParam("serverTimezone", Calendar.getInstance().getTimeZone().getID())
                .withCommand("--character-set-server=utf8mb4")
                .withCommand("--max-allowed-packet=5242880");
    }

    @Override
    String getDockerImage() {
        return "mysql:8";
    }

    @Override
    public Class<? extends JdbcAdapter> getAdapterClass() {
        return MySQLAdapter.class;
    }
}
