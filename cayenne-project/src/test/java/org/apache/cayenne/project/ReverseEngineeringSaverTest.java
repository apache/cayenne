package org.apache.cayenne.project;

import org.apache.cayenne.configuration.*;
import org.apache.cayenne.dbimport.ReverseEngineering;
import org.apache.cayenne.di.*;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.project.unit.Project2Case;
import org.apache.cayenne.resource.URLResource;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;

import static org.junit.Assert.assertTrue;

/**
 * @since 4.0
 */
public class ReverseEngineeringSaverTest extends Project2Case {
    private FileProjectSaver saver;

    @Before
    public void setUp() throws Exception {
        Module testModule = new Module() {

            public void configure(Binder binder) {
                binder.bind(ConfigurationNameMapper.class).to(
                        DefaultConfigurationNameMapper.class);
            }
        };

        saver = new FileProjectSaver();
        Injector injector = DIBootstrap.createInjector(testModule);
        injector.injectMembers(saver);
    }

    @Test
    public void testSaveReversEngineering() throws MalformedURLException {
        File testFolder = setupTestDirectory("testSaverRE");

        DataChannelDescriptor rootNode = new DataChannelDescriptor();
        rootNode.setName("test");

        DataMap dataMap = new DataMap("datamap1");
        dataMap.setReverseEngineering(new ReverseEngineering("reverseEngineering1"));
        rootNode.getDataMaps().add(dataMap);

        Project project = new Project(new ConfigurationTree<>(rootNode));
        saver.saveAs(project, new URLResource(testFolder.toURL()));

        DataMapLoader dataMapLoader = new XMLDataMapLoader();
        DataMap dataMap1 = dataMapLoader.load(dataMap.getConfigurationSource());
        String reverseEngineeringName = dataMap1.getReverseEngineering().getName();

        File reFile = new File(testFolder, reverseEngineeringName + ".xml");
        assertTrue(reFile.exists());
        assertTrue(reFile.length() > 0);
    }


}
