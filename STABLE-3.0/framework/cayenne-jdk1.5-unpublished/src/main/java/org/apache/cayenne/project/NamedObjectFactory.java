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

package org.apache.cayenne.project;

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SelectQuery;

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
 */
public abstract class NamedObjectFactory {
    private static final Map<Class, NamedObjectFactory> factories = new HashMap<Class, NamedObjectFactory>();

    static {
        factories.put(DataMap.class, new DataMapFactory());
        factories.put(ObjEntity.class, new ObjEntityFactory());
        factories.put(DbEntity.class, new DbEntityFactory());
        factories.put(ObjAttribute.class, new ObjAttributeFactory());
        factories.put(DbAttribute.class, new DbAttributeFactory());
        factories.put(DataNode.class, new DataNodeFactory());
        factories.put(DbRelationship.class, new DbRelationshipFactory(null, false));
        factories.put(ObjRelationship.class, new ObjRelationshipFactory(null, false));
        factories.put(DataDomain.class, new DataDomainFactory());
        factories.put(Procedure.class, new ProcedureFactory());
        factories.put(Query.class, new SelectQueryFactory());
        factories.put(ProcedureParameter.class, new ProcedureParameterFactory());
        factories.put(Embeddable.class, new EmbeddableFactory());
        factories.put(EmbeddableAttribute.class, new EmbeddableAttributeFactory());
    }

    public static String createName(Class objectClass, Object namingContext) {
        return (factories.get(objectClass)).makeName(namingContext);
    }
    
    /**
     * @since 1.0.5
     */
    public static String createName(Class objectClass, Object namingContext, String nameBase) {
        return (factories.get(objectClass)).makeName(namingContext, nameBase);
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
        return (factories.get(objectClass)).makeObject(
            namingContext);
    }

