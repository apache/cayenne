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
import java.util.List;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.tools.NamePatternMatcher;
import org.apache.commons.logging.Log;
import org.apache.velocity.VelocityContext;

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

    protected List<ObjEntity> objEntities;
    protected String superPkg;
    protected DataMap dataMap;
    protected ClassGeneratorMode mode;
    protected VelocityContext context;

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

        TemplateProcessor mainGenSetup = new TemplateProcessor(classTemplate, context);
        TemplateProcessor superGenSetup = new TemplateProcessor(superTemplate, context);

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
                superOut.close();
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
                mainOut.close();
            }
        }
    }

    protected Collection<ObjEntity> entitiesForCurrentMode() {

        // TODO: andrus, 12/2/2007 - should we setup a dummy entity for an empty map in
        // DataMap mode?
        if (mode != ClassGeneratorMode.entity && !objEntities.isEmpty()) {
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

        TemplateProcessor generator = new TemplateProcessor(classTemplate, context);

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

            generator.generateClass(
                    out,
                    dataMap,
                    ent,
                    fqnBaseClass,
                    fqnSuperClass,
                    fqnSubClass);
            out.close();
        }
    }

    /**
     * Runs class generation.
     */
    public void execute() throws Exception {
        validateAttributes();

        if (makePairs) {
            String t = getTemplateForPairs();
            String st = getSupertemplateForPairs();
            generateClassPairs(t, st, SUPERCLASS_PREFIX);
        }
        else {
            generateSingleClasses(getTemplateForSingles(), SUPERCLASS_PREFIX);
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
    protected Writer openWriter(ObjEntity entity, String pkgName, String className)
            throws Exception {

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

        return (getEncoding() != null)
                ? new OutputStreamWriter(out, getEncoding())
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
        return file.lastModified() <= getTimestamp();
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

    /**
     * Returns internal timestamp of this generator used to make decisions about
     * overwriting individual files.
     */
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns file encoding for the generated files.
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Sets file encoding. If set to null, default system encoding will be used.
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
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
    }

    public void setMode(ClassGeneratorMode mode) {
        this.mode = mode;
    }

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
