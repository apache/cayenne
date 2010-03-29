/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.gen;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;

import foundrylogic.vpp.VPPConfig;

/**
 * Generates Java source code for ObjEntities in the DataMap. This class is abstract and
 * does not deal with filesystem issues directly. Concrete subclasses should provide ways
 * to store generated files by implementing {@link #openWriter(ObjEntity, String, String)}
 * and {@link #closeWriter(Writer)} methods.
 * 
 * @deprecated since 3.0 use {@link ClassGenerationAction} and subclasses.
 */
public abstract class MapClassGenerator {

    public static final String SINGLE_CLASS_TEMPLATE_1_1 = "dotemplates/singleclass.vm";
    public static final String SUBCLASS_TEMPLATE_1_1 = "dotemplates/subclass.vm";
    public static final String SUPERCLASS_TEMPLATE_1_1 = "dotemplates/superclass.vm";

    public static final String SINGLE_CLASS_TEMPLATE_1_2 = "dotemplates/v1_2/singleclass.vm";
    public static final String SUBCLASS_TEMPLATE_1_2 = "dotemplates/v1_2/subclass.vm";
    public static final String SUPERCLASS_TEMPLATE_1_2 = "dotemplates/v1_2/superclass.vm";

    /**
     * @since 1.2
     */
    public static final String CLIENT_SUBCLASS_TEMPLATE_1_2 = "dotemplates/v1_2/client-subclass.vm";

    /**
     * @since 1.2
     */
    public static final String CLIENT_SUPERCLASS_TEMPLATE_1_2 = "dotemplates/v1_2/client-superclass.vm";

    public static final String SINGLE_CLASS_TEMPLATE = SINGLE_CLASS_TEMPLATE_1_1;
    public static final String SUBCLASS_TEMPLATE = SUBCLASS_TEMPLATE_1_1;
    public static final String SUPERCLASS_TEMPLATE = SUPERCLASS_TEMPLATE_1_1;

    public static final String SUPERCLASS_PREFIX = "_";

    protected static final String VERSION_1_1 = ClassGenerator.VERSION_1_1;
    protected static final String VERSION_1_2 = ClassGenerator.VERSION_1_2;

    public static final String DEFAULT_VERSION = VERSION_1_2;

    public static final String MODE_DATAMAP = "datamap";
    public static final String MODE_ENTITY = "entity";

    protected String versionString = DEFAULT_VERSION;

    protected List<ObjEntity> objEntities;
    protected String superPkg;
    protected DataMap dataMap;
    protected VPPConfig vppConfig;
    protected String mode = MODE_ENTITY;
    protected boolean client;

    public MapClassGenerator() {
    }

    /**
     * @deprecated since 3.0 - unused.
     */
    public MapClassGenerator(DataMap dataMap) {
        this(dataMap, new ArrayList<ObjEntity>(dataMap.getObjEntities()));
    }

    /**
     * Creates a new MapClassGenerator.
     * 
     * @since 1.2
     */
    public MapClassGenerator(DataMap dataMap, List<ObjEntity> objEntities) {
        this.dataMap = dataMap;
        this.setObjEntities(objEntities);
    }

    protected String defaultSingleClassTemplate() {
        // there is no default single class client template at the moment
        if (client) {
            throw new IllegalStateException(
                    "Default generation for single classes on the client is not supported...");
        }
        else if (VERSION_1_1.equals(versionString)) {
            return SINGLE_CLASS_TEMPLATE_1_1;
        }
        else if (VERSION_1_2.equals(versionString)) {
            return SINGLE_CLASS_TEMPLATE_1_2;
        }
        return SINGLE_CLASS_TEMPLATE;
    }

    protected String defaultSubclassTemplate() {
        // client templates are always 1.2
        if (client) {
            return CLIENT_SUBCLASS_TEMPLATE_1_2;
        }
        else if (VERSION_1_1.equals(versionString)) {
            return SUBCLASS_TEMPLATE_1_1;
        }
        else if (VERSION_1_2.equals(versionString)) {
            return SUBCLASS_TEMPLATE_1_2;
        }
        return SUBCLASS_TEMPLATE;
    }

