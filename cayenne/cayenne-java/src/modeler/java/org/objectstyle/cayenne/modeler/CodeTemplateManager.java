/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.modeler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.objectstyle.cayenne.gen.MapClassGenerator;
import org.objectstyle.cayenne.modeler.pref.FSPath;
import org.objectstyle.cayenne.pref.Domain;

/**
 * Manages code generation templates.
 * 
 * @author Andrus Adamchik
 */
public class CodeTemplateManager {

    public static final String STANDARD_SERVER_SUPERCLASS = "Standard Server Superclass";
    public static final String STANDARD_SERVER_SUBCLASS = "Standard Server Subclass";

    protected Map standardSubclassTemplates;
    protected Map standardSuperclassTemplates;
    protected Map customTemplates;

    public static Domain getTemplateDomain(Application application) {
        return application.getPreferenceDomain().getSubdomain(CodeTemplateManager.class);
    }

    public CodeTemplateManager(Application application) {
        standardSuperclassTemplates = new HashMap();

        standardSuperclassTemplates.put(
                STANDARD_SERVER_SUPERCLASS,
                MapClassGenerator.SUPERCLASS_TEMPLATE_1_1);

        standardSuperclassTemplates.put(
                "Standard Client Superclass",
                MapClassGenerator.CLIENT_SUPERCLASS_TEMPLATE_1_2);

        standardSubclassTemplates = new HashMap();
        standardSubclassTemplates.put(
                STANDARD_SERVER_SUBCLASS,
                MapClassGenerator.SUBCLASS_TEMPLATE_1_1);

        standardSubclassTemplates.put(
                "Standard Client Subclass",
                MapClassGenerator.CLIENT_SUBCLASS_TEMPLATE_1_2);

        updateCustomTemplates(getTemplateDomain(application));
    }

    /**
     * Updates custom templates from preferences.
     */
    public void updateCustomTemplates(Domain preferenceDomain) {
        Map templates = preferenceDomain.getDetailsMap(FSPath.class);
        this.customTemplates = new HashMap(templates.size(), 1);
        Iterator it = templates.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();

            FSPath path = (FSPath) entry.getValue();
            customTemplates.put(entry.getKey(), path.getPath());
        }
    }

    public String getTemplatePath(String name) {
        Object value = customTemplates.get(name);
        if (value != null) {
            return value.toString();
        }

        value = standardSuperclassTemplates.get(name);

        if (value != null) {
            return value.toString();
        }

        value = standardSubclassTemplates.get(name);
        return value != null ? value.toString() : null;
    }

    public Map getCustomTemplates() {
        return customTemplates;
    }

    public Map getStandardSubclassTemplates() {
        return standardSubclassTemplates;
    }

    public Map getStandardSuperclassTemplates() {
        return standardSuperclassTemplates;
    }
}
