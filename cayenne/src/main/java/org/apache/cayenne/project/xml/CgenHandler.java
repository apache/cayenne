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

package org.apache.cayenne.project.xml;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.project.cgen.CgenDescriptor;
import org.xml.sax.Attributes;

/**
 * Reads a "cgen" tag of a DataMap.
 */
final class CgenHandler extends NamespaceAwareNestedTagHandler {

    static final String CGEN_TAG = "cgen";
    private static final String NAME_TAG = "name";
    private static final String EXCLUDE_ENTITIES_TAG = "excludeEntities";
    private static final String EXCLUDE_EMBEDDABLES_TAG = "excludeEmbeddables";
    private static final String DEST_DIR_TAG = "destDir";
    private static final String MODE_TAG = "mode";
    private static final String TEMPLATE_TAG = "template";
    private static final String SUPER_TEMPLATE_TAG = "superTemplate";
    private static final String EMBEDDABLE_TEMPLATE_TAG = "embeddableTemplate";
    private static final String EMBEDDABLE_SUPER_TEMPLATE_TAG = "embeddableSuperTemplate";
    private static final String DATA_MAP_TEMPLATE_TAG = "dataMapTemplate";
    private static final String DATA_MAP_SUPER_TEMPLATE_TAG = "dataMapSuperTemplate";
    private static final String OUTPUT_PATTERN_TAG = "outputPattern";
    private static final String MAKE_PAIRS_TAG = "makePairs";
    private static final String USE_PKG_PATH_TAG = "usePkgPath";
    private static final String OVERWRITE_TAG = "overwrite";
    private static final String CREATE_PROPERTY_NAMES_TAG = "createPropertyNames";
    private static final String SUPER_PKG_TAG = "superPkg";
    private static final String CREATE_PK_PROPERTIES_TAG = "createPKProperties";
    private static final String EXTERNAL_TOOL_CONFIG_TAG = "externalToolConfig";

    private final DataMap dataMap;
    private CgenDescriptor descriptor;

    CgenHandler(NamespaceAwareNestedTagHandler parentHandler, DataMap dataMap) {
        super(parentHandler);
        this.dataMap = dataMap;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) {
        return switch (localName) {
            case CGEN_TAG -> {
                descriptor = new CgenDescriptor();
                dataMap.addCgenDescriptor(descriptor);
                yield true;
            }
            case NAME_TAG, EXCLUDE_ENTITIES_TAG, EXCLUDE_EMBEDDABLES_TAG, DEST_DIR_TAG, MODE_TAG, TEMPLATE_TAG,
                 SUPER_TEMPLATE_TAG, EMBEDDABLE_TEMPLATE_TAG, EMBEDDABLE_SUPER_TEMPLATE_TAG, DATA_MAP_TEMPLATE_TAG,
                 DATA_MAP_SUPER_TEMPLATE_TAG, OUTPUT_PATTERN_TAG, MAKE_PAIRS_TAG, USE_PKG_PATH_TAG, OVERWRITE_TAG,
                 CREATE_PROPERTY_NAMES_TAG, SUPER_PKG_TAG, CREATE_PK_PROPERTIES_TAG, EXTERNAL_TOOL_CONFIG_TAG -> true;
            default -> false;
        };
    }

    @Override
    protected boolean processCharData(String localName, String data) {
        // blank values keep the defaults
        if (data.isBlank()) {
            return true;
        }

        switch (localName) {
            case NAME_TAG -> descriptor.setName(data);
            case EXCLUDE_ENTITIES_TAG ->
                    descriptor.getExcludedEntities().addAll(CgenDescriptor.parseExcludedNames(data));
            case EXCLUDE_EMBEDDABLES_TAG ->
                    descriptor.getExcludedEmbeddables().addAll(CgenDescriptor.parseExcludedNames(data));
            case DEST_DIR_TAG -> descriptor.setDestDir(data);
            case MODE_TAG -> descriptor.setMode(data);
            case TEMPLATE_TAG -> descriptor.setTemplate(data);
            case SUPER_TEMPLATE_TAG -> descriptor.setSuperTemplate(data);
            case EMBEDDABLE_TEMPLATE_TAG -> descriptor.setEmbeddableTemplate(data);
            case EMBEDDABLE_SUPER_TEMPLATE_TAG -> descriptor.setEmbeddableSuperTemplate(data);
            case DATA_MAP_TEMPLATE_TAG -> descriptor.setDataMapTemplate(data);
            case DATA_MAP_SUPER_TEMPLATE_TAG -> descriptor.setDataMapSuperTemplate(data);
            case OUTPUT_PATTERN_TAG -> descriptor.setOutputPattern(data);
            case MAKE_PAIRS_TAG -> descriptor.setMakePairs("true".equals(data));
            case USE_PKG_PATH_TAG -> descriptor.setUsePkgPath("true".equals(data));
            case OVERWRITE_TAG -> descriptor.setOverwrite("true".equals(data));
            case CREATE_PROPERTY_NAMES_TAG -> descriptor.setCreatePropertyNames("true".equals(data));
            case SUPER_PKG_TAG -> descriptor.setSuperPkg(data);
            case CREATE_PK_PROPERTIES_TAG -> descriptor.setCreatePKProperties("true".equals(data));
            case EXTERNAL_TOOL_CONFIG_TAG -> descriptor.setExternalToolConfig(data);
        }
        return true;
    }
}
