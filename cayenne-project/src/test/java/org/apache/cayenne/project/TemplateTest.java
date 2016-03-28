package org.apache.cayenne.project;

import org.apache.cayenne.map.template.ClassTemplate;
import org.apache.cayenne.configuration.*;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.project.unit.Project2Case;
import org.apache.cayenne.resource.URLResource;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;

import static junit.framework.TestCase.assertTrue;

/**
 * @since 4.0
 */
public class TemplateTest extends Project2Case {
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
    public void testSaveTemplate() throws MalformedURLException {
        File testFolder = setupTestDirectory("testSaverRE");

        DataChannelDescriptor rootNode = new DataChannelDescriptor();
        rootNode.setName("test");

        DataMap dataMap = new DataMap("datamap1");
        ClassTemplate classTemplate = new ClassTemplate();
        classTemplate.setName("template");
        dataMap.getClassGenerationDescriptor().getTemplates().put(classTemplate.getName(), classTemplate);
        classTemplate.setDataMap(dataMap);
        rootNode.getDataMaps().add(dataMap);

        Project project = new Project(new ConfigurationTree<>(rootNode));
        saver.saveAs(project, new URLResource(testFolder.toURL()));

        DataMapLoader dataMapLoader = new XMLDataMapLoader();
        DataMap dataMap1 = dataMapLoader.load(dataMap.getConfigurationSource());
        String templateName = dataMap1.getClassGenerationDescriptor().getTemplates().get(classTemplate.getName()).getName();

        File templateFile = new File(testFolder, templateName + ".vm");
        assertTrue(templateFile.exists());
    }
}
