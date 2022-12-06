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
package org.apache.cayenne.gen.xml;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.configuration.xml.NamespaceAwareNestedTagHandler;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.CgenConfigList;
import org.apache.cayenne.gen.CgenTemplate;
import org.apache.cayenne.gen.TemplateType;
import org.apache.cayenne.map.DataMap;
import org.xml.sax.Attributes;

/**
 * @since 4.1
 */
public class CgenConfigHandler extends NamespaceAwareNestedTagHandler {

    public static final String CONFIG_TAG = "cgen";

    private static final String NAME = "name";
    private static final String OUTPUT_DIRECTORY_TAG = "destDir";
    private static final String GENERATION_MODE_TAG = "mode";
    private static final String SUBCLASS_TEMPLATE_TAG = "template";
    private static final String SUPERCLASS_TEMPLATE_TAG = "superTemplate";
    private static final String EMBEDDABLE_TEMPLATE_TAG = "embeddableTemplate";
    private static final String EMBEDDABLE_SUPER_TEMPLATE_TAG = "embeddableSuperTemplate";
    private static final String DATAMAP_TEMPLATE_TAG = "dataMapTemplate";
    private static final String DATAMAP_SUPER_TEMPLATE_TAG = "dataMapSuperTemplate";
    private static final String OUTPUT_PATTERN_TAG = "outputPattern";
    private static final String MAKE_PAIRS_TAG = "makePairs";
    private static final String USE_PKG_PATH_TAG = "usePkgPath";
    private static final String OVERWRITE_SUBCLASSES_TAG = "overwrite";
    private static final String CREATE_PROPERTY_NAMES_TAG = "createPropertyNames";
    private static final String EXCLUDE_ENTITIES_TAG = "excludeEntities";
    private static final String EXCLUDE_EMBEDDABLES_TAG = "excludeEmbeddables";
    private static final String CREATE_PK_PROPERTIES = "createPKProperties";
    private static final String SUPER_PKG_TAG = "superPkg";

    public static final String TRUE = "true";

    private DataChannelMetaData metaData;
    private CgenConfiguration configuration;

    CgenConfigHandler(NamespaceAwareNestedTagHandler parentHandler, DataChannelMetaData metaData) {
        super(parentHandler);
        this.metaData = metaData;
        this.targetNamespace = CgenExtension.NAMESPACE;
        this.configuration = new CgenConfiguration();
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) {
        if (CONFIG_TAG.equals(localName)) {
            createConfig();
            return true;
        }
        return false;
    }

    @Override
    protected void processCharData(String localName, String data) {
        switch (localName) {
            case NAME:
                setName(data);
                break;
            case OUTPUT_DIRECTORY_TAG:
                createOutputDir(data);
                break;
            case GENERATION_MODE_TAG:
                createGenerationMode(data);
                break;
            case EXCLUDE_ENTITIES_TAG:
                createExcludeEntities(data);
                break;
            case EXCLUDE_EMBEDDABLES_TAG:
                createExcludeEmbeddables(data);
                break;
            case SUBCLASS_TEMPLATE_TAG:
                createSubclassTemplate(data);
                break;
            case SUPERCLASS_TEMPLATE_TAG:
                createSuperclassTemplate(data);
                break;
            case EMBEDDABLE_TEMPLATE_TAG:
                createEmbeddableTemplate(data);
                break;
            case EMBEDDABLE_SUPER_TEMPLATE_TAG:
                createEmbeddableSuperTemplate(data);
            case DATAMAP_TEMPLATE_TAG:
                createDataMapTemplate(data);
                break;
            case DATAMAP_SUPER_TEMPLATE_TAG:
                createDataMapSuperTemplate(data);
                break;
            case OUTPUT_PATTERN_TAG:
                createOutputPattern(data);
                break;
            case MAKE_PAIRS_TAG:
                createMakePairs(data);
                break;
            case USE_PKG_PATH_TAG:
                createUsePkgPath(data);
                break;
            case OVERWRITE_SUBCLASSES_TAG:
                createOverwriteSubclasses(data);
                break;
            case CREATE_PROPERTY_NAMES_TAG:
                createPropertyNamesTag(data);
                break;
            case CREATE_PK_PROPERTIES:
                createPkPropertiesTag(data);
                break;
            case SUPER_PKG_TAG:
                createSuperPkg(data);
                break;
        }
    }

    private void setName(String name) {
        if (name.trim().length() == 0) {
            return;
        }
        configuration.setName(name);
    }

    private void createOutputDir(String path) {
        if (path.trim().length() == 0) {
            return;
        }
        configuration.setRelativePath(Paths.get(path));
    }

    private void createGenerationMode(String mode) {
        if (mode.trim().length() == 0) {
            return;
        }
        configuration.setArtifactsGenerationMode(mode);
    }

    private void createExcludeEntities(String entities) {
        if (entities.trim().length() == 0) {
            return;
        }
        configuration.loadEntities(entities);
    }