    protected String defaultSuperclassTemplate() {
        // client templates are always 1.2
        if (client) {
            return CLIENT_SUPERCLASS_TEMPLATE_1_2;
        }
        else if (VERSION_1_1.equals(versionString)) {
            return SUPERCLASS_TEMPLATE_1_1;
        }
        else if (VERSION_1_2.equals(versionString)) {
            return SUPERCLASS_TEMPLATE_1_2;
        }
        return SUPERCLASS_TEMPLATE;
    }

    /**
     * Creates a Writer to output source code for a given ObjEntity and Java class.
     * 
     * @return Writer to store generated class source code or null if this class
     *         generation should be skipped.
     */
    public abstract Writer openWriter(ObjEntity entity, String pkgName, String className)
            throws Exception;

    /**
     * Closes writer after class code has been successfully written by
     * ClassGenerationInfo.
     */
    public abstract void closeWriter(Writer out) throws Exception;

    /**
     * Runs class generation. Produces a pair of Java classes for each ObjEntity in the
     * map. Uses default Cayenne templates for classes.
     * 
     * @deprecated since 3.0 - unused
     */
    public void generateClassPairs() throws Exception {
        generateClassPairs(
                defaultSubclassTemplate(),
                defaultSuperclassTemplate(),
                SUPERCLASS_PREFIX);
    }

    /**
     * Runs class generation. Produces a pair of Java classes for each ObjEntity in the
     * map. This allows developers to use generated <b>subclass </b> for their custom
     * code, while generated <b>superclass </b> will contain Cayenne code. Superclass will
     * be generated in the same package, its class name will be derived from the class
     * name by adding a <code>superPrefix</code>.
     */
    public void generateClassPairs(
            String classTemplate,
            String superTemplate,
            String superPrefix) throws Exception {

        // note - client templates ignore version and unconditionally use 1.2
        if (client) {
            generateClientClassPairs_1_2(classTemplate, superTemplate, superPrefix);
        }
        else if (VERSION_1_1.equals(versionString)) {
            generateClassPairs_1_1(classTemplate, superTemplate, superPrefix);
        }
        else if (VERSION_1_2.equals(versionString)) {
            generateClassPairs_1_2(classTemplate, superTemplate, superPrefix);
        }
        else {
            throw new IllegalStateException("Illegal Version in generateClassPairs: "
                    + versionString);
        }
    }

    /**
     * Runs class generation. Produces a pair of Java classes for each ObjEntity in the
     * map. This allows developers to use generated <b>subclass </b> for their custom
     * code, while generated <b>superclass </b> will contain Cayenne code. Superclass will
     * be generated in the same package, its class name will be derived from the class
     * name by adding a <code>superPrefix</code>.
     */
    private void generateClassPairs_1_1(
            String classTemplate,
            String superTemplate,
            String superPrefix) throws Exception {

        ClassGenerator mainGenSetup = new ClassGenerator(classTemplate, versionString);
        ClassGenerator superGenSetup = new ClassGenerator(superTemplate, versionString);

        ClassGenerationInfo mainGen = mainGenSetup.getClassGenerationInfo();
        ClassGenerationInfo superGen = superGenSetup.getClassGenerationInfo();

        // prefix is needed for both generators
        mainGen.setSuperPrefix(superPrefix);
        superGen.setSuperPrefix(superPrefix);

        for (ObjEntity ent : entitiesForCurrentMode()) {

            // 1. do the superclass
            initClassGenerator_1_1(superGen, ent, true);

            Writer superOut = openWriter(ent, superGen.getPackageName(), superPrefix
                    + superGen.getClassName());

            if (superOut != null) {
                superGenSetup.generateClass(superOut, ent);
                closeWriter(superOut);
            }

            // 2. do the main class
            initClassGenerator_1_1(mainGen, ent, false);
            Writer mainOut = openWriter(ent, mainGen.getPackageName(), mainGen
                    .getClassName());
            if (mainOut != null) {
                mainGenSetup.generateClass(mainOut, ent);
                closeWriter(mainOut);
            }
        }
    }

