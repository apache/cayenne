package org.apache.cayenne.unit.di.server;

import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.server.DataSourceFactory;
import org.apache.cayenne.di.Inject;

import javax.sql.DataSource;

public class ServerCaseSharedDataSourceFactory implements DataSourceFactory {

    private ServerCaseDataSourceFactory factory;

    public ServerCaseSharedDataSourceFactory(@Inject ServerCaseDataSourceFactory factory) {
    }

    public DataSource getDataSource(DataNodeDescriptor nodeDescriptor) throws Exception {
        return factory.getSharedDataSource();
    }
}
