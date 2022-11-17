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
import org.apache.cayenne.gen.CgenTemplate;
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
        configuration.setTemplate(new CgenTemplate(CUSTOM_TPL,false,TemplateType.ENTITY_SUBCLASS));
        String customTemplate = loader.load(TemplateType.ENTITY_SUBCLASS, false);
        assertEquals(CUSTOM_TPL, customTemplate);

    }

    @Test
    public void testLoadDefault(){
        configuration.setSuperTemplate(TemplateType.ENTITY_SUPERCLASS.defaultTemplate());
        String defaultTemplateText = getDefaultTemplateText(TemplateType.ENTITY_SUPERCLASS);
        String defaultTemplate = loader.load(TemplateType.ENTITY_SUPERCLASS, true);
        assertEquals(defaultTemplateText, defaultTemplate);
    }

    @Test
    public void testLoadSingleDefault(){
        configuration.setSuperTemplate(TemplateType.ENTITY_SUBCLASS.defaultTemplate());
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
