/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

package org.objectstyle.cayenne.gen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.ObjEntity;

/** 
 * Extends MapClassGenerator to allow target-specific filesystem locations 
 * where the files should go. Adds "execute" method that performs class 
 * generation based on the internal state of this object.
 * 
 * @author Andrei Adamchik
 */
public class DefaultClassGenerator extends MapClassGenerator {
    protected File destDir;
    protected boolean overwrite;
    protected boolean usePkgPath = true;
    protected boolean makePairs = true;
    protected File template;
    protected File superTemplate;
    protected long timestamp = System.currentTimeMillis();

    public DefaultClassGenerator() {}

    /** 
     * Creates class generator and initializes it with DataMap.
     * This will ensure generation of classes for all ObjEntities
     * in the DataMap.
     */
    public DefaultClassGenerator(DataMap map) {
        this(new ArrayList(map.getObjEntities()));
    }

    /** 
     * Creates class generator and initializes it with the list of ObjEntities
     * that will be used in class generation.
     */
    public DefaultClassGenerator(List objEntities) {
        super(objEntities);
    }

    /** Runs class generation. */
    public void execute() throws Exception {
        validateAttributes();

        if (makePairs) {
            String t = getTemplateForPairs();
            String st = getSupertemplateForPairs();
            generateClassPairs(t, st, MapClassGenerator.SUPERCLASS_PREFIX);
        } else {
            generateSingleClasses(getTemplateForSingles());
        }
    }

    /** 
     * Validates the state of this class generator. Throws exception if 
     * it is in inconsistent state. Called internally from "execute".
     */
    public void validateAttributes() throws Exception {
        if (destDir == null) {
            throw new Exception("'destDir' attribute is missing.");
        }

        if (!destDir.isDirectory()) {
            throw new Exception("'destDir' is not a directory.");
        }

        if (!destDir.canWrite()) {
            throw new Exception("Do not have write permissions for " + destDir);
        }

        if (template != null && !template.canRead()) {
            throw new Exception("Can't read template from " + template);
        }

        if (makePairs && superTemplate != null && !superTemplate.canRead()) {
            throw new Exception("Can't read super template from " + superTemplate);
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
    public void setTemplate(File template) {
        this.template = template;
    }

    /**
     * Sets <code>superTemplate</code> property.
     */
    public void setSuperTemplate(File superTemplate) {
        this.superTemplate = superTemplate;
    }

    /**
     * Sets <code>usepkgpath</code> property.
     */
    public void setUsePkgPath(boolean usePkgPath) {
        this.usePkgPath = usePkgPath;
    }

    public void closeWriter(Writer out) throws Exception {
        out.close();
    }

    public Writer openWriter(ObjEntity entity, String pkgName, String className)
        throws Exception {
        File outFile =
            (className.startsWith(SUPERCLASS_PREFIX))
                ? fileForSuperclass(pkgName, className)
                : fileForClass(pkgName, className);
        return (outFile != null) ? new FileWriter(outFile) : null;
    }

    /**
     * Returns a target file where a generated superclass must be saved. If null is returned,
     * class shouldn't be generated.
     */
    protected File fileForSuperclass(String pkgName, String className) throws Exception {

        File dest = new File(mkpath(destDir, pkgName), className + ".java");

		// Ignore if the destination is newer than the map
		// (internal timestamp), i.e. has been generated after the map was
		// last saved AND the template is older than the destination file
		if (dest.exists() && !isOld(dest)
			&& (superTemplate == null || superTemplate.lastModified() < dest.lastModified())) {
			return null;
		}
		
        return dest;
    }

    /**
     * Returns a target file where a generated class must be saved. If null is returned,
     * class shouldn't be generated.
     */
    protected File fileForClass(String pkgName, String className) throws Exception {

        File dest = new File(mkpath(destDir, pkgName), className + ".java");

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
            if (!isOld(dest)
                && (template == null || template.lastModified() < dest.lastModified())) {
                return null;
            }
        }

        return dest;
    }

    /** 
     * Returns true if <code>file</code> parameter is older than internal 
     * timestamp of this class generator.
     */
    protected boolean isOld(File file) {
        return file.lastModified() <= getTimestamp();
    }

    /** 
     *  Returns a File object corresponding to a directory where files
     *  that belong to <code>pkgName</code> package should reside. 
     *  Creates any missing diectories below <code>dest</code>.
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
    *  Returns template file path for Java class 
    *  when generating single classes. 
    */
    protected String getTemplateForSingles() throws IOException {
        return (template != null) ? template.getPath() : defaultSingleClassTemplate();
    }

    /** 
     *  Returns template file path for Java subclass 
     *  when generating class pairs. 
     */
    protected String getTemplateForPairs() throws IOException {
        return (template != null) ? template.getPath() : defaultSubclassTemplate();
    }

    /** 
     *  Returns template file path for Java superclass 
     *  when generating class pairs. 
     */
    protected String getSupertemplateForPairs() throws IOException {
        return (superTemplate != null)
            ? superTemplate.getPath()
            : defaultSuperclassTemplate();
    }

    /**
     * Returns internal timestamp of this generator used to make
     * decisions about overwriting individual files. 
     */
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}