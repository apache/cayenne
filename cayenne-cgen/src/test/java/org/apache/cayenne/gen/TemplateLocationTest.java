package org.apache.cayenne.gen;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;

public class TemplateLocationTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private CgenConfiguration cgenConfiguration;
    private ClassGenerationAction action;
    private TemplateType templateType;

    @Before
    public void setUp() {
        cgenConfiguration = new CgenConfiguration(false);
        action = new ClassGenerationAction(cgenConfiguration);
        templateType = TemplateType.ENTITY_SUBCLASS;
    }

    @Test
    public void upperLevel() throws Exception {
        cgenConfiguration.setRootPath(tempFolder.newFolder().toPath());
        tempFolder.newFile("testTemplate.vm");
        cgenConfiguration.setTemplate("../testTemplate.vm");
        assertNotNull(action.getTemplate(templateType));
    }

    @Test
    public void sameLevel() throws Exception {
        cgenConfiguration.setRootPath(tempFolder.getRoot().toPath());
        tempFolder.newFile("testTemplate2.vm");
        cgenConfiguration.setTemplate("testTemplate2.vm");
        assertNotNull(action.getTemplate(templateType));
    }

    @Test
    public void aboveLevel() throws Exception {
        cgenConfiguration.setRootPath(Paths.get(tempFolder.getRoot().getParent()));
        tempFolder.newFile("testTemplate3.vm");
        cgenConfiguration.setTemplate(tempFolder.getRoot() + "/testTemplate3.vm");
        assertNotNull(action.getTemplate(templateType));
    }
}
