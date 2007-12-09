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

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.ObjEntity;
import org.apache.velocity.Template;

/**
 * @since 3.0
 * @author Andrus Adamchik
 * @deprecated since 3.0
 */
public class ClassGenerationAction1_1 extends ClassGenerationAction {

    public static final String SINGLE_CLASS_TEMPLATE = "dotemplates/singleclass.vm";
    public static final String SUBCLASS_TEMPLATE = "dotemplates/subclass.vm";
    public static final String SUPERCLASS_TEMPLATE = "dotemplates/superclass.vm";

    @Override
    protected String defaultSingleClassTemplateName() {
        return ClassGenerationAction1_1.SINGLE_CLASS_TEMPLATE;
    }

    @Override
    protected String defaultSubclassTemplateName() {
        return ClassGenerationAction1_1.SUBCLASS_TEMPLATE;
    }

    @Override
    protected String defaultSuperclassTemplateName() {
        return ClassGenerationAction1_1.SUPERCLASS_TEMPLATE;
    }

    @Override
    protected void generateClassPairs() throws Exception {

        Template superTemplate = superclassTemplate();
        Template classTemplate = subclassTemplate();
        String superPrefix = getSuperclassPrefix();
        
        ClassGenerationInfo mainGen = new ClassGenerationInfo();
        ClassGenerationInfo superGen = new ClassGenerationInfo();

        // prefix is needed for both generators
        mainGen.setSuperPrefix(superPrefix);
        superGen.setSuperPrefix(superPrefix);

        for (ObjEntity entity : entitiesForCurrentMode()) {

            // 1. do the superclass
            initClassGenerator(superGen, entity, true);

            Writer superOut = openWriter(superGen.getPackageName(), superPrefix
                    + superGen.getClassName());

            if (superOut != null) {
                superTemplate.merge(context, superOut);
                superOut.close();
            }

            // 2. do the main class
            initClassGenerator(mainGen, entity, false);
            Writer mainOut = openWriter(mainGen.getPackageName(), mainGen.getClassName());
            if (mainOut != null) {
                classTemplate.merge(context, mainOut);
                mainOut.close();
            }
        }

        context.remove("classGen");
    }

    @Override
    protected void generateSingleClasses()
            throws Exception {

        Template classTemplate = singleClassTemplate();
        ClassGenerationInfo mainGen = new ClassGenerationInfo();

        for (ObjEntity entity : entitiesForCurrentMode()) {

            initClassGenerator(mainGen, entity, false);
            Writer out = openWriter(mainGen.getPackageName(), mainGen.getClassName());
            if (out != null) {
                classTemplate.merge(context, out);
                out.close();
            }
        }
    }

    /**
     * Initializes ClassGenerationInfo with class name and package of a generated class.
     */
    private void initClassGenerator(
            ClassGenerationInfo generatorInfo,
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

        generatorInfo.setPackageName(pkg);
        generatorInfo.setClassName(cname);
        if (entity.getSuperClassName() != null) {
            generatorInfo.setSuperClassName(entity.getSuperClassName());
        }
        else {
            generatorInfo.setSuperClassName(CayenneDataObject.class.getName());
        }
        generatorInfo.setSuperPackageName(spkg);

        generatorInfo.setObjEntity(entity);
        context.put("classGen", generatorInfo);
    }
}
