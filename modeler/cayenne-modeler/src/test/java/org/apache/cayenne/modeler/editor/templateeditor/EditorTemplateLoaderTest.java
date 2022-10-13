package org.apache.cayenne.modeler.editor.templateeditor;

import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.TemplateType;
import org.apache.cayenne.modeler.editor.cgen.templateeditor.EditorTemplateLoader;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class EditorTemplateLoaderTest {

    private CgenConfiguration configuration;
    private EditorTemplateLoader loader;
    private static final String CUSTOM_TPL = "Custom tpl";

    @Before
    public void createCgenConfiguration (){
        this.configuration = new CgenConfiguration();
        this.loader = new EditorTemplateLoader(configuration,null);

    }

    @Test
    public void testLoadCustom(){
        configuration.setTemplate(CUSTOM_TPL);
        String customTemplate = loader.load(TemplateType.ENTITY_SUBCLASS, false);
        assertEquals(CUSTOM_TPL, customTemplate);

    }

    @Test
    public void testLoadDefault(){
        configuration.setSuperTemplate(TemplateType.ENTITY_SUPERCLASS.pathFromSourceRoot());
        String defaultTemplateText = getDefaultTemplateText(TemplateType.ENTITY_SUPERCLASS);
        String defaultTemplate = loader.load(TemplateType.ENTITY_SUPERCLASS, true);
        assertEquals(defaultTemplateText, defaultTemplate);
    }

    @Test
    public void testLoadSingleDefault(){
        configuration.setSuperTemplate(TemplateType.ENTITY_SUBCLASS.pathFromSourceRoot());
        configuration.setMakePairs(false);
        String defaultTemplateText = getDefaultTemplateText(TemplateType.ENTITY_SINGLE_CLASS);
        String defaultTemplate = loader.load(TemplateType.ENTITY_SUBCLASS, true);
        assertEquals(defaultTemplateText, defaultTemplate);
    }



    private String getDefaultTemplateText(TemplateType type) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(type.pathFromSourceRoot())) {
            if (stream != null) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException | ResourceNotFoundException ignored) {
        }
        return null;
    }

}
