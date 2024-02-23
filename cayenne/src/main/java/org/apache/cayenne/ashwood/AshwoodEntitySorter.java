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

package org.apache.cayenne.ashwood;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.access.flush.operation.DbRowOp;
import org.apache.cayenne.ashwood.graph.Digraph;
import org.apache.cayenne.ashwood.graph.IndegreeTopologicalSort;
import org.apache.cayenne.ashwood.graph.MapDigraph;
import org.apache.cayenne.ashwood.graph.StrongConnection;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.reflect.ClassDescriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Implements dependency sorting algorithms for ObjEntities, DbEntities and
 * Persistent objects. Presently it works for acyclic database schemas with possible
 * multi-reflexive tables.
 * 
 * @since 3.1
 */
public class AshwoodEntitySorter implements EntitySorter {

	protected EntityResolver entityResolver;
	protected Map<DbEntity, ComponentRecord> components;
	protected Map<DbEntity, List<DbRelationship>> reflexiveDbEntities;

	protected Comparator<DbEntity> dbEntityComparator;
	protected Comparator<ObjEntity> objEntityComparator;

	private volatile boolean dirty;

	public AshwoodEntitySorter() {
		dbEntityComparator = new DbEntityComparator();
		objEntityComparator = new ObjEntityComparator();
		dirty = true;
	}

	/**
	 * Reindexes internal sorter in a thread-safe manner.
	 */
	protected void indexSorter() {

		// correct double check locking per Joshua Bloch
		// http://java.sun.com/developer/technicalArticles/Interviews/bloch_effective_08_qa.html
		// (maybe we should use something like CountDownLatch or a Cyclic
		// barrier
		// instead?)

		boolean localDirty = dirty;
		if (localDirty) {
			synchronized (this) {
				localDirty = dirty;
				if (localDirty) {
					doIndexSorter();
					dirty = false;
				}
			}
		}
	}

	/**
	 * Reindexes internal sorter without synchronization.
	 */
	protected void doIndexSorter() {

		Map<DbEntity, List<DbRelationship>> reflexiveDbEntities = new HashMap<>();
		Digraph<DbEntity, List<DbAttribute>> referentialDigraph = new MapDigraph<>();

		if (entityResolver != null) {
			for (DbEntity entity : entityResolver.getDbEntities()) {
				referentialDigraph.addVertex(entity);
			}
		}

		for (DbEntity destination : entityResolver.getDbEntities()) {
			for (DbRelationship candidate : destination.getRelationships()) {
				if ((!candidate.isToMany() && !candidate.isToDependentPK()) || candidate.isToMasterPK()) {
					DbEntity origin = candidate.getTargetEntity();
					boolean newReflexive = destination.equals(origin);

					for (DbJoin join : candidate.getJoins()) {
						DbAttribute targetAttribute = join.getTarget();
						if (targetAttribute.isPrimaryKey()) {

							if (newReflexive) {
								List<DbRelationship> reflexiveRels = reflexiveDbEntities
										.computeIfAbsent(destination, k -> new ArrayList<>(1));
								reflexiveRels.add(candidate);
								newReflexive = false;
							}

							List<DbAttribute> fks = referentialDigraph.getArc(origin, destination);
							if (fks == null) {
								fks = new ArrayList<>();
								referentialDigraph.putArc(origin, destination, fks);
							}

							fks.add(targetAttribute);
						}
					}
				}
			}
		}

		StrongConnection<DbEntity, List<DbAttribute>> contractor = new StrongConnection<>(referentialDigraph);

		Digraph<Collection<DbEntity>, Collection<List<DbAttribute>>> contractedReferentialDigraph = new MapDigraph<>();
		contractor.contract(contractedReferentialDigraph);

		IndegreeTopologicalSort<Collection<DbEntity>> sorter = new IndegreeTopologicalSort<>(
				contractedReferentialDigraph);

		Map<DbEntity, ComponentRecord> components = new HashMap<>(contractedReferentialDigraph.order());
		int componentIndex = 0;
		while (sorter.hasNext()) {
			Collection<DbEntity> component = sorter.next();
			ComponentRecord rec = new ComponentRecord(componentIndex++, component);

			for (DbEntity table : component) {
				components.put(table, rec);
			}
		}

		this.reflexiveDbEntities = reflexiveDbEntities;
		this.components = components;
	}

	/**
	 * @since 3.1
	 */
	@Override
	public void setEntityResolver(EntityResolver entityResolver) {
		this.entityResolver = entityResolver;
		this.entityResolver.setEntitySorter(this);
		this.dirty = true;
	}

	@Override
	public void sortDbEntities(List<DbEntity> dbEntities, boolean deleteOrder) {
		indexSorter();
		dbEntities.sort(getDbEntityComparator(deleteOrder));
	}

	@Override
	public void sortObjEntities(List<ObjEntity> objEntities, boolean deleteOrder) {
		indexSorter();
		objEntities.sort(getObjEntityComparator(deleteOrder));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void sortObjectsForEntity(ObjEntity objEntity, List<?> objects, boolean deleteOrder) {
		if(objects == null || objects.size() == 0) {
			return;
		}

		indexSorter();
		DbEntity dbEntity = objEntity.getDbEntity();
		// if no sorting is required
		if (!isReflexive(dbEntity)) {
			return;
		}

		Object probe = objects.get(0);
		if (probe instanceof DbRowOp) {
			sortObjectsForEntity(objEntity, (List<DbRowOp>) objects, deleteOrder, DbRowOp::getObject);
		} else if(probe instanceof Persistent) {
			sortObjectsForEntity(objEntity, (List<Persistent>) objects, deleteOrder, Function.identity());
		} else {
			throw new IllegalArgumentException("Can sort only Persistent or DbRow objects, got " + probe.getClass().getSimpleName());
		}
	}

	protected <E> void sortObjectsForEntity(ObjEntity objEntity, List<E> objects, boolean deleteOrder, Function<E, Persistent> converter) {
		Digraph<E, Boolean> objectDependencyGraph = buildDigraph(objEntity, objects, converter);

		if(!topologicalSort(objects, objectDependencyGraph, deleteOrder)) {
			throw new CayenneRuntimeException("Sorting objects for %s failed. Cycles found."
					, objEntity.getClassName());
		}
	}

	protected <E> Digraph<E, Boolean> buildDigraph(ObjEntity objEntity, List<E> objects, Function<E, Persistent> converter) {
		EntityResolver resolver = converter.apply(objects.get(0)).getObjectContext().getEntityResolver();
		ClassDescriptor descriptor = resolver.getClassDescriptor(objEntity.getName());
		String[] reflexiveRelNames = getReflexiveRelationshipsNames(objEntity);

		int size = objects.size();
		Digraph<E, Boolean> objectDependencyGraph = new MapDigraph<>();
		Persistent[] masters = new Persistent[reflexiveRelNames.length];
		for (int i = 0; i < size; i++) {
			E current = objects.get(i);
			objectDependencyGraph.addVertex(current);
			int actualMasterCount = 0;
			for (int k = 0; k < reflexiveRelNames.length; k++) {
				String reflexiveRelName = reflexiveRelNames[k];

				if (reflexiveRelName == null) {
					continue;
				}

				Persistent persistent = converter.apply(current);
				masters[k] = (Persistent)descriptor.getProperty(reflexiveRelName).readProperty(persistent);

				if (masters[k] == null) {
					masters[k] = findReflexiveMaster(persistent, objEntity.getRelationship(reflexiveRelName)
							, persistent.getObjectId().getEntityName());
				}

				if (masters[k] != null) {
					actualMasterCount++;
				}
			}

			int mastersFound = 0;
			for (int j = 0; j < size && mastersFound < actualMasterCount; j++) {

				if (i == j) {
					continue;
				}

				E masterCandidate = objects.get(j);
				for (Persistent master : masters) {
					if (converter.apply(masterCandidate) == master) {
						objectDependencyGraph.putArc(masterCandidate, current, Boolean.TRUE);
						mastersFound++;
					}
				}
			}
		}
		return objectDependencyGraph;
	}

	protected <E> boolean topologicalSort(List<E> data, Digraph<E, Boolean> graph, boolean reverse) {
		IndegreeTopologicalSort<E> sorter = new IndegreeTopologicalSort<>(graph);
		List<E> sorted = new ArrayList<>(data.size());

		while (sorter.hasNext()) {
			E o = sorter.next();
			if (o == null) {
				return false;
			}
			sorted.add(o);
		}

		// since API requires sorting within the same array,
		// simply replace all objects with objects in the right order...
		// may come up with something cleaner later
		data.clear();
		data.addAll(sorted);

		if (reverse) {
			Collections.reverse(data);
		}
		return true;
	}

	protected String[] getReflexiveRelationshipsNames(ObjEntity objEntity) {
		List<DbRelationship> reflexiveRels = reflexiveDbEntities.get(objEntity.getDbEntity());
		String[] reflexiveRelNames = new String[reflexiveRels.size()];
		for (int i = 0; i < reflexiveRelNames.length; i++) {
			DbRelationship dbRel = reflexiveRels.get(i);
			ObjRelationship objRel = (dbRel != null ? objEntity.getRelationshipForDbRelationship(dbRel) : null);
			reflexiveRelNames[i] = (objRel != null ? objRel.getName() : null);
		}
		return reflexiveRelNames;
	}

	protected Persistent findReflexiveMaster(Persistent object, ObjRelationship toOneRel, String targetEntityName) {

		DbRelationship finalRel = toOneRel.getDbRelationships().get(0);
		ObjectContext context = object.getObjectContext();

		// find committed snapshot - so we can't fetch from the context as it will return dirty snapshot;
		// must go down the stack instead

		// how do we handle this for NEW objects correctly? For now bail from the method
		if (object.getObjectId().isTemporary()) {
			return null;
		}

		ObjectIdQuery query = new ObjectIdQuery(object.getObjectId(), true, ObjectIdQuery.CACHE);
		QueryResponse response = context.getChannel().onQuery(null, query);
		List<?> result = response.firstList();
		if (result == null || result.size() == 0) {
			return null;
		}

		DataRow snapshot = (DataRow) result.get(0);

		ObjectId id = snapshot.createTargetObjectId(targetEntityName, finalRel);

		// not using 'localObject', looking up in context instead, as within the sorter
		// we only care about objects participating in transaction, so no need to create hollow objects
		return (id != null) ? (Persistent) context.getGraphManager().getNode(id) : null;
	}

	@Override
	public Comparator<DbEntity> getDbEntityComparator() {
		indexSorter();
		return dbEntityComparator;
	}

	@Override
	public Comparator<ObjEntity> getObjEntityComparator() {
		indexSorter();
		return objEntityComparator;
	}

	protected Comparator<DbEntity> getDbEntityComparator(boolean dependantFirst) {
		Comparator<DbEntity> c = dbEntityComparator;
		if (dependantFirst) {
			c = c.reversed();
		}
		return c;
	}

	protected Comparator<ObjEntity> getObjEntityComparator(boolean dependantFirst) {
		Comparator<ObjEntity> c = objEntityComparator;
		if (dependantFirst) {
			c = c.reversed();
		}
		return c;
	}

	@Override
	public boolean isReflexive(DbEntity metadata) {
		indexSorter();
		return reflexiveDbEntities.containsKey(metadata);
	}

	private final class ObjEntityComparator implements Comparator<ObjEntity> {

		@Override
		public int compare(ObjEntity o1, ObjEntity o2) {
			if (o1 == o2) {
				return 0;
			}
			DbEntity t1 = o1.getDbEntity();
			DbEntity t2 = o2.getDbEntity();
			return dbEntityComparator.compare(t1, t2);
		}
	}

	private final class DbEntityComparator implements Comparator<DbEntity> {

		@Override
		public int compare(DbEntity t1, DbEntity t2) {

			if (t1 == t2) {
				return 0;
			}
			if (t1 == null) {
				return -1;
			} else if (t2 == null) {
				return 1;
			}
			else {
				ComponentRecord rec1 = components.get(t1);
				ComponentRecord rec2 = components.get(t2);

				if(rec1 == null) {
					throw new NullPointerException("No record for DbEntity: " + t1);
				}

				if(rec2 == null) {
					throw new NullPointerException("No record for DbEntity: " + t2);
				}

				int index1 = rec1.index;
				int index2 = rec2.index;

				int result = Integer.compare(index1, index2);

				// TODO: is this check really needed?
				if (result != 0 && rec1.component == rec2.component) {
					result = 0;
				}

				return result;
			}
		}
	}

	private final static class ComponentRecord {

		ComponentRecord(int index, Collection<DbEntity> component) {
			this.index = index;
			this.component = component;
		}

		int index;
		Collection<DbEntity> component;
	}

}
