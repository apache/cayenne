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
package org.objectstyle.cayenne.project;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.DerivedDbAttribute;
import org.objectstyle.cayenne.map.DerivedDbEntity;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.map.ProcedureParameter;
import org.objectstyle.cayenne.map.Relationship;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;

/** 
 * Factory class that generates various Cayenne objects with 
 * default names that are unique in their corresponding context. 
 * Supports creation of the following
 * objects:
 * <ul>
 *    <li>DataMap</li>
 *    <li>ObjEntity</li>
 *    <li>ObjAttribute</li>
 *    <li>ObjRelationship</li>
 *    <li>DbEntity</li>
 *    <li>DerivedDbEntity</li>
 *    <li>DbAttribute</li>
 *    <li>DerivedDbAttribute</li>
 *    <li>DbRelationship</li>
 *    <li>DataNode</li>
 *    <li>DataDomain</li>
 *    <li>Query</li>
 * 	  <li>Procedure</li>
 *    <li>ProcedureParameter</li>
 * </ul>
 * 
 * This is a helper class used mostly by GUI and database 
 * reengineering classes.
 * 
 * @author Andrei Adamchik
 */
public abstract class NamedObjectFactory {
    private static final Map factories = new HashMap();

    static {
        factories.put(DataMap.class, new DataMapFactory());
        factories.put(ObjEntity.class, new ObjEntityFactory());
        factories.put(DbEntity.class, new DbEntityFactory());
        factories.put(DerivedDbEntity.class, new DerivedDbEntityFactory());
        factories.put(ObjAttribute.class, new ObjAttributeFactory());
        factories.put(DbAttribute.class, new DbAttributeFactory());
        factories.put(DerivedDbAttribute.class, new DerivedDbAttributeFactory());
        factories.put(DataNode.class, new DataNodeFactory());
        factories.put(DbRelationship.class, new DbRelationshipFactory(null, false));
        factories.put(ObjRelationship.class, new ObjRelationshipFactory(null, false));
        factories.put(DataDomain.class, new DataDomainFactory());
        factories.put(Procedure.class, new ProcedureFactory());
        factories.put(Query.class, new SelectQueryFactory());
        factories.put(ProcedureParameter.class, new ProcedureParameterFactory());
    }

    public static String createName(Class objectClass, Object namingContext) {
        return ((NamedObjectFactory) factories.get(objectClass)).makeName(namingContext);
    }
    
    /**
     * @since 1.0.5
     */
    public static String createName(Class objectClass, Object namingContext, String nameBase) {
        return ((NamedObjectFactory) factories.get(objectClass)).makeName(namingContext, nameBase);
    }

    /**
     * Creates an object using an appropriate factory class.
     * If no factory is found for the object, NullPointerException is 
     * thrown. 
     * 
     * <p><i>Note that newly created object is not added to the parent.
     * This behavior can be changed later.</i></p>
     */
    public static Object createObject(Class objectClass, Object namingContext) {
        return ((NamedObjectFactory) factories.get(objectClass)).makeObject(
            namingContext);
    }

    /**
     * @since 1.0.5
     */
    public static Object createObject(
        Class objectClass,
        Object namingContext,
        String nameBase) {
        return ((NamedObjectFactory) factories.get(objectClass)).makeObject(
            namingContext,
            nameBase);
    }

    /**
     * Creates a relationship using an appropriate factory class.
     * If no factory is found for the object, NullPointerException is 
     * thrown. 
     * 
     * <p><i>Note that newly created object is not added to the parent.
     * This behavior can be changed later.</i></p>
     */
    public static Relationship createRelationship(
        Entity srcEnt,
        Entity targetEnt,
        boolean toMany) {
        NamedObjectFactory factory =
            (srcEnt instanceof ObjEntity)
                ? new ObjRelationshipFactory(targetEnt, toMany)
                : new DbRelationshipFactory(targetEnt, toMany);
        return (Relationship) factory.makeObject(srcEnt);
    }

    /**
     * Creates a unique name for the new object and constructs
     * this object.
     */
    protected synchronized String makeName(Object namingContext) {
        return  makeName(namingContext, nameBase());
    }
    
    /**
     * @since 1.0.5
     */
    protected synchronized String makeName(Object namingContext, String nameBase) {
        int c = 1;
        String name = nameBase;
        while (isNameInUse(name, namingContext)) {
            name = nameBase + c++;
        }
        
        return name;
    }

    /**
     * Creates a unique name for the new object and constructs this object.
     */
    protected Object makeObject(Object namingContext) {
        return makeObject(namingContext, nameBase());
    }
    
    /**
     * @since 1.0.5
     */
    protected Object makeObject(Object namingContext, String nameBase) {
        return create(makeName(namingContext, nameBase), namingContext);
    }

    /** Returns a base default name, like "UntitledEntity", etc. */
    protected abstract String nameBase();

    /** Internal factory method. Invoked after the name is figured out. */
    protected abstract Object create(String name, Object namingContext);

    /** 
     * Checks if the name is already taken by another sibling
     * in the same context.
     */
    protected abstract boolean isNameInUse(String name, Object namingContext);

    // concrete factories
    static class DataDomainFactory extends NamedObjectFactory {
        protected String nameBase() {
            return "UntitledDomain";
        }

        protected Object create(String name, Object namingContext) {
            return new DataDomain(name);
        }

        protected boolean isNameInUse(String name, Object namingContext) {
            Configuration config = (Configuration) namingContext;
            return config.getDomain(name) != null;
        }
    }

    static class DataMapFactory extends NamedObjectFactory {
        protected String nameBase() {
            return "UntitledMap";
        }

        protected Object create(String name, Object namingContext) {
            return new DataMap(name);
        }

