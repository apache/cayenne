package org.objectstyle.cayenne.modeler.action;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.objectstyle.cayenne.ConfigurationException;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conf.RuntimeLoadDelegate;
import org.objectstyle.cayenne.modeler.util.ModelerDbAdapter;

/**
 * Project loader delegate customized for use in CayenneModeler.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class ModelerProjectLoadDelegate extends RuntimeLoadDelegate {

    public ModelerProjectLoadDelegate(Configuration configuration) {
        super(configuration, configuration.getLoadStatus());
    }

    protected void initAdapter(DataNode node, String adapterName) {
        node.setAdapter(new ModelerDbAdapter(adapterName, node.getDataSource()));
    }

    public void shouldLoadDataDomain(String domainName) {
        super.shouldLoadDataDomain(domainName);

        try {
            // disable class indexing
            findDomain(domainName).getEntityResolver().setIndexedByClass(false);
        }
        catch (Exception ex) {
            throw new ConfigurationException("Domain is not loaded: " + domainName);
        }
    }

    public void shouldLoadDataDomainProperties(String domainName, Map properties) {

        // remove factory property to avoid instatiation attempts for unknown/invalid
        // classes

        Map propertiesClone = new HashMap(properties);
        Object dataContextFactory = propertiesClone
                .remove(DataDomain.DATA_CONTEXT_FACTORY_PROPERTY);

        super.shouldLoadDataDomainProperties(domainName, propertiesClone);

        // stick property back in...
        if (dataContextFactory != null) {
            try {
                findDomain(domainName).getProperties().put(
                        DataDomain.DATA_CONTEXT_FACTORY_PROPERTY,
                        dataContextFactory);
            }
            catch (Exception ex) {
                throw new ConfigurationException("Domain is not loaded: " + domainName);
            }
        }
    }

    /**
     * Creates a subclass of the DataNode that does not decorate its DataSource, exposing
     * the version that was set on it.
     */
    protected DataNode createDataNode(String nodeName) {
        return new DataNode(nodeName) {

            public DataSource getDataSource() {
                return dataSource;
            }
        };
    }
}
