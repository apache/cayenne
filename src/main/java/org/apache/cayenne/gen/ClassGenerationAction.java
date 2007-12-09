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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.tools.NamePatternMatcher;
import org.apache.commons.logging.Log;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.NullLogSystem;

/**
 * @since 3.0
 * @author Andrus Adamchik
 */
public class ClassGenerationAction {

    public static final String SINGLE_CLASS_TEMPLATE = "dotemplates/v1_2/singleclass.vm";
    public static final String SUBCLASS_TEMPLATE = "dotemplates/v1_2/subclass.vm";
    public static final String SUPERCLASS_TEMPLATE = "dotemplates/v1_2/superclass.vm";

    public static final String SUPERCLASS_PREFIX = "_";
    private static final String WILDCARD = "*";

    protected Collection<ObjEntity> entities;
    protected String superPkg;
    protected DataMap dataMap;
    protected ClassGeneratorMode mode;
    protected VelocityContext context;
    protected Map<String, Template> templateCache;

    protected Log logger;
    protected File destDir;
    protected boolean overwrite;
    protected boolean usePkgPath;
    protected boolean makePairs;
    protected String template;
    protected String superTemplate;
    protected long timestamp;
    protected String outputPattern;
    protected String encoding;

    public ClassGenerationAction() {
        this.mode = ClassGeneratorMode.entity;
        this.outputPattern = "*.java";
        this.timestamp = System.currentTimeMillis();
        this.usePkgPath = true;
        this.makePairs = true;
        this.context = new VelocityContext();
        this.templateCache = new HashMap<String, Template>(5);
    }

    protected String defaultSingleClassTemplate() {
        return ClassGenerationAction.SINGLE_CLASS_TEMPLATE;
    }

    protected String defaultSubclassTemplate() {
        return ClassGenerationAction.SUBCLASS_TEMPLATE;
    }

    protected String defaultSuperclassTemplate() {
        return ClassGenerationAction.SUPERCLASS_TEMPLATE;
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

        StringUtils stringUtils = StringUtils.getInstance();

        for (ObjEntity entity : entitiesForCurrentMode()) {

            String fqnSubClass = entity.getClassName();
            String fqnBaseClass = (entity.getSuperClassName() != null) ? entity
                    .getSuperClassName() : CayenneDataObject.class.getName();

            String subClassName = stringUtils.stripPackageName(fqnSubClass);
            String subPackageName = stringUtils.stripClass(fqnSubClass);

            String superClassName = superPrefix
                    + stringUtils.stripPackageName(fqnSubClass);

            String superPackageName = this.superPkg;
            if (superPackageName == null) {
                superPackageName = subPackageName;
            }
            String fqnSuperClass = superPackageName + "." + superClassName;

            Writer superOut = openWriter(superPackageName, superClassName);
            if (superOut != null) {
                generate(
                        superOut,
                        superTemplate,
                        entity,
                        fqnBaseClass,
                        fqnSuperClass,
                        fqnSubClass);
                superOut.close();
            }

            Writer mainOut = openWriter(subPackageName, subClassName);
            if (mainOut != null) {
                generate(
                        mainOut,
                        classTemplate,
                        entity,
                        fqnBaseClass,
                        fqnSuperClass,
                        fqnSubClass);
                mainOut.close();
            }
        }
    }

    /**
     * Runs class generation. Produces a single Java class for each ObjEntity in the map.
     */
    public void generateSingleClasses(String classTemplate, String superPrefix)
            throws Exception {

        for (ObjEntity entity : entitiesForCurrentMode()) {

            String fqnSubClass = entity.getClassName();
            String fqnBaseClass = (null != entity.getSuperClassName()) ? entity
                    .getSuperClassName() : CayenneDataObject.class.getName();

            StringUtils stringUtils = StringUtils.getInstance();

            String subClassName = stringUtils.stripPackageName(fqnSubClass);
            String subPackageName = stringUtils.stripClass(fqnSubClass);

            String superClassName = superPrefix
                    + stringUtils.stripPackageName(fqnSubClass);

            String superPackageName = this.superPkg;
            String fqnSuperClass = superPackageName + "." + superClassName;

            Writer out = openWriter(subPackageName, subClassName);
            if (out != null) {
                generate(
                        out,
                        classTemplate,
                        entity,
                        fqnBaseClass,
                        fqnSuperClass,
                        fqnSubClass);
                out.close();
            }
        }
    }