        protected boolean isNameInUse(String name, Object namingContext) {
            // null context is a situation when DataMap is a
            // top level object of the project
            if (namingContext == null) {
                return false;
            }

            DataDomain domain = (DataDomain) namingContext;
            return domain.getMap(name) != null;
        }
    }

    static class ObjEntityFactory extends NamedObjectFactory {
        protected String nameBase() {
            return "UntitledObjEntity";
        }

        protected Object create(String name, Object namingContext) {
            return new ObjEntity(name);
        }

        protected boolean isNameInUse(String name, Object namingContext) {
            DataMap map = (DataMap) namingContext;
            return map.getObjEntity(name) != null;
        }
    }

    static class DbEntityFactory extends NamedObjectFactory {
        protected String nameBase() {
            return "UntitledDbEntity";
        }

        protected Object create(String name, Object namingContext) {
            return new DbEntity(name);
        }

        protected boolean isNameInUse(String name, Object namingContext) {
            DataMap map = (DataMap) namingContext;
            return map.getDbEntity(name) != null;
        }
    }

    static class ProcedureParameterFactory extends NamedObjectFactory {
        protected String nameBase() {
            return "UntitledProcedureParameter";
        }

        protected Object create(String name, Object namingContext) {
            return new ProcedureParameter(name);
        }

        protected boolean isNameInUse(String name, Object namingContext) {
        	
            // it doesn't matter if we create a parameter with 
            // a duplicate name.. parameters are positional anyway..
            // still try to use unique names for visual consistency
            Procedure procedure = (Procedure) namingContext;
            Iterator it = procedure.getCallParameters().iterator();
            while (it.hasNext()) {
                ProcedureParameter parameter = (ProcedureParameter) it.next();
                if (name.equals(parameter.getName())) {
                    return true;
                }
            }

            return false;
        }
    }

    static class ProcedureFactory extends NamedObjectFactory {
        protected String nameBase() {
            return "UntitledProcedure";
        }

        protected Object create(String name, Object namingContext) {
            return new Procedure(name);
        }

        protected boolean isNameInUse(String name, Object namingContext) {
            DataMap map = (DataMap) namingContext;
            return map.getProcedure(name) != null;
        }
    }
    
    static class SelectQueryFactory extends NamedObjectFactory {
        protected String nameBase() {
            return "UntitledQuery";
        }

        protected Object create(String name, Object namingContext) {
            SelectQuery query = new SelectQuery();
            query.setName(name);
            return query;
        }

        protected boolean isNameInUse(String name, Object namingContext) {
            DataMap map = (DataMap) namingContext;
            return map.getQuery(name) != null;
        }
    }

    static class DerivedDbEntityFactory extends DbEntityFactory {
        protected Object create(String name, Object namingContext) {
            return new DerivedDbEntity(name);
        }
    }

    static class ObjAttributeFactory extends NamedObjectFactory {
        protected String nameBase() {
            return "untitledAttr";
        }

        protected Object create(String name, Object namingContext) {
            return new ObjAttribute(name, null, (ObjEntity) namingContext);
        }

        protected boolean isNameInUse(String name, Object namingContext) {
            Entity ent = (Entity) namingContext;
            return ent.getAttribute(name) != null;
        }
    }

    static class DbAttributeFactory extends ObjAttributeFactory {
        protected Object create(String name, Object namingContext) {
            return new DbAttribute(
                name,
                TypesMapping.NOT_DEFINED,
                (DbEntity) namingContext);
        }
    }

    static class DerivedDbAttributeFactory extends ObjAttributeFactory {
        protected Object create(String name, Object namingContext) {
            return new DerivedDbAttribute(
                name,
                TypesMapping.NOT_DEFINED,
                (DbEntity) namingContext,
                DerivedDbAttribute.ATTRIBUTE_TOKEN);
        }
    }

    static class DataNodeFactory extends NamedObjectFactory {
        protected String nameBase() {
            return "UntitledDataNode";
        }

        protected Object create(String name, Object namingContext) {
            return new DataNode(name);
        }

        protected boolean isNameInUse(String name, Object namingContext) {
            DataDomain domain = (DataDomain) namingContext;
            return domain.getNode(name) != null;
        }
    }

    static class ObjRelationshipFactory extends NamedObjectFactory {
        protected Entity target;
        protected boolean toMany;

        public ObjRelationshipFactory(Entity target, boolean toMany) {
            this.target = target;
            this.toMany = toMany;
        }

        protected Object create(String name, Object namingContext) {
            return new ObjRelationship(name);
        }

        protected boolean isNameInUse(String name, Object namingContext) {
            Entity ent = (Entity) namingContext;
            return ent.getRelationship(name) != null;
        }

        /** 
         * Returns generated name for the ObjRelationships. 
         * For to-one case and entity name "xxxx" it generates name "toXxxx".
         * For to-many case and entity name "Xxxx" it generates name "xxxxArray".
         */
        protected String nameBase() {
            if (target == null) {
                return "untitledRel";
            }

            String name = target.getName();
            return (toMany)
                ? Character.toLowerCase(name.charAt(0)) + name.substring(1) + "Array"
                : "to" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
    }

    static class DbRelationshipFactory extends ObjRelationshipFactory {
        public DbRelationshipFactory(Entity target, boolean toMany) {
            super(target, toMany);
        }

        protected Object create(String name, Object namingContext) {
            return new DbRelationship(name);
        }

        /** 
         * Returns generated name for the DbRelationships. 
         * For to-one case it generates name "TO_XXXX".
         * For to-many case it generates name "XXXX_ARRAY". 
         */
        protected String nameBase() {
            if (target == null) {
                return "untitledRel";
            }

            String name = target.getName();
            return (toMany) ? name + "_ARRAY" : "TO_" + name;
        }
    }
}