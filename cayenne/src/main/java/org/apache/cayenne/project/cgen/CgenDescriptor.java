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

package org.apache.cayenne.project.cgen;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class generation settings of a DataMap, stored in the DataMap XML. A DataMap can have several of them, told apart by
 * name. The values are kept as they are set, and normalized when written to XML.
 *
 * @since 6.0
 */
public class CgenDescriptor {

    public static final String DEFAULT_NAME = "Default";

    public static final String DEFAULT_TEMPLATE = "templates/v4_1/subclass.vm";
    public static final String DEFAULT_SUPER_TEMPLATE = "templates/v4_1/superclass.vm";
    public static final String DEFAULT_EMBEDDABLE_TEMPLATE = "templates/v4_1/embeddable-subclass.vm";
    public static final String DEFAULT_EMBEDDABLE_SUPER_TEMPLATE = "templates/v4_1/embeddable-superclass.vm";
    public static final String DEFAULT_DATA_MAP_TEMPLATE = "templates/v4_1/datamap-subclass.vm";
    public static final String DEFAULT_DATA_MAP_SUPER_TEMPLATE = "templates/v4_1/datamap-superclass.vm";
    // the single class templates are used as the subclass templates when the classes are not generated in pairs
    public static final String DEFAULT_SINGLE_CLASS_TEMPLATE = "templates/v4_1/singleclass.vm";
    public static final String DEFAULT_EMBEDDABLE_SINGLE_CLASS_TEMPLATE = "templates/v4_1/embeddable-singleclass.vm";
    public static final String DEFAULT_DATA_MAP_SINGLE_CLASS_TEMPLATE = "templates/v4_1/datamap-singleclass.vm";

    private static final Set<String> DEFAULT_TEMPLATES = Set.of(
            DEFAULT_TEMPLATE,
            DEFAULT_SUPER_TEMPLATE,
            DEFAULT_EMBEDDABLE_TEMPLATE,
            DEFAULT_EMBEDDABLE_SUPER_TEMPLATE,
            DEFAULT_DATA_MAP_TEMPLATE,
            DEFAULT_DATA_MAP_SUPER_TEMPLATE,
            DEFAULT_SINGLE_CLASS_TEMPLATE,
            DEFAULT_EMBEDDABLE_SINGLE_CLASS_TEMPLATE,
            DEFAULT_DATA_MAP_SINGLE_CLASS_TEMPLATE);

    private String name;
    private String destDir;
    private String mode;
    private boolean makePairs;
    private boolean usePkgPath;
    private boolean overwrite;
    private boolean createPropertyNames;
    private boolean createPKProperties;
    private String outputPattern;
    private String superPkg;
    private String externalToolConfig;
    private String template;
    private String superTemplate;
    private String embeddableTemplate;
    private String embeddableSuperTemplate;
    private String dataMapTemplate;
    private String dataMapSuperTemplate;
    private Set<String> excludedEntities;
    private Set<String> excludedEmbeddables;

    public CgenDescriptor() {
        this.name = DEFAULT_NAME;
        this.mode = "entity";
        this.makePairs = true;
        this.usePkgPath = true;
        this.createPKProperties = true;
        this.outputPattern = "*.java";
        this.excludedEntities = new LinkedHashSet<>();
        this.excludedEmbeddables = new LinkedHashSet<>();
    }