    private void generateClientClassPairs_1_2(
            String classTemplate,
            String superTemplate,
            String superPrefix) throws Exception {

        ClassGenerator mainGenSetup = new ClassGenerator(
                classTemplate,
                ClassGenerator.VERSION_1_2,
                vppConfig);
        ClassGenerator superGenSetup = new ClassGenerator(
                superTemplate,
                ClassGenerator.VERSION_1_2,
                vppConfig);

        for (ObjEntity entity : entitiesForCurrentMode()) {

            // use client name, and if not specified use regular class name
            String fqnSubClass = entity.getClientClassName();
            if (fqnSubClass == null) {
                fqnSubClass = entity.getClassName();
            }

            // use PersistentObject instead of CayenneDataObject as base ...
            String fqnBaseClass = (entity.getClientSuperClassName() != null) ? entity
                    .getClientSuperClassName() : PersistentObject.class.getName();

            StringUtils stringUtils = StringUtils.getInstance();

            String subClassName = stringUtils.stripPackageName(fqnSubClass);
            String subPackageName = stringUtils.stripClass(fqnSubClass);

            String superClassName = superPrefix
                    + stringUtils.stripPackageName(fqnSubClass);

            String superPackageName = this.superPkg;
            String fqnSuperClass = superPackageName + "." + superClassName;

            Writer superOut = openWriter(entity, superPackageName, superClassName);

            if (superOut != null) {
                superGenSetup.generateClass(
                        superOut,
                        dataMap,
                        entity,
                        fqnBaseClass,
                        fqnSuperClass,
                        fqnSubClass);
                closeWriter(superOut);
            }

            Writer mainOut = openWriter(entity, subPackageName, subClassName);
            if (mainOut != null) {
                mainGenSetup.generateClass(
                        mainOut,
                        dataMap,
                        entity,
                        fqnBaseClass,
                        fqnSuperClass,
                        fqnSubClass);
                closeWriter(mainOut);
            }
        }
    }

    /**
     * Runs class generation. Produces a pair of Java classes for each ObjEntity in the
     * map. This allows developers to use generated <b>subclass </b> for their custom
     * code, while generated <b>superclass </b> will contain Cayenne code. Superclass will
     * be generated in the same package, its class name will be derived from the class
     * name by adding a <code>superPrefix</code>.
     */
    private void generateClassPairs_1_2(
            String classTemplate,
            String superTemplate,
            String superPrefix) throws Exception {

        ClassGenerator mainGenSetup = new ClassGenerator(
                classTemplate,
                versionString,
                vppConfig);
        ClassGenerator superGenSetup = new ClassGenerator(
                superTemplate,
                versionString,
                vppConfig);

        for (ObjEntity ent : entitiesForCurrentMode()) {

            String fqnSubClass = ent.getClassName();
            String fqnBaseClass = (null != ent.getSuperClassName()) ? ent
                    .getSuperClassName() : CayenneDataObject.class.getName();

            StringUtils stringUtils = StringUtils.getInstance();

            String subClassName = stringUtils.stripPackageName(fqnSubClass);
            String subPackageName = stringUtils.stripClass(fqnSubClass);

            String superClassName = superPrefix
                    + stringUtils.stripPackageName(fqnSubClass);

            String superPackageName = this.superPkg;
            String fqnSuperClass = superPackageName + "." + superClassName;

            Writer superOut = openWriter(ent, superPackageName, superClassName);

            if (superOut != null) {
                superGenSetup.generateClass(
                        superOut,
                        dataMap,
                        ent,
                        fqnBaseClass,
                        fqnSuperClass,
                        fqnSubClass);
                closeWriter(superOut);
            }

            Writer mainOut = openWriter(ent, subPackageName, subClassName);
            if (mainOut != null) {
                mainGenSetup.generateClass(
                        mainOut,
                        dataMap,
                        ent,
                        fqnBaseClass,
                        fqnSuperClass,
                        fqnSubClass);
                closeWriter(mainOut);
            }
        }
    }

