/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.editor.templateeditor;

import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.TemplateType;
import org.apache.cayenne.modeler.editor.cgen.templateeditor.TemplateEditorView;
import org.apache.cayenne.modeler.editor.cgen.templateeditor.EditorTemplateSaver;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class EditorTemplateSaverTest {

    private CgenConfiguration configuration;
    private EditorTemplateSaver saver;
    private static final String CUSTOM_TPL = "Custom tpl";

    @Before
    public void config(){
        this.configuration = new CgenConfiguration();
        this.saver = new EditorTemplateSaver(configuration);
    }

    @Test
    public void testSaveCustom(){
        configuration.setMakePairs(true);
        saver.save(TemplateType.ENTITY_SUBCLASS,false, CUSTOM_TPL);
        String customTemplate = configuration.getTemplate().getData();
        assertEquals(CUSTOM_TPL, customTemplate);
    }

    @Test
    public void testSaveDefault(){
        configuration.setMakePairs(true);
        saver.save(TemplateType.ENTITY_SUPERCLASS,true, CUSTOM_TPL);
        assertEquals( TemplateType.ENTITY_SUPERCLASS.pathFromSourceRoot(),configuration.getSuperTemplate().getData());
    }

    @Test
    public void testSaveSingleDefault(){
        configuration.setMakePairs(false);
        saver.save(TemplateType.ENTITY_SUBCLASS,true, CUSTOM_TPL);
        assertEquals(TemplateType.ENTITY_SINGLE_CLASS.pathFromSourceRoot(),configuration.getTemplate().getData() );
    }

}
