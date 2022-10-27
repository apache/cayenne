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

package org.apache.cayenne.modeler.editor.cgen.templateeditor;

import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.CgenTemplate;
import org.apache.cayenne.gen.TemplateType;

/**
 * since 4.3
 */
public class EditorTemplateSaver {

    private final CgenConfiguration configuration;


    public EditorTemplateSaver(CgenConfiguration configuration) {
        this.configuration = configuration;
    }

    public void save(TemplateType type, Boolean isTemplateDefault, String templateText) {
        if (configuration.isMakePairs() || !isTemplateDefault) {
            setTemplate(type, isTemplateDefault, templateText);
        } else {
            setDefaultSingleTemplate(type);
        }
    }

    private void setTemplate(TemplateType templateType, Boolean isDefault, String templateText) {

        String template;
        if (isDefault) {
            template = templateType.pathFromSourceRoot();
        } else {
            template = templateText;
        }

        switch (templateType) {
            case ENTITY_SUPERCLASS: {
                configuration.setSuperTemplate(new CgenTemplate(template, false, TemplateType.ENTITY_SUPERCLASS));
                break;
            }
            case ENTITY_SUBCLASS: {
                configuration.setTemplate(new CgenTemplate(template, false, TemplateType.ENTITY_SUBCLASS));
                break;
            }
            case EMBEDDABLE_SUPERCLASS: {
                configuration.setEmbeddableSuperTemplate(new CgenTemplate(template, false, TemplateType.EMBEDDABLE_SUPERCLASS));
                break;
            }
            case EMBEDDABLE_SUBCLASS: {
                configuration.setEmbeddableTemplate(new CgenTemplate(template, false, TemplateType.EMBEDDABLE_SUBCLASS));
                break;
            }
            case DATAMAP_SUPERCLASS: {
                configuration.setDataMapSuperTemplate(new CgenTemplate(template, false, TemplateType.DATAMAP_SUPERCLASS));
                break;
            }
            case DATAMAP_SUBCLASS: {
                configuration.setDataMapTemplate(new CgenTemplate(template, false, TemplateType.DATAMAP_SUBCLASS));
                break;
            }
            default:
                throw new IllegalStateException("Illegal template type for for nonSingle template " + templateType);
        }
    }

    private void setDefaultSingleTemplate(TemplateType templateType) {
        switch (templateType) {
            case ENTITY_SUBCLASS: {
                configuration.setTemplate(TemplateType.ENTITY_SINGLE_CLASS.defaultTemplate());
                break;
            }
            case EMBEDDABLE_SUBCLASS: {
                configuration.setEmbeddableTemplate(TemplateType.EMBEDDABLE_SINGLE_CLASS.defaultTemplate());
                break;
            }
            case DATAMAP_SUBCLASS: {
                configuration.setDataMapTemplate(TemplateType.DATAMAP_SINGLE_CLASS.defaultTemplate());
                break;
            }
            default:
                throw new IllegalStateException("Illegal template type for single template " + templateType);
        }
    }

}