    public CgenDescriptor(CgenDescriptor original) {
        this.name = original.name;
        this.destDir = original.destDir;
        this.mode = original.mode;
        this.makePairs = original.makePairs;
        this.usePkgPath = original.usePkgPath;
        this.overwrite = original.overwrite;
        this.createPropertyNames = original.createPropertyNames;
        this.createPKProperties = original.createPKProperties;
        this.outputPattern = original.outputPattern;
        this.superPkg = original.superPkg;
        this.externalToolConfig = original.externalToolConfig;
        this.template = original.template;
        this.superTemplate = original.superTemplate;
        this.embeddableTemplate = original.embeddableTemplate;
        this.embeddableSuperTemplate = original.embeddableSuperTemplate;
        this.dataMapTemplate = original.dataMapTemplate;
        this.dataMapSuperTemplate = original.dataMapSuperTemplate;
        this.excludedEntities = new LinkedHashSet<>(original.excludedEntities);
        this.excludedEmbeddables = new LinkedHashSet<>(original.excludedEmbeddables);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the directory to generate the classes into, either absolute or relative to the DataMap directory. Null
     * means the DataMap directory.
     */
    public String getDestDir() {
        return destDir;
    }

    public void setDestDir(String destDir) {
        this.destDir = destDir;
    }

    /**
     * Returns "entity" to generate the entity and embeddable classes only, or "all" to also generate the DataMap class.
     */
    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = "entity".equalsIgnoreCase(mode) ? "entity" : "all";
    }

    public boolean isMakePairs() {
        return makePairs;
    }

    public void setMakePairs(boolean makePairs) {
        this.makePairs = makePairs;
    }

    public boolean isUsePkgPath() {
        return usePkgPath;
    }

