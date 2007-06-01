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
package org.objectstyle.cayenne.wocompat;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.exp.ExpressionException;
import org.objectstyle.cayenne.exp.parser.ASTObjPath;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * A descriptor of SelectQuery loaded from EOModel. It is an informal "decorator" of
 * Cayenne SelectQuery to provide access to the extra information of WebObjects
 * EOFetchSpecification.
 * 
 * @author Andrei Adamchik
 * @since 1.1
 */
public class EOQuery extends SelectQuery {

    protected Map plistMap;
    protected Map bindings;

    public EOQuery(ObjEntity root, Map plistMap) {
        super(root);
        this.plistMap = plistMap;
        initFromPlist(plistMap);
    }

    protected void initFromPlist(Map plistMap) {

        setResolvingInherited("YES".equalsIgnoreCase((String) plistMap.get("isDeep")));
        setRefreshingObjects("YES".equalsIgnoreCase((String) plistMap
                .get("refreshesRefetchedObjects")));

        setDistinct("YES".equalsIgnoreCase((String) plistMap.get("usesDistinct")));

        Object fetchLimit = plistMap.get("fetchLimit");
        if (fetchLimit != null) {
            try {
                if (fetchLimit instanceof Number) {
                    setFetchLimit(((Number) fetchLimit).intValue());
                } else {
                    setFetchLimit(Integer.parseInt(fetchLimit.toString()));
                }
            }
            catch (NumberFormatException nfex) {
                // ignoring...
            }
        }

        // sort orderings
        List orderings = (List) plistMap.get("sortOrderings");
        if (orderings != null && !orderings.isEmpty()) {
            Iterator it = orderings.iterator();
            while (it.hasNext()) {
                Map ordering = (Map) it.next();
                boolean asc = !"compareDescending:".equals(ordering.get("selectorName"));
                String key = (String) ordering.get("key");
                if (key != null) {
                    addOrdering(key, asc);
                }
            }
        }

        // TODO: add support for qualifier parsing...
    }

    public String getEOName() {
        if (getRoot() instanceof EOObjEntity) {
            return ((EOObjEntity) getRoot()).localQueryName(getName());
        } else {
            return getName();
        }
    }

    public Collection getBindingNames() {
        if (bindings == null) {
            initBindings();
        }

        return bindings.keySet();
    }

    public String bindingClass(String name) {
        if (bindings == null) {
            initBindings();
        }

        return (String) bindings.get(name);
    }

    private synchronized void initBindings() {
        if (bindings != null) {
            return;
        }

        bindings = new HashMap();

        if (!(getRoot() instanceof Entity)) {
            return;
        }

        Map qualifier = (Map) plistMap.get("qualifier");
        initBindings(bindings, (Entity) getRoot(), qualifier);
    }

    private void initBindings(Map bindings, Entity entity, Map qualifier) {
        if (qualifier == null) {
            return;
        }

        if ("EOKeyValueQualifier".equals(qualifier.get("class"))) {
            String key = (String) qualifier.get("key");
            if (key == null) {
                return;
            }

            Object value = qualifier.get("value");
            if (!(value instanceof Map)) {
                return;
            }

            Map valueMap = (Map) value;
            if (!"EOQualifierVariable".equals(valueMap.get("class"))
                    || !valueMap.containsKey("_key")) {
                return;
            }

            String name = (String) valueMap.get("_key");
            String className = null;

            // we don't know whether its obj path or db path, so the expression can blow
            // ... in fact we can't support DB Path as the key is different from external name, 
            // so we will use Object type for all DB path...
            try {
                Object lastObject = new ASTObjPath(key).evaluate(entity);

                if (lastObject instanceof ObjAttribute) {
                    className = ((ObjAttribute) lastObject).getType();
                } else if (lastObject instanceof ObjRelationship) {
                    ObjEntity target = (ObjEntity) ((ObjRelationship) lastObject)
                            .getTargetEntity();
                    if (target != null) {
                        className = target.getClassName();
                    }
                }
            }
            catch (ExpressionException ex) {
                className = "java.lang.Object";
            }

            if (className == null) {
                className = "java.lang.Object";
            }

            bindings.put(name, className);

            return;
        }

        List children = (List) qualifier.get("qualifiers");
        if (children != null) {
            Iterator it = children.iterator();
            while (it.hasNext()) {
                initBindings(bindings, entity, (Map) it.next());
            }
        }
    }
}