    /**
     * Runs class generation. Produces a single Java class for each ObjEntity in the map.
     * Uses default Cayenne templates for classes.
     * 
     * @deprecated since 3.0 - unused.
     */
    public void generateSingleClasses() throws Exception {
        generateSingleClasses(defaultSingleClassTemplate(), SUPERCLASS_PREFIX);
    }

    /**
     * Runs class generation. Produces a single Java class for each ObjEntity in the map.
     */
    private void generateSingleClasses_1_1(String classTemplate) throws Exception {
        ClassGenerator gen = new ClassGenerator(classTemplate, versionString);

        for (ObjEntity ent : entitiesForCurrentMode()) {

            initClassGenerator_1_1(gen.getClassGenerationInfo(), ent, false);
            Writer out = openWriter(
                    ent,
                    gen.getClassGenerationInfo().getPackageName(),
                    gen.getClassGenerationInfo().getClassName());
            if (out == null) {
                continue;
            }

            gen.generateClass(out, ent);
            closeWriter(out);
        }
    }

    /**
     * Runs class generation. Produces a single Java class for each ObjEntity in the map.
     */
    private void generateSingleClasses_1_2(String classTemplate, String superPrefix)
            throws Exception {
        ClassGenerator gen = new ClassGenerator(classTemplate, versionString, vppConfig);

        for (ObjEntity ent : entitiesForCurrentMode()) {

            String fqnSubClass = ent.getClassName();
            String fqnBaseClass = (null != ent.getSuperClassName()) ? ent
                    .getSuperClassName() : CayenneDataObject.class.getName();

            StringUtils stringUtils = StringUtils.getInstance();

            String subClassName = stringUtils.stripPackageName(fqnSubClass);
            String subPackageName = stringUtils.stripClass(fqnSubClass);

            String superClassName = superPrefix
                    + stringUtils.stripPackageName(fqnSubClass);

            String superPackageName = this.superPkg;
            String fqnSuperClass = superPackageName + "." + superClassName;

            Writer out = openWriter(ent, subPackageName, subClassName);
            if (out == null) {
                continue;
            }

            gen
                    .generateClass(
                            out,
                            dataMap,
                            ent,
                            fqnBaseClass,
                            fqnSuperClass,
                            fqnSubClass);
            closeWriter(out);
        }
    }

    private Collection<ObjEntity> entitiesForCurrentMode() {

        // Iterate only once if this is datamap mode
        if (!MODE_ENTITY.equals(mode) && !objEntities.isEmpty()) {
            return Collections.singleton(objEntities.get(0));
        }
        else {
            return this.objEntities;
        }
    }

    /**
     * Runs class generation. Produces a single Java class for each ObjEntity in the map.
     */
    public void generateSingleClasses(String classTemplate, String superPrefix)
            throws Exception {
        // note - client templates ignore version and automatically switch to 1.2
        if (client) {
            generateSingleClasses_1_2(classTemplate, superPrefix);
        }
        else if (VERSION_1_1.equals(versionString)) {
            generateSingleClasses_1_1(classTemplate);
        }
        else if (VERSION_1_2.equals(versionString)) {
            generateSingleClasses_1_2(classTemplate, superPrefix);
        }
        else {
            throw new IllegalStateException("Illegal Version in generateClassPairs: "
                    + versionString);
        }
    }

