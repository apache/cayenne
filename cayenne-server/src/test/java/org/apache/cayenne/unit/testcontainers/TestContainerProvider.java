package org.apache.cayenne.unit.testcontainers;

import org.apache.cayenne.dba.JdbcAdapter;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class TestContainerProvider {

    abstract JdbcDatabaseContainer<?> createContainer(DockerImageName dockerImageName);

    abstract String getDockerImage();

    public abstract Class<? extends JdbcAdapter> getAdapterClass();

    public JdbcDatabaseContainer<?> startContainer(String version) {
        DockerImageName dockerImageName = DockerImageName.parse(getDockerImage());
        if(version != null) {
            dockerImageName = dockerImageName.withTag(version);
        }
        JdbcDatabaseContainer<?> container = createContainer(dockerImageName);
        container.start();
        return container;
    }

}