    private void createExcludeEmbeddables(String embeddables) {
        if (embeddables.trim().length() == 0) {
            return;
        }
        configuration.loadEmbeddables(embeddables);
    }

    private void createSubclassTemplate(String template) {

        if (template.trim().length() == 0) {
            return;
        }
        if (TemplateType.isDefault(template)) {
            configuration.setTemplate(TemplateType.ENTITY_SUBCLASS.defaultTemplate());
        } else {
            configuration.setTemplate(new CgenTemplate(template, false,TemplateType.ENTITY_SUBCLASS));
        }
    }

    private void createSuperclassTemplate(String template) {
        if (template.trim().length() == 0) {
            return;
        }
        if (TemplateType.isDefault(template)) {
            configuration.setSuperTemplate(TemplateType.ENTITY_SUPERCLASS.defaultTemplate());
        } else {
            configuration.setSuperTemplate(new CgenTemplate(template,false,TemplateType.ENTITY_SUBCLASS));
        }
    }

    private void createEmbeddableTemplate(String template) {
        if (template.trim().length() == 0) {
            return;
        }
        if (TemplateType.isDefault(template)) {
            configuration.setEmbeddableTemplate(TemplateType.EMBEDDABLE_SUBCLASS.defaultTemplate());
        } else {
            configuration.setEmbeddableTemplate(new CgenTemplate(template,false,TemplateType.EMBEDDABLE_SUBCLASS));
        }
    }

    private void createEmbeddableSuperTemplate(String template) {
        if (template.trim().length() == 0) {
            return;
        }
        if (TemplateType.isDefault(template)) {
            configuration.setEmbeddableSuperTemplate(TemplateType.EMBEDDABLE_SUPERCLASS.defaultTemplate());
        } else {
            configuration.setEmbeddableSuperTemplate(new CgenTemplate(template,false,TemplateType.EMBEDDABLE_SUPERCLASS));
        }
    }

    private void createDataMapTemplate(String template) {
        if (template.trim().length() == 0) {
            return;
        }
        if (TemplateType.isDefault(template)) {
            configuration.setDataMapTemplate(TemplateType.DATAMAP_SUBCLASS.defaultTemplate());
        } else {
            configuration.setDataMapTemplate(new CgenTemplate(template,false,TemplateType.DATAMAP_SUBCLASS));
        }
    }

    private void createDataMapSuperTemplate(String template) {
        if (template.trim().length() == 0) {
            return;
        }
        if (TemplateType.isDefault(template)) {
            configuration.setDataMapSuperTemplate(TemplateType.DATAMAP_SUPERCLASS.defaultTemplate());
        } else {
            configuration.setDataMapSuperTemplate(new CgenTemplate(template,false,TemplateType.DATAMAP_SUPERCLASS));
        }
    }

    private void createOutputPattern(String pattern) {
        if (pattern.trim().length() == 0) {
            return;
        }
        configuration.setOutputPattern(pattern);
    }

    private void createMakePairs(String makePairs) {
        if (makePairs.trim().length() == 0) {
            return;
        }
        configuration.setMakePairs(makePairs.equals(TRUE));
    }

    private void createUsePkgPath(String data) {
        if (data.trim().length() == 0) {
            return;
        }
        configuration.setUsePkgPath(data.equals(TRUE));
    }

    private void createOverwriteSubclasses(String data) {
        if (data.trim().length() == 0) {
            return;
        }
        configuration.setOverwrite(data.equals(TRUE));
    }

    private void createPropertyNamesTag(String data) {
        if (data.trim().length() == 0) {
            return;
        }
        configuration.setCreatePropertyNames(data.equals(TRUE));
    }

    private void createPkPropertiesTag(String data) {
        if (data.trim().length() == 0) {
            return;
        }
        configuration.setCreatePKProperties(data.equals(TRUE));
    }

    private void createSuperPkg(String data) {
        if (data.trim().length() == 0) {
            return;
        }
        configuration.setSuperPkg(data);
    }

    private void createConfig() {
        loaderContext.addDataMapListener(dataMap -> {
            configuration.setDataMap(dataMap);
            configuration.setRootPath(buildRootPath(dataMap));
            configuration.resolveExcludeEntities();
            configuration.resolveExcludeEmbeddables();

            CgenConfigList configurations = CgenConfigHandler.this.metaData.get(dataMap, CgenConfigList.class);
            if (configurations == null) {
                configurations = new CgenConfigList();
                CgenConfigHandler.this.metaData.add(dataMap, configurations);
            }
            configurations.add(configuration);
        });
    }

    private Path buildRootPath(DataMap dataMap) {
        URL url = dataMap.getConfigurationSource().getURL();
        Path resourcePath;
        try {
            resourcePath = Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new CayenneRuntimeException("Unable to read cgen path", e);
        }
        if (Files.isRegularFile(resourcePath)) {
            resourcePath = resourcePath.getParent();
        }
        return resourcePath;
    }
}
