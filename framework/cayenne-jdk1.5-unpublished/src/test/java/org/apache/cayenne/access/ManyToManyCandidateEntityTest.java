package org.apache.cayenne.access;

import junit.framework.TestCase;
import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.DefaultConfigurationNameMapper;
import org.apache.cayenne.configuration.XMLDataChannelDescriptorLoader;
import org.apache.cayenne.configuration.XMLDataMapLoader;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.resource.URLResource;

import java.net.URL;
import java.util.ArrayList;

/**
 *
 */
public class ManyToManyCandidateEntityTest extends TestCase {

    private DataMap map;

    @Override
    public void setUp() throws Exception {
        Module testModule = new Module() {

            public void configure(Binder binder) {
                binder.bind(DataMapLoader.class).to(XMLDataMapLoader.class);
                binder.bind(ConfigurationNameMapper.class).to(
                        DefaultConfigurationNameMapper.class);
            }
        };

        Injector injector = DIBootstrap.createInjector(testModule);

        // create and initialize loader instance to test
        XMLDataChannelDescriptorLoader loader = new XMLDataChannelDescriptorLoader();
        injector.injectMembers(loader);

        String testConfigName = "relationship-optimisation";
        URL url = getClass().getResource("cayenne-" + testConfigName + ".xml");

        ConfigurationTree<DataChannelDescriptor> tree = loader.load(new URLResource(url));

        map = tree.getRootNode().getDataMap(testConfigName);
    }

    public void testMatchingForManyToManyEntity() throws Exception {
        ObjEntity manyToManyEntity = map.getObjEntity("Table1Table2");

        assertEquals(true, new ManyToManyCandidateEntity(manyToManyEntity).isRepresentManyToManyTable());
    }

    public void testMatchingForNotManyToManyEntity() throws Exception {
        ObjEntity entity = map.getObjEntity("Table1");

        assertEquals(false, new ManyToManyCandidateEntity(entity).isRepresentManyToManyTable());
    }

    public void testOptimisationForManyToManyEntity() {
        ObjEntity manyToManyEntity = map.getObjEntity("Table1Table2");

        new ManyToManyCandidateEntity(manyToManyEntity).optimizeRelationships();

        ObjEntity table1Entity = map.getObjEntity("Table1");
        ObjEntity table2Entity = map.getObjEntity("Table2");

        assertEquals(1, table1Entity.getRelationships().size());
        assertEquals(table2Entity, new ArrayList<Relationship>(table1Entity.getRelationships()).get(0)
                .getTargetEntity());

        assertEquals(1, table2Entity.getRelationships().size());
        assertEquals(table1Entity, new ArrayList<Relationship>(table2Entity.getRelationships()).get(0)
                .getTargetEntity());
    }

}
