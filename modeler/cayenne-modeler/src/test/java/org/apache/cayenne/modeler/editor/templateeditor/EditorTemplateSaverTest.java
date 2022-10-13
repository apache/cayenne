package org.apache.cayenne.modeler.editor.templateeditor;

import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.TemplateType;
import org.apache.cayenne.modeler.editor.cgen.templateeditor.TemplateEditorView;
import org.apache.cayenne.modeler.editor.cgen.templateeditor.EditorTemplateSaver;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class EditorTemplateSaverTest {

    private CgenConfiguration configuration;
    private EditorTemplateSaver saver;
    private static final String CUSTOM_TPL = "Custom tpl";

    @Before
    public void createCgenConfiguration (){
        this.configuration = new CgenConfiguration();
        TemplateEditorView editorView = new TemplateEditorView(new ArrayList<>());
        editorView.getEditingTemplatePane().setText(CUSTOM_TPL);
        this.saver = new EditorTemplateSaver(configuration,editorView);

    }


    @Test
    public void testSaveCustom(){
        configuration.setMakePairs(true);
        saver.save(TemplateType.ENTITY_SUBCLASS,false);
        String customTemplate = configuration.getTemplate();
        assertEquals(CUSTOM_TPL, customTemplate);
    }

    @Test
    public void testSaveDefault(){
        configuration.setMakePairs(true);
        saver.save(TemplateType.ENTITY_SUPERCLASS,true);
        assertEquals(configuration.getSuperTemplate(), TemplateType.ENTITY_SUPERCLASS.pathFromSourceRoot());
    }

    @Test
    public void testSaveSingleDefault(){
        configuration.setMakePairs(false);
        saver.save(TemplateType.ENTITY_SUBCLASS,true);
        assertEquals(configuration.getTemplate(), TemplateType.ENTITY_SINGLE_CLASS.pathFromSourceRoot());

    }

}
