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

package org.apache.cayenne.map;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.Persistent;

public class MockMappingNamespace implements MappingNamespace {

	private Map<String, DbEntity> dbEntities = new HashMap<>();
	private Map<String, ObjEntity> objEntities = new HashMap<>();
	private Map<String, QueryDescriptor> queryDescriptors = new HashMap<>();
	private Map<String, Procedure> procedures = new HashMap<>();

	public Embeddable getEmbeddable(String className) {
		return null;
	}

	public SQLResult getResult(String name) {
		return null;
	}

	public void addDbEntity(DbEntity entity) {
		dbEntities.put(entity.getName(), entity);
	}

	public void addObjEntity(ObjEntity entity) {
		objEntities.put(entity.getName(), entity);
	}

	public void addQueryDescriptor(QueryDescriptor queryDescriptor) {
		queryDescriptors.put(queryDescriptor.getName(), queryDescriptor);
	}

	public void addProcedure(Procedure procedure) {
		procedures.put(procedure.getName(), procedure);
	}

	public DbEntity getDbEntity(String name) {
		return dbEntities.get(name);
	}

	public ObjEntity getObjEntity(String name) {
		return objEntities.get(name);
	}

	public Procedure getProcedure(String name) {
		return procedures.get(name);
	}

	public QueryDescriptor getQueryDescriptor(String name) {
		return queryDescriptors.get(name);
	}

	public Collection<DbEntity> getDbEntities() {
		return dbEntities.values();
	}

	public Collection<ObjEntity> getObjEntities() {
		return objEntities.values();
	}

	public Collection<Procedure> getProcedures() {
		return procedures.values();
	}

	public Collection<QueryDescriptor> getQueryDescriptors() {
		return queryDescriptors.values();
	}

	public Collection<Embeddable> getEmbeddables() {
		// TODO Auto-generated method stub
		return null;
	}

	public EntityInheritanceTree getInheritanceTree(String entityName) {
		// TODO Auto-generated method stub
		return null;
	}

	public ObjEntity getObjEntity(Class<?> entityClass) {
		// TODO Auto-generated method stub
		return null;
	}

	public Collection<SQLResult> getResults() {
		// TODO Auto-generated method stub
		return null;
	}

	public ObjEntity getObjEntity(Persistent object) {
		// TODO Auto-generated method stub
		return null;
	}
}