    /** Initializes ClassGenerationInfo with class name and package of a generated class. */
    protected void initClassGenerator_1_1(
            ClassGenerationInfo gen,
            ObjEntity entity,
            boolean superclass) {

        // figure out generator properties
        String fullClassName = entity.getClassName();
        int i = fullClassName.lastIndexOf(".");

        String pkg = null;
        String spkg = null;
        String cname = null;

        // dot in first or last position is invalid
        if (i == 0 || i + 1 == fullClassName.length()) {
            throw new CayenneRuntimeException("Invalid class mapping: " + fullClassName);
        }
        else if (i < 0) {
            pkg = (superclass) ? superPkg : null;
            spkg = (superclass) ? null : superPkg;
            cname = fullClassName;
        }
        else {
            cname = fullClassName.substring(i + 1);
            pkg = (superclass && superPkg != null) ? superPkg : fullClassName.substring(
                    0,
                    i);

            spkg = (!superclass && superPkg != null && !pkg.equals(superPkg))
                    ? superPkg
                    : null;
        }

        // init generator
        gen.setPackageName(pkg);
        gen.setClassName(cname);
        if (entity.getSuperClassName() != null) {
            gen.setSuperClassName(entity.getSuperClassName());
        }
        else {
            gen.setSuperClassName(CayenneDataObject.class.getName());
        }
        gen.setSuperPackageName(spkg);
    }

    /**
     * Returns "superPkg" property value - a name of a superclass package that should be
     * used for all generated superclasses.
     */
    public String getSuperPkg() {
        return superPkg;
    }

    /**
     * Sets "superPkg" property value.
     */
    public void setSuperPkg(String superPkg) {
        this.superPkg = superPkg;
    }

    /**
     * Returns whether a default client object template will be used.
     * 
     * @since 1.2
     */
    public boolean isClient() {
        return client;
    }

    /**
     * Sets whether a default client object template should be used.
     * 
     * @since 1.2
     */
    public void setClient(boolean client) {
        this.client = client;
    }

    /**
     * @return Returns the dataMap.
     */
    public DataMap getDataMap() {
        return dataMap;
    }

    /**
     * @param dataMap The dataMap to set.
     */
    public void setDataMap(DataMap dataMap) {
        this.dataMap = dataMap;
    }

    public List<ObjEntity> getObjEntities() {
        return objEntities;
    }

    /**
     * Initializes internal ObjEntities list. This method creates a copy of the provided
     * list to allow its independent modification and also filters out entities that do
     * not require class generation.
     */
    public void setObjEntities(List<ObjEntity> objEntities) {
        this.objEntities = objEntities != null
                ? new ArrayList<ObjEntity>(objEntities)
                : new ArrayList<ObjEntity>();

        // remove generic entities...
        Iterator<ObjEntity> it = objEntities.iterator();
        while (it.hasNext()) {
            ObjEntity e = it.next();
            if (e.isGeneric()) {
                it.remove();
            }
        }
    }

    /**
     * @return Returns the versionString.
     */
    public String getVersionString() {
        return versionString;
    }

    /**
     * @param versionString The versionString to set.
     */
    public void setVersionString(String versionString) {
        if ((false == VERSION_1_1.equals(versionString))
                && (false == VERSION_1_2.equals(versionString))) {
            throw new IllegalStateException("'version' must be '"
                    + VERSION_1_1
                    + "' or '"
                    + VERSION_1_2
                    + "', but was '"
                    + versionString
                    + "'");
        }
        this.versionString = versionString;
    }

    /**
     * @return Returns the vppConfig.
     */
    public VPPConfig getVppConfig() {
        return vppConfig;
    }

    /**
     * @param vppConfig The vppConfig to set.
     */
    public void setVppConfig(VPPConfig vppConfig) {
        this.vppConfig = vppConfig;
    }

    /**
     * @param mode use "entity" for per-entity generation and "datamap" for per-datamap
     *            generation.
     */
    public void setMode(String mode) {
        if ((false == MODE_ENTITY.equals(mode)) && (false == MODE_DATAMAP.equals(mode))) {
            throw new IllegalStateException("'mode' must be '"
                    + MODE_ENTITY
                    + "' or '"
                    + MODE_DATAMAP
                    + "', but was '"
                    + mode
                    + "'");
        }
        this.mode = mode;
    }
}
