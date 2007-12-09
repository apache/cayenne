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

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.map.ObjEntity;
import org.apache.velocity.Template;

/**
 * @since 3.0
 * @author Andrus Adamchik
 */
public class ClientClassGenerationAction extends ClassGenerationAction {

    public static final String SUBCLASS_TEMPLATE = "dotemplates/v1_2/client-subclass.vm";
    public static final String SUPERCLASS_TEMPLATE = "dotemplates/v1_2/client-superclass.vm";

    @Override
    protected String defaultSingleClassTemplateName() {
        throw new IllegalStateException(
                "Default generation for single classes on the client is not supported.");
    }

    @Override
    protected String defaultSubclassTemplateName() {
        return ClientClassGenerationAction.SUBCLASS_TEMPLATE;
    }

    @Override
    protected String defaultSuperclassTemplateName() {
        return ClientClassGenerationAction.SUPERCLASS_TEMPLATE;
    }

    @Override
    protected void generateSingleClasses() throws Exception {
        throw new IllegalStateException(
                "Single classes generation is not supported for the client.");
    }

    @Override
    protected void generateClassPairs() throws Exception {

        Template superTemplate = superclassTemplate();
        Template classTemplate = subclassTemplate();
        String superPrefix = getSuperclassPrefix();

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

            Writer superOut = openWriter(superPackageName, superClassName);

            if (superOut != null) {
                context.put("objEntity", entity);
                context.put("stringUtils", StringUtils.getInstance());
                context.put("entityUtils", new EntityUtils(
                        dataMap,
                        entity,
                        fqnBaseClass,
                        fqnSuperClass,
                        fqnSubClass));
                context.put("importUtils", new ImportUtils());

                superTemplate.merge(context, superOut);
                superOut.close();
            }

            Writer mainOut = openWriter(subPackageName, subClassName);
            if (mainOut != null) {
                context.put("objEntity", entity);
                context.put("stringUtils", StringUtils.getInstance());
                context.put("entityUtils", new EntityUtils(
                        dataMap,
                        entity,
                        fqnBaseClass,
                        fqnSuperClass,
                        fqnSubClass));
                context.put("importUtils", new ImportUtils());

                classTemplate.merge(context, mainOut);
                mainOut.close();
            }
        }
    }
}
