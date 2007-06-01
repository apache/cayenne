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

import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.ObjEntity;

/**
 * Generates Java source code for ObjEntities in the DataMap. This class is
 * abstract and does not deal with filesystem issues directly. Concrete
 * subclasses should provide ways to store generated files by implementing
 * {@link #openWriter(ObjEntity, String, String)} and
 * {@link #closeWriter(Writer)}methods.
 * 
 * @author Andrei Adamchik
 */
public abstract class MapClassGenerator {

    public static final String SINGLE_CLASS_TEMPLATE = "dotemplates/singleclass.vm";
    public static final String SUBCLASS_TEMPLATE = "dotemplates/subclass.vm";
    public static final String SUPERCLASS_TEMPLATE = "dotemplates/superclass.vm";
    public static final String SUPERCLASS_PREFIX = "_";

    protected List objEntities;
    protected String superPkg;

    public MapClassGenerator() {}

    public MapClassGenerator(DataMap map) {
        this(new ArrayList(map.getObjEntities()));
    }

    public MapClassGenerator(List objEntities) {
        this.objEntities = objEntities;
    }

    protected String defaultSingleClassTemplate() {
        return SINGLE_CLASS_TEMPLATE;
    }

    protected String defaultSubclassTemplate() {
        return SUBCLASS_TEMPLATE;
    }

    protected String defaultSuperclassTemplate() {
        return SUPERCLASS_TEMPLATE;
    }

    /**
     * Creates a Writer to output source code for a given ObjEntity and Java
     * class.
     * 
     * @return Writer to store generated class source code or null if this class
     *         generation should be skipped.
     */
    public abstract Writer openWriter(
        ObjEntity entity,
        String pkgName,
        String className)
        throws Exception;

    /**
     * Closes writer after class code has been successfully written by
     * ClassGenerator.
     */
    public abstract void closeWriter(Writer out) throws Exception;

    /**
     * Runs class generation. Produces a pair of Java classes for each ObjEntity
     * in the map. Uses default Cayenne templates for classes.
     */
    public void generateClassPairs() throws Exception {
        generateClassPairs(SUBCLASS_TEMPLATE, SUPERCLASS_TEMPLATE, SUPERCLASS_PREFIX);
    }

    /**
     * Runs class generation. Produces a pair of Java classes for each ObjEntity
     * in the map. This allows developers to use generated <b>subclass </b> for
     * their custom code, while generated <b>superclass </b> will contain
     * Cayenne code. Superclass will be generated in the same package, its class
     * name will be derived from the class name by adding a
     * <code>superPrefix</code>.
     */
    public void generateClassPairs(
        String classTemplate,
        String superTemplate,
        String superPrefix)
        throws Exception {

        ClassGenerator mainGen = new ClassGenerator(classTemplate);
        ClassGenerator superGen = new ClassGenerator(superTemplate);

        // prefix is needed for both generators
        mainGen.setSuperPrefix(superPrefix);
        superGen.setSuperPrefix(superPrefix);

        Iterator it = objEntities.iterator();
        while (it.hasNext()) {
            ObjEntity ent = (ObjEntity) it.next();

            // 1. do the superclass
            initClassGenerator(superGen, ent, true);

            Writer superOut =
                openWriter(
                    ent,
                    superGen.getPackageName(),
                    superPrefix + superGen.getClassName());

            if (superOut != null) {
                superGen.generateClass(superOut, ent);
                closeWriter(superOut);
            }

            // 2. do the main class
            initClassGenerator(mainGen, ent, false);
            Writer mainOut =
                openWriter(ent, mainGen.getPackageName(), mainGen.getClassName());
            if (mainOut != null) {
                mainGen.generateClass(mainOut, ent);
                closeWriter(mainOut);
            }
        }
    }

    /** 
     * Runs class generation. Produces a single Java class for
     * each ObjEntity in the map. Uses default Cayenne templates for classes. 
     */
    public void generateSingleClasses() throws Exception {
        generateSingleClasses(SINGLE_CLASS_TEMPLATE);
    }

    /** 
     * Runs class generation. Produces a single Java class for
     * each ObjEntity in the map. 
     */
    public void generateSingleClasses(String classTemplate) throws Exception {
        ClassGenerator gen = new ClassGenerator(classTemplate);

        Iterator it = objEntities.iterator();
        while (it.hasNext()) {
            ObjEntity ent = (ObjEntity) it.next();
            initClassGenerator(gen, ent, false);
            Writer out = openWriter(ent, gen.getPackageName(), gen.getClassName());
            if (out == null) {
                continue;
            }

            gen.generateClass(out, ent);
            closeWriter(out);
        }
    }

    /** Initializes ClassGenerator with class name and package of a generated class. */
    protected void initClassGenerator(
        ClassGenerator gen,
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
            pkg =
                (superclass && superPkg != null) ? superPkg : fullClassName.substring(0, i);

            spkg =
                (!superclass && superPkg != null && !pkg.equals(superPkg)) ? superPkg : null;
        }

        // init generator
        gen.setPackageName(pkg);
        gen.setClassName(cname);
        if(entity.getSuperClassName()!=null) {
        	gen.setSuperClassName(entity.getSuperClassName());
        } else {
        	gen.setSuperClassName("org.objectstyle.cayenne.CayenneDataObject");
        }
        gen.setSuperPackageName(spkg);
    }

    /**
     * Returns "superPkg" property value -
     * a name of a superclass package that should be used
     * for all generated superclasses.
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

    public List getObjEntities() {
        return objEntities;
    }

    public void setObjEntities(List objEntities) {
        this.objEntities = objEntities;
    }

}