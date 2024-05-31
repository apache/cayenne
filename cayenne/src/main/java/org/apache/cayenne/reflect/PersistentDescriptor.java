/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.reflect;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityInheritanceTree;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A default ClassDescriptor implementation for persistent objects.
 * 
 * @since 3.0
 */
public class PersistentDescriptor implements ClassDescriptor {

	static final Integer TRANSIENT_STATE = PersistenceState.TRANSIENT;
	static final Integer HOLLOW_STATE = PersistenceState.HOLLOW;
	static final Integer COMMITTED_STATE = PersistenceState.COMMITTED;

	protected ClassDescriptor superclassDescriptor;

	// compiled properties ...
	protected Class<?> objectClass;
	protected Map<String, PropertyDescriptor> declaredProperties;
	protected Map<String, PropertyDescriptor> properties;

	protected Map<String, ClassDescriptor> subclassDescriptors;
	protected Accessor persistenceStateAccessor;

	protected ObjEntity entity;
	protected Collection<DbEntity> rootDbEntities;
	protected Map<CayennePath, AdditionalDbEntityDescriptor> additionalDbEntities;

	protected EntityInheritanceTree entityInheritanceTree;

	// combines declared and super properties
	protected Collection<AttributeProperty> idProperties;

	// combines declared and super properties
	protected Collection<ArcProperty> mapArcProperties;

	// inheritance information
	protected Collection<ObjAttribute> allDiscriminatorColumns;
	protected Expression entityQualifier;

	/**
	 * Creates a PersistentDescriptor.
	 */
	public PersistentDescriptor() {
		this.declaredProperties = new HashMap<>();
		this.properties = new HashMap<>();
		this.subclassDescriptors = new HashMap<>();

		// must be a set as duplicate addition attempts are expected...
		this.rootDbEntities = new HashSet<DbEntity>(1);
	}

	public void setDiscriminatorColumns(Collection<ObjAttribute> columns) {
		if (columns == null || columns.isEmpty()) {
			allDiscriminatorColumns = null;
		} else {
			allDiscriminatorColumns = new ArrayList<>(columns);
		}
	}

	/**
	 * Registers a superclass property.
	 */
	public void addSuperProperty(PropertyDescriptor property) {
		properties.put(property.getName(), property);
		indexAddedProperty(property);
	}

	/**
	 * Registers a property. This method is useful to customize default
	 * ClassDescriptor generated from ObjEntity by adding new properties or
	 * overriding the standard ones.
	 */
	public void addDeclaredProperty(PropertyDescriptor property) {
		declaredProperties.put(property.getName(), property);
		properties.put(property.getName(), property);
		indexAddedProperty(property);
	}

	/**
	 * Adds a root DbEntity to the list of roots, filtering duplicates.
	 */
	public void addRootDbEntity(DbEntity dbEntity) {
		this.rootDbEntities.add(dbEntity);
	}

	/**
	 * Adds additional DbEntity for this descriptor.
	 *
	 * @param path path for entity
	 * @param targetEntity additional entity
	 */
	void addAdditionalDbEntity(CayennePath path, DbEntity targetEntity, boolean noDelete) {
		if(additionalDbEntities == null) {
			additionalDbEntities = new HashMap<>();
		}

		additionalDbEntities.put(path, new AdditionalDbEntityDescriptor(path, targetEntity, noDelete));
	}

	void sortProperties() {

		// ensure properties are stored in predictable order per CAY-1729

		// 'properties' is a superset of 'declaredProperties', so let's sort all
		// properties, and populated both ordered collections at once
		if (properties.size() > 1) {

			List<Entry<String, PropertyDescriptor>> entries = new ArrayList<>(properties.entrySet());

			entries.sort(PropertyComparator.comparator);
			Map<String, PropertyDescriptor> orderedProperties = new LinkedHashMap<>((int) (entries.size() / 0.75));
			Map<String, PropertyDescriptor> orderedDeclared = new LinkedHashMap<>((int) (declaredProperties.size() / 0.75));

			for (Entry<String, PropertyDescriptor> e : entries) {
				orderedProperties.put(e.getKey(), e.getValue());

				if (declaredProperties.containsKey(e.getKey())) {
					orderedDeclared.put(e.getKey(), e.getValue());
				}
			}

			this.properties = orderedProperties;
			this.declaredProperties = orderedDeclared;
		}
	}

	void indexAddedProperty(PropertyDescriptor property) {
		if (property instanceof AttributeProperty) {

			AttributeProperty attributeProperty = (AttributeProperty) property;
			ObjAttribute attribute = attributeProperty.getAttribute();
			if (attribute.isPrimaryKey()) {

				if (idProperties == null) {
					idProperties = new ArrayList<>(2);
				}

				idProperties.add(attributeProperty);
			}
		} else if (property instanceof ArcProperty) {
			ObjRelationship relationship = ((ArcProperty) property).getRelationship();
			ObjRelationship reverseRelationship = relationship.getReverseRelationship();
			if (reverseRelationship != null && "java.util.Map".equals(reverseRelationship.getCollectionType())) {

				if (mapArcProperties == null) {
					mapArcProperties = new ArrayList<>(2);
				}

				mapArcProperties.add((ArcProperty) property);
			}
		}
	}

	/**
	 * Removes declared property. This method can be used to customize default
	 * ClassDescriptor generated from ObjEntity.
	 */
	public void removeDeclaredProperty(String propertyName) {
		PropertyDescriptor removed = declaredProperties.remove(propertyName);

		if (removed != null) {
			if (idProperties != null) {
				idProperties.remove(removed);
			}

			if (mapArcProperties != null) {
				mapArcProperties.remove(removed);
			}

			properties.remove(propertyName);
		}
	}

	/**
	 * Adds a subclass descriptor that maps to a given class name.
	 */
	public void addSubclassDescriptor(String className, ClassDescriptor subclassDescriptor) {
		// note that 'className' should be used instead of
		// "subclassDescriptor.getEntity().getClassName()", as this method is
		// called in
		// the early phases of descriptor initialization and we do not want to
		// trigger
		// subclassDescriptor resolution just yet to prevent stack overflow.
		subclassDescriptors.put(className, subclassDescriptor);
	}

	public ObjEntity getEntity() {
		return entity;
	}

	public Collection<DbEntity> getRootDbEntities() {
		return rootDbEntities;
	}

	@Override
	public Map<CayennePath, AdditionalDbEntityDescriptor> getAdditionalDbEntities() {
		if(additionalDbEntities == null) {
			return Collections.emptyMap();
		}
		return additionalDbEntities;
	}

	public boolean isFault(Object object) {
		if (superclassDescriptor != null) {
			return superclassDescriptor.isFault(object);
		}

		if (object == null) {
			return false;
		}

		return HOLLOW_STATE.equals(persistenceStateAccessor.getValue(object));
	}

	public Class<?> getObjectClass() {
		return objectClass;
	}

	void setObjectClass(Class<?> objectClass) {
		this.objectClass = objectClass;
	}

	public ClassDescriptor getSubclassDescriptor(Class<?> objectClass) {
		if (objectClass == null) {
			throw new IllegalArgumentException("Null objectClass");
		}

		if (subclassDescriptors.isEmpty()) {
			return this;
		}

		ClassDescriptor subclassDescriptor = subclassDescriptors.get(objectClass.getName());

		// ascend via the class hierarchy (only doing it if there are multiple
		// choices)
		if (subclassDescriptor == null) {
			Class<?> currentClass = objectClass;
			while (subclassDescriptor == null && (currentClass = currentClass.getSuperclass()) != null) {
				subclassDescriptor = subclassDescriptors.get(currentClass.getName());
			}
		}

		return subclassDescriptor != null ? subclassDescriptor : this;
	}

	public Collection<ObjAttribute> getDiscriminatorColumns() {
		return allDiscriminatorColumns != null ? allDiscriminatorColumns : Collections.<ObjAttribute> emptyList();
	}

	public Collection<AttributeProperty> getIdProperties() {

		if (idProperties != null) {
			return idProperties;
		}

		return Collections.emptyList();
	}

	public Collection<ArcProperty> getMapArcProperties() {

		if (mapArcProperties != null) {
			return mapArcProperties;
		}

		return Collections.emptyList();
	}