    /**
     * @since 1.0.5
     */
    public static Object createObject(
        Class<? extends DataMap> objectClass,
        Object namingContext,
        String nameBase) {
        return (factories.get(objectClass)).makeObject(
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
        @Override
        protected String nameBase() {
            return "UntitledDomain";
        }

        @Override
        protected Object create(String name, Object namingContext) {
            return new DataDomain(name);
        }

        @Override
        protected boolean isNameInUse(String name, Object namingContext) {
            Configuration config = (Configuration) namingContext;
            return config.getDomain(name) != null;
        }
    }

    static class DataMapFactory extends NamedObjectFactory {
        @Override
        protected String nameBase() {
            return "UntitledMap";
        }

        @Override
        protected Object create(String name, Object namingContext) {
            return new DataMap(name);
        }

        @Override
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
        @Override
        protected String nameBase() {
            return "UntitledObjEntity";
        }

        @Override
        protected Object create(String name, Object namingContext) {
            return new ObjEntity(name);
        }

        @Override
        protected boolean isNameInUse(String name, Object namingContext) {
            DataMap map = (DataMap) namingContext;
            return map.getObjEntity(name) != null;
        }
    }

    static class EmbeddableFactory extends NamedObjectFactory {
        
        private String nameBase;
        
        
        public String getNameBase() {
            return nameBase;
        }

        
        public void setNameBase(String nameBase) {
            this.nameBase = nameBase;
        }

        @Override
        protected String nameBase() {
            System.out.println("nameBase");
            if(getNameBase()==null){
                setNameBase("UntitledEmbeddable");
            }
            return getNameBase();
           
        }

        @Override
        protected Object create(String name, Object namingContext) {
            DataMap map = (DataMap) namingContext;
            if(map.getDefaultPackage() != null){
                return new Embeddable(map.getDefaultPackage() + "." + name);
            }
            return new Embeddable(name);
        }

        @Override
        protected boolean isNameInUse(String name, Object namingContext) {
            DataMap map = (DataMap) namingContext;
            if(map.getDefaultPackage() != null){
                return map.getEmbeddable((map.getDefaultPackage() + "." + name)) != null;
            }
            return map.getEmbeddable(name) != null;
        }
    }
    
    static class EmbeddableAttributeFactory extends NamedObjectFactory {
        @Override
        protected String nameBase() {
            return "untitledAttr";
        }

        @Override
        protected Object create(String name, Object namingContext) {
            return new EmbeddableAttribute(name);
        }

        @Override
        protected boolean isNameInUse(String name, Object namingContext) {
            Embeddable emb = (Embeddable) namingContext;
            return emb.getAttribute(name) != null;
        }
    }

    
    
    static class DbEntityFactory extends NamedObjectFactory {
        @Override
        protected String nameBase() {
            return "UntitledDbEntity";
        }

        @Override
        protected Object create(String name, Object namingContext) {
            return new DbEntity(name);
        }

        @Override
        protected boolean isNameInUse(String name, Object namingContext) {
            DataMap map = (DataMap) namingContext;
            return map.getDbEntity(name) != null;
        }
    }

    static class ProcedureParameterFactory extends NamedObjectFactory {
        @Override
        protected String nameBase() {
            return "UntitledProcedureParameter";
        }

        @Override
        protected Object create(String name, Object namingContext) {
            return new ProcedureParameter(name);
        }

        @Override
        protected boolean isNameInUse(String name, Object namingContext) {
        	
            // it doesn't matter if we create a parameter with 
            // a duplicate name.. parameters are positional anyway..
            // still try to use unique names for visual consistency
            Procedure procedure = (Procedure) namingContext;
            for (final ProcedureParameter parameter : procedure.getCallParameters()) {
                if (name.equals(parameter.getName())) {
                    return true;
                }
            }

            return false;
        }
    }

    static class ProcedureFactory extends NamedObjectFactory {
        @Override
        protected String nameBase() {
            return "UntitledProcedure";
        }

        @Override
        protected Object create(String name, Object namingContext) {
            return new Procedure(name);
        }

        @Override
        protected boolean isNameInUse(String name, Object namingContext) {
            DataMap map = (DataMap) namingContext;
            return map.getProcedure(name) != null;
        }
    }
    
    static class SelectQueryFactory extends NamedObjectFactory {
        @Override
        protected String nameBase() {
            return "UntitledQuery";
        }

        @Override
        protected Object create(String name, Object namingContext) {
            SelectQuery query = new SelectQuery();
            query.setName(name);
            return query;
        }

        @Override
        protected boolean isNameInUse(String name, Object namingContext) {
            DataMap map = (DataMap) namingContext;
            return map.getQuery(name) != null;
        }
    }

    static class ObjAttributeFactory extends NamedObjectFactory {
        @Override
        protected String nameBase() {
            return "untitledAttr";
        }

        @Override
        protected Object create(String name, Object namingContext) {
            return new ObjAttribute(name, null, (ObjEntity) namingContext);
        }

        @Override
        protected boolean isNameInUse(String name, Object namingContext) {
            Entity ent = (Entity) namingContext;
            return ent.getAttribute(name) != null;
        }
    }

    static class DbAttributeFactory extends ObjAttributeFactory {
        @Override
        protected Object create(String name, Object namingContext) {
            return new DbAttribute(
                name,
                TypesMapping.NOT_DEFINED,
                (DbEntity) namingContext);
        }
    }

    static class DataNodeFactory extends NamedObjectFactory {
        @Override
        protected String nameBase() {
            return "UntitledDataNode";
        }

        @Override
        protected Object create(String name, Object namingContext) {
            return new DataNode(name);
        }

        @Override
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

        @Override
        protected Object create(String name, Object namingContext) {
            return new ObjRelationship(name);
        }

        @Override
        protected boolean isNameInUse(String name, Object namingContext) {
            Entity ent = (Entity) namingContext;
            return ent.getRelationship(name) != null;
        }

        /** 
         * Returns generated name for the ObjRelationships. 
         * For to-one case and entity name "xxxx" it generates name "toXxxx".
         * For to-many case and entity name "Xxxx" it generates name "xxxxArray".
         */
        @Override
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

        @Override
        protected Object create(String name, Object namingContext) {
            return new DbRelationship(name);
        }

        /** 
         * Returns generated name for the DbRelationships. 
         * For to-one case it generates name "TO_XXXX".
         * For to-many case it generates name "XXXX_ARRAY". 
         */
        @Override
        protected String nameBase() {
            if (target == null) {
                return "untitledRel";
            }

            String name = target.getName();
            return (toMany) ? name + "_ARRAY" : "TO_" + name;
        }
    }
}