    /**
     * Runs class generation.
     */
    public void execute() throws Exception {
        validateAttributes();

        try {
            if (makePairs) {
                String t = getTemplateForPairs();
                String st = getSupertemplateForPairs();
                generateClassPairs(t, st, SUPERCLASS_PREFIX);
            }
            else {
                generateSingleClasses(getTemplateForSingles(), SUPERCLASS_PREFIX);
            }
        }
        finally {
            // must reset engine at the end of class generator run to avoid memory
            // leaks and stale templates
            this.templateCache.clear();
        }
    }

    protected Template getTemplate(String name) throws Exception {
        // Velocity < 1.5 has some memory problems, so we will create a VelocityEngine
        // every time, and store templates in an internal cache, to avoid uncontrolled
        // memory leaks... Presumably 1.5 fixes it.

        Template template = templateCache.get(name);

        if (template == null) {

            Properties props = new Properties();

            // null logger that will prevent velocity.log from being generated
            props.put(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, NullLogSystem.class
                    .getName());
            props.put("resource.loader", "cayenne");
            props.put("cayenne.resource.loader.class", ClassGeneratorResourceLoader.class
                    .getName());
            props.put("cayenne.resource.loader.cache", "false");

            VelocityEngine velocityEngine = new VelocityEngine();
            velocityEngine.init(props);

            template = velocityEngine.getTemplate(name);
            templateCache.put(name, template);
        }

        return template;
    }

    protected Collection<ObjEntity> entitiesForCurrentMode() {

        // TODO: andrus, 12/2/2007 - should we setup a dummy entity for an empty map in
        // DataMap mode?
        if (mode != ClassGeneratorMode.entity && !entities.isEmpty()) {
            return Collections.singleton(entities.iterator().next());
        }
        else {
            return this.entities;
        }
    }

    /**
     * Validates the state of this class generator. Throws CayenneRuntimeException if it
     * is in an inconsistent state. Called internally from "execute".
     */
    protected void validateAttributes() {
        if (destDir == null) {
            throw new CayenneRuntimeException("'destDir' attribute is missing.");
        }

        if (!destDir.isDirectory()) {
            throw new CayenneRuntimeException("'destDir' is not a directory.");
        }

        if (!destDir.canWrite()) {
            throw new CayenneRuntimeException("Do not have write permissions for "
                    + destDir);
        }
    }

    /**
     * Sets the destDir.
     */
    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Sets <code>overwrite</code> property.
     */
    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    /**
     * Sets <code>makepairs</code> property.
     */
    public void setMakePairs(boolean makePairs) {
        this.makePairs = makePairs;
    }

    /**
     * Sets <code>template</code> property.
     */
    public void setTemplate(String template) {
        this.template = template;
    }

    /**
     * Sets <code>superTemplate</code> property.
     */
    public void setSuperTemplate(String superTemplate) {
        this.superTemplate = superTemplate;
    }

    /**
     * Sets <code>usepkgpath</code> property.
     */
    public void setUsePkgPath(boolean usePkgPath) {
        this.usePkgPath = usePkgPath;
    }

    /**
     * Sets <code>outputPattern</code> property.
     */
    public void setOutputPattern(String outputPattern) {
        this.outputPattern = outputPattern;
    }

    /**
     * Opens a Writer to write generated output. Writer encoding is determined from the
     * value of the "encoding" property.
     */
    protected Writer openWriter(String pkgName, String className) throws Exception {

        boolean superclass = className.startsWith(SUPERCLASS_PREFIX);
        File outFile = (superclass)
                ? fileForSuperclass(pkgName, className)
                : fileForClass(pkgName, className);

        if (outFile == null) {
            return null;
        }

        if (logger != null) {
            String label = superclass ? "superclass" : "class";
            logger.info("Generating " + label + " file: " + outFile.getCanonicalPath());
        }

        // return writer with specified encoding
        FileOutputStream out = new FileOutputStream(outFile);

        return (encoding != null)
                ? new OutputStreamWriter(out, encoding)
                : new OutputStreamWriter(out);
    }

    /**
     * Returns a target file where a generated superclass must be saved. If null is
     * returned, class shouldn't be generated.
     */
    protected File fileForSuperclass(String pkgName, String className) throws Exception {

        String filename = NamePatternMatcher.replaceWildcardInStringWithString(
                WILDCARD,
                outputPattern,
                className);
        File dest = new File(mkpath(destDir, pkgName), filename);

        // Ignore if the destination is newer than the map
        // (internal timestamp), i.e. has been generated after the map was
        // last saved AND the template is older than the destination file
        if (dest.exists() && !isOld(dest)) {

            if (superTemplate == null) {
                return null;
            }

            File superTemplateFile = new File(superTemplate);
            if (superTemplateFile.lastModified() < dest.lastModified()) {
                return null;
            }
        }

        return dest;
    }

