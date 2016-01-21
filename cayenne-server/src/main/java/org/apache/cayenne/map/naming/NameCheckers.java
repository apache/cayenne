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

package org.apache.cayenne.map.naming;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.commons.lang.StringUtils;

/**
 * @since 4.0
 */
public enum NameCheckers implements NameChecker {

	dataChannelDescriptor("project") {
		@Override
		public boolean isNameInUse(Object namingContext, String name) {
			return false;
		}
	},

	dataMap("datamap") {
		@Override
		public boolean isNameInUse(Object namingContext, String name) {
			// null context is a situation when DataMap is a
			// top level object of the project
			if (namingContext == null) {
				return false;
			}

			if (namingContext instanceof DataDomain) {
				DataDomain domain = (DataDomain) namingContext;
				return domain.getDataMap(name) != null;
			}

			if (namingContext instanceof DataChannelDescriptor) {
				DataChannelDescriptor domain = (DataChannelDescriptor) namingContext;
				return domain.getDataMap(name) != null;
			}
			return false;
		}
	},

	reverseEngineering("reverseEngineering") {
		@Override
		public boolean isNameInUse(Object namingContext, String name) {
			if (namingContext == null) {
				return false;
			}

			for (DataMap dataMap : ((DataChannelDescriptor) namingContext).getDataMaps()) {
                if (dataMap!= null && dataMap.getReverseEngineering() != null &&
                        dataMap.getReverseEngineering().getName()!=null && dataMap.getReverseEngineering().getName().equals(name)) {
                    return true;
                }
			}
			return false;
		}
	},

	objEntity("ObjEntity") {
		@Override
		public boolean isNameInUse(Object namingContext, String name) {
			DataMap map = (DataMap) namingContext;
			return map.getObjEntity(name) != null;
		}
	},

	embeddable("Embeddable") {
		@Override
		public boolean isNameInUse(Object namingContext, String name) {
			DataMap map = (DataMap) namingContext;
			return map.getEmbeddable(map.getNameWithDefaultPackage(name)) != null;
		}
	},

	embeddableAttribute("untitledAttr") {
		@Override
		public boolean isNameInUse(Object namingContext, String name) {
			Embeddable emb = (Embeddable) namingContext;
			return emb.getAttribute(name) != null;
		}
	},

	dbEntity("db_entity") {
		@Override
		public boolean isNameInUse(Object namingContext, String name) {
			DataMap map = (DataMap) namingContext;
			return map.getDbEntity(name) != null;
		}
	},

	procedureParameter("UntitledProcedureParameter") {
		@Override
		public boolean isNameInUse(Object namingContext, String name) {

			// it doesn't matter if we create a parameter with
			// a duplicate name.. parameters are positional anyway..
			// still try to use unique names for visual consistency
			Procedure procedure = (Procedure) namingContext;
			for (final ProcedureParameter parameter : procedure
					.getCallParameters()) {
				if (name.equals(parameter.getName())) {
					return true;
				}
			}

			return false;
		}
	},

	procedure("procedure") {
		@Override
		public boolean isNameInUse(Object namingContext, String name) {
			DataMap map = (DataMap) namingContext;
			return map.getProcedure(name) != null;
		}
	},

	query("query") {
		@Override
		public boolean isNameInUse(Object namingContext, String name) {
			DataMap map = (DataMap) namingContext;
			return map.getQueryDescriptor(name) != null;
		}
	},

	objAttribute("untitledAttr") {
		@Override
		public boolean isNameInUse(Object namingContext, String name) {
			return objRelationship.isNameInUse(namingContext, name);
		}
	},

	dbAttribute("untitledAttr") {
		@Override
		public boolean isNameInUse(Object namingContext, String name) {
			Entity ent = (Entity) namingContext;
			return ent.getAttribute(name) != null
					|| ent.getRelationship(name) != null;
		}
	},

	dataNodeDescriptor("datanode") {
		@Override
		public boolean isNameInUse(Object namingContext, String name) {
			DataChannelDescriptor domain = (DataChannelDescriptor) namingContext;
			for (DataNodeDescriptor dataNodeDescriptor : domain
					.getNodeDescriptors()) {
				if (dataNodeDescriptor.getName().equals(name)) {
					return true;
				}
			}
			return false;
		}
	},

	objRelationship("untitledRel") {
		@Override
		public boolean isNameInUse(Object namingContext, String name) {
			ObjEntity ent = (ObjEntity) namingContext;
			return dbAttribute.isNameInUse(namingContext, name)
					|| ent.getCallbackMethods().contains(
					"get" + StringUtils.capitalize(name));
		}
	},

	dbRelationship("untitledRel") {
		@Override
		public boolean isNameInUse(Object namingContext, String name) {
			return dbAttribute.isNameInUse(namingContext, name);
		}
	},

	objCallbackMethod("ObjCallbackMethod") {
		@Override
		public boolean isNameInUse(Object namingContext, String name) {
			ObjEntity ent = (ObjEntity) namingContext;

			return name.startsWith("get")
					&& dbAttribute.isNameInUse(namingContext,
					StringUtils.uncapitalize(name.substring(3)))
					|| ent.getCallbackMethods().contains(name);
		}
	};

	public final String baseName;

	NameCheckers(String baseName) {
		this.baseName = baseName;
	}

	@Override
	public String baseName() {
		return baseName;
	}

}