	/**
	 * Recursively looks up property descriptor in this class descriptor and all
	 * superclass descriptors.
	 */
	public PropertyDescriptor getProperty(String propertyName) {
		PropertyDescriptor property = getDeclaredProperty(propertyName);

		if (property == null && superclassDescriptor != null) {
			property = superclassDescriptor.getProperty(propertyName);
		}

		return property;
	}

	public PropertyDescriptor getDeclaredProperty(String propertyName) {
		return declaredProperties.get(propertyName);
	}

	/**
	 * Returns a descriptor of the mapped superclass or null if the descriptor's
	 * entity sits at the top of inheritance hierarchy.
	 */
	public ClassDescriptor getSuperclassDescriptor() {
		return superclassDescriptor;
	}

	/**
	 * Creates a new instance of a class described by this object.
	 */
	public Object createObject() {
		if (objectClass == null) {
			throw new NullPointerException("Null objectClass. Descriptor wasn't initialized properly.");
		}

		try {
			return objectClass.getDeclaredConstructor().newInstance();
		} catch (Throwable e) {
			throw new CayenneRuntimeException("Error creating object of class '" + objectClass.getName() + "'", e);
		}
	}

	/**
	 * Invokes 'prepareForAccess' of a super descriptor and then invokes
	 * 'prepareForAccess' of each declared property.
	 */
	public void injectValueHolders(Object object) throws PropertyException {

		// do super first
		if (getSuperclassDescriptor() != null) {
			getSuperclassDescriptor().injectValueHolders(object);
		}

		for (PropertyDescriptor property : declaredProperties.values()) {
			property.injectValueHolder(object);
		}
	}

	/**
	 * Copies object properties from one object to another. Invokes
	 * 'shallowCopy' of a super descriptor and then invokes 'shallowCopy' of
	 * each declared property.
	 */
	public void shallowMerge(final Object from, final Object to) throws PropertyException {
		injectValueHolders(to);

		visitProperties(new PropertyVisitor() {
			public boolean visitAttribute(AttributeProperty property) {
				property.writePropertyDirectly(to, property.readPropertyDirectly(to),
						property.readPropertyDirectly(from));
				return true;
			}

			public boolean visitToOne(ToOneProperty property) {
				property.invalidate(to);
				return true;
			}

			public boolean visitToMany(ToManyProperty property) {
				return true;
			}
		});

		if (from instanceof Persistent && to instanceof Persistent) {
			((Persistent) to).setSnapshotVersion(((Persistent) from).getSnapshotVersion());
		}
	}

	/**
	 * @since 3.0
	 */
	public boolean visitDeclaredProperties(PropertyVisitor visitor) {

		for (PropertyDescriptor next : declaredProperties.values()) {
			if (!next.visit(visitor)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * @since 3.0
	 */
	public boolean visitAllProperties(PropertyVisitor visitor) {
		if (!visitProperties(visitor)) {
			return false;
		}

		if (!subclassDescriptors.isEmpty()) {
			for (ClassDescriptor next : subclassDescriptors.values()) {
				if (!next.visitDeclaredProperties(visitor)) {
					return false;
				}
			}
		}

		return true;
	}

	public boolean visitProperties(PropertyVisitor visitor) {

		for (PropertyDescriptor next : properties.values()) {
			if (!next.visit(visitor)) {
				return false;
			}
		}

		return true;
	}

	public void setPersistenceStateAccessor(Accessor persistenceStateAccessor) {
		this.persistenceStateAccessor = persistenceStateAccessor;
	}

	public void setEntity(ObjEntity entity) {
		this.entity = entity;
	}

	public void setSuperclassDescriptor(ClassDescriptor superclassDescriptor) {
		this.superclassDescriptor = superclassDescriptor;
	}

	public Expression getEntityQualifier() {
		return entityQualifier;
	}

	public void setEntityQualifier(Expression entityQualifier) {
		this.entityQualifier = entityQualifier;
	}

	public EntityInheritanceTree getEntityInheritanceTree() {
		return entityInheritanceTree;
	}

	public void setEntityInheritanceTree(EntityInheritanceTree entityInheritanceTree) {
		this.entityInheritanceTree = entityInheritanceTree;
	}

	public boolean hasSubclasses() {
		return entityInheritanceTree != null && !entityInheritanceTree.getChildren().isEmpty();
	}
}