    /**
     * Returns a target file where a generated class must be saved. If null is returned,
     * class shouldn't be generated.
     */
    protected File fileForClass(String pkgName, String className) throws Exception {

        String filename = NamePatternMatcher.replaceWildcardInStringWithString(
                WILDCARD,
                outputPattern,
                className);
        File dest = new File(mkpath(destDir, pkgName), filename);

        if (dest.exists()) {
            // no overwrite of subclasses
            if (makePairs) {
                return null;
            }

            // skip if said so
            if (!overwrite) {
                return null;
            }

            // Ignore if the destination is newer than the map
            // (internal timestamp), i.e. has been generated after the map was
            // last saved AND the template is older than the destination file
            if (!isOld(dest)) {

                if (template == null) {
                    return null;
                }

                File templateFile = new File(template);
                if (templateFile.lastModified() < dest.lastModified()) {
                    return null;
                }
            }
        }

        return dest;
    }

    /**
     * Returns true if <code>file</code> parameter is older than internal timestamp of
     * this class generator.
     */
    protected boolean isOld(File file) {
        return file.lastModified() <= timestamp;
    }

    /**
     * Merges a template with prebuilt context, writing output to provided writer.
     */
    protected void generate(
            Writer out,
            String template,
            ObjEntity entity,
            String fqnBaseClass,
            String fqnSuperClass,
            String fqnSubClass) throws Exception {

        context.put("objEntity", entity);
        context.put("stringUtils", StringUtils.getInstance());
        context.put("entityUtils", new EntityUtils(
                dataMap,
                entity,
                fqnBaseClass,
                fqnSuperClass,
                fqnSubClass));
        context.put("importUtils", new ImportUtils());

        getTemplate(template).merge(context, out);
    }

    /**
     * Returns a File object corresponding to a directory where files that belong to
     * <code>pkgName</code> package should reside. Creates any missing diectories below
     * <code>dest</code>.
     */
    protected File mkpath(File dest, String pkgName) throws Exception {

        if (!usePkgPath || pkgName == null) {
            return dest;
        }

        String path = pkgName.replace('.', File.separatorChar);
        File fullPath = new File(dest, path);
        if (!fullPath.isDirectory() && !fullPath.mkdirs()) {
            throw new Exception("Error making path: " + fullPath);
        }

        return fullPath;
    }

    /**
     * Returns template file path for Java class when generating single classes.
     */
    protected String getTemplateForSingles() throws IOException {
        return (template != null) ? template : defaultSingleClassTemplate();
    }

    /**
     * Returns template file path for Java subclass when generating class pairs.
     */
    protected String getTemplateForPairs() throws IOException {
        return (template != null) ? template : defaultSubclassTemplate();
    }

    /**
     * Returns template file path for Java superclass when generating class pairs.
     */
    protected String getSupertemplateForPairs() throws IOException {
        return (superTemplate != null) ? superTemplate : defaultSuperclassTemplate();
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Sets file encoding. If set to null, default system encoding will be used.
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Sets "superPkg" property value.
     */
    public void setSuperPkg(String superPkg) {
        this.superPkg = superPkg;
    }

    /**
     * @param dataMap The dataMap to set.
     */
    public void setDataMap(DataMap dataMap) {
        this.dataMap = dataMap;
    }

    /**
     * Initializes internal ObjEntities list. This method creates a copy of the provided
     * list to allow its independent modification and also filters out entities that do
     * not require class generation.
     */
    public void setEntities(Collection<ObjEntity> objEntities) {
        this.entities = objEntities != null
                ? new ArrayList<ObjEntity>(objEntities)
                : new ArrayList<ObjEntity>();
    }

    public void setMode(ClassGeneratorMode mode) {
        this.mode = mode;
    }

    /**
     * Sets an optional shared VelocityContext. Useful with tools like VPP that can set
     * custom values in the context, not known to Cayenne.
     */
    public void setContext(VelocityContext context) {
        this.context = context;
    }

    /**
     * Injects an optional logger that will be used to trace generated files at the info
     * level.
     */
    public void setLogger(Log logger) {
        this.logger = logger;
    }
}