    public void setUsePkgPath(boolean usePkgPath) {
        this.usePkgPath = usePkgPath;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public boolean isCreatePropertyNames() {
        return createPropertyNames;
    }

    public void setCreatePropertyNames(boolean createPropertyNames) {
        this.createPropertyNames = createPropertyNames;
    }

    public boolean isCreatePKProperties() {
        return createPKProperties;
    }

    public void setCreatePKProperties(boolean createPKProperties) {
        this.createPKProperties = createPKProperties;
    }

    public String getOutputPattern() {
        return outputPattern;
    }

    public void setOutputPattern(String outputPattern) {
        this.outputPattern = outputPattern;
    }

    public String getSuperPkg() {
        return superPkg;
    }

    public void setSuperPkg(String superPkg) {
        this.superPkg = superPkg;
    }

    public String getExternalToolConfig() {
        return externalToolConfig;
    }

    public void setExternalToolConfig(String externalToolConfig) {
        this.externalToolConfig = externalToolConfig;
    }

    /**
     * Returns the template of the entity subclasses, either a path or a template text. Null means the default
     * template. The same applies to the other templates.
     */
    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getSuperTemplate() {
        return superTemplate;
    }

    public void setSuperTemplate(String superTemplate) {
        this.superTemplate = superTemplate;
    }

    public String getEmbeddableTemplate() {
        return embeddableTemplate;
    }

    public void setEmbeddableTemplate(String embeddableTemplate) {
        this.embeddableTemplate = embeddableTemplate;
    }

    public String getEmbeddableSuperTemplate() {
        return embeddableSuperTemplate;
    }

    public void setEmbeddableSuperTemplate(String embeddableSuperTemplate) {
        this.embeddableSuperTemplate = embeddableSuperTemplate;
    }

    public String getDataMapTemplate() {
        return dataMapTemplate;
    }

    public void setDataMapTemplate(String dataMapTemplate) {
        this.dataMapTemplate = dataMapTemplate;
    }

    public String getDataMapSuperTemplate() {
        return dataMapSuperTemplate;
    }

    public void setDataMapSuperTemplate(String dataMapSuperTemplate) {
        this.dataMapSuperTemplate = dataMapSuperTemplate;
    }

    /**
     * Returns a modifiable set of the ObjEntity names to skip. Names missing from the DataMap are not saved.
     */
    public Set<String> getExcludedEntities() {
        return excludedEntities;
    }

    public void setExcludedEntities(Collection<String> excludedEntities) {
        this.excludedEntities = excludedEntities != null
                ? new LinkedHashSet<>(excludedEntities)
                : new LinkedHashSet<>();
    }

    /**
     * Returns a modifiable set of the Embeddable class names to skip. Names missing from the DataMap are not saved.
     */
    public Set<String> getExcludedEmbeddables() {
        return excludedEmbeddables;
    }

    public void setExcludedEmbeddables(Collection<String> excludedEmbeddables) {
        this.excludedEmbeddables = excludedEmbeddables != null
                ? new LinkedHashSet<>(excludedEmbeddables)
                : new LinkedHashSet<>();
    }

    /**
     * @param owner the DataMap of these settings, which the excluded names are checked against
     */
    public void encodeAsXML(XMLEncoder encoder, DataMap owner) {
        encoder.start("cgen")
                .textTag("name", name)
                .textTag("excludeEntities", encodeExcludedNames(excludedEntities, owner.getObjEntityMap().keySet()))
                .textTag("excludeEmbeddables",
                        encodeExcludedNames(excludedEmbeddables, owner.getEmbeddableMap().keySet()))
                .textTag("destDir", destDir != null && !destDir.isEmpty() ? Util.substBackslashes(destDir) : ".")
                .textTag("mode", mode);
        encodeTemplate(encoder, "template", template, DEFAULT_TEMPLATE, DEFAULT_SINGLE_CLASS_TEMPLATE);
        encodeTemplate(encoder, "superTemplate", superTemplate, DEFAULT_SUPER_TEMPLATE, null);
        encodeTemplate(encoder, "embeddableTemplate", embeddableTemplate, DEFAULT_EMBEDDABLE_TEMPLATE,
                DEFAULT_EMBEDDABLE_SINGLE_CLASS_TEMPLATE);
        encodeTemplate(encoder, "embeddableSuperTemplate", embeddableSuperTemplate, DEFAULT_EMBEDDABLE_SUPER_TEMPLATE,
                null);
        encodeTemplate(encoder, "dataMapTemplate", dataMapTemplate, DEFAULT_DATA_MAP_TEMPLATE,
                DEFAULT_DATA_MAP_SINGLE_CLASS_TEMPLATE);
        encodeTemplate(encoder, "dataMapSuperTemplate", dataMapSuperTemplate, DEFAULT_DATA_MAP_SUPER_TEMPLATE, null);
        encoder.textTag("outputPattern", outputPattern)
                .textTag("makePairs", Boolean.toString(makePairs))
                .textTag("usePkgPath", Boolean.toString(usePkgPath))
                .textTag("overwrite", Boolean.toString(overwrite))
                .textTag("createPropertyNames", Boolean.toString(createPropertyNames))
                .textTag("superPkg", Util.substBackslashes(superPkg))
                .textTag("createPKProperties", Boolean.toString(createPKProperties))
                .textTag("externalToolConfig", externalToolConfig)
                .end();
    }

    /**
     * Splits the comma-separated excluded names, as they are stored in XML.
     */
    public static List<String> parseExcludedNames(String names) {
        return Arrays.stream(names.split(","))
                .map(String::strip)
                .filter(name -> !name.isEmpty())
                .toList();
    }

    // sorted, so that the saved XML doesn't depend on the order of the edits
    private static String encodeExcludedNames(Set<String> excluded, Set<String> existing) {
        return excluded.stream()
                .filter(existing::contains)
                .sorted()
                .collect(Collectors.joining(","));
    }

    /**
     * Returns the template of a template setting: the default template of the setting for no template, and for any
     * default template path except the single class template of the setting. Otherwise the template as is.
     *
     * @param defaultTemplate     the default template path of the setting
     * @param singleClassTemplate the single class template path of the setting, or null for a superclass setting
     */
    public static String normalizeTemplate(String template, String defaultTemplate, String singleClassTemplate) {
        return template == null || (DEFAULT_TEMPLATES.contains(template) && !template.equals(singleClassTemplate))
                ? defaultTemplate
                : template;
    }

    private static void encodeTemplate(XMLEncoder encoder, String tag, String template, String defaultTemplate,
                                       String singleClassTemplate) {
        String value = normalizeTemplate(template, defaultTemplate, singleClassTemplate);

        if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            // "]]>" would end the CDATA section, so it is split between two sections
            encoder.start(tag).cdata(value.replace("]]>", "]]]]><![CDATA[>"), true).end();
        } else {
            encoder.textTag(tag, value);
        }
    }
}
