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

package org.apache.cayenne.exp.parser;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SelectById;
import org.apache.cayenne.util.CayenneMapEntry;

/**
 * Path expression traversing DB relationships and attributes.
 * 
 * @since 1.1
 */
public class ASTDbPath extends ASTPath {

	private static final long serialVersionUID = 6623715674339310782L;

	public static final String DB_PREFIX = "db:";

	ASTDbPath(int id) {
		super(id);
	}

	public ASTDbPath() {
		super(ExpressionParserTreeConstants.JJTDBPATH);
	}

	public ASTDbPath(String value) {
		super(ExpressionParserTreeConstants.JJTDBPATH);
		setPath(value);
	}

	public ASTDbPath(CayennePath value) {
		super(ExpressionParserTreeConstants.JJTDBPATH);
		setPath(value);
	}

	@Override
	protected Object evaluateNode(Object o) throws Exception {

		if (o instanceof Entity) {
			return evaluateEntityNode((Entity<?,?,?>) o);
		}

		Map<?, ?> map = toMap(o);
		String finalPathComponent = path.last().value();
		return (map != null) ? map.get(finalPathComponent) : null;
	}

	protected Map<?, ?> toMap(Object o) {
		if (o instanceof Map) {
			return (Map<?, ?>) o;
		} else if (o instanceof ObjectId) {
			return ((ObjectId) o).getIdSnapshot();
		} else if (o instanceof Persistent) {

			Persistent persistent = (Persistent) o;

			// before reading full snapshot, check if we can use smaller ID
			// snapshot ... it is much cheaper...
			return persistent.getObjectContext() != null ? toMap_AttachedObject(persistent.getObjectContext(),
					persistent) : toMap_DetachedObject(persistent);
		} else {
			return null;
		}
	}

	private Map<?, ?> toMap_AttachedObject(ObjectContext context, Persistent persistent) {
		return path.length() > 1
				? toMap_AttachedObject_MultiStepPath(context, persistent)
				: toMap_AttachedObject_SingleStepPath(context, persistent);
	}

	private Map<?, ?> toMap_AttachedObject_MultiStepPath(ObjectContext context, Persistent persistent) {
		Iterator<CayenneMapEntry> pathComponents = Cayenne.getObjEntity(persistent)
				.getDbEntity()
				.resolvePathComponents(this);
		LinkedList<DbRelationship> reversedPathComponents = new LinkedList<>();

		while (pathComponents.hasNext()) {
			CayenneMapEntry component = pathComponents.next();
			if (component instanceof DbRelationship) {
				DbRelationship rel = (DbRelationship) component;
				DbRelationship reverseRelationship = rel.getReverseRelationship();
				if (reverseRelationship == null) {
					reverseRelationship = rel.createReverseRelationship();
				}
				reversedPathComponents.addFirst(reverseRelationship);
			} else {
				break; // an attribute can only occur at the end of the path
			}
		}

		DbEntity finalEntity = reversedPathComponents.get(0).getSourceEntity();

		StringBuilder reversedPathStr = new StringBuilder();
		for (int i = 0; i < reversedPathComponents.size(); i++) {
			reversedPathStr.append(reversedPathComponents.get(i).getName());
			if (i < reversedPathComponents.size() - 1) {
				reversedPathStr.append('.');
			}
		}

		return ObjectSelect.dbQuery(finalEntity.getName())
				.where(ExpressionFactory.matchDbExp(reversedPathStr.toString(), persistent)).selectOne(context);
	}

	private Map<?, ?> toMap_AttachedObject_SingleStepPath(ObjectContext context, Persistent persistent) {
		ObjectId oid = persistent.getObjectId();

		// TODO: snapshotting API should not be limited to DataContext...
		if (context instanceof DataContext) {
			return ((DataContext) context).currentSnapshot(persistent);
		}

		if (oid != null) {
			return SelectById.dataRowQuery(persistent.getObjectId()).selectOne(context);
		}

		// fallback to ID snapshot as a last resort
		return toMap_DetachedObject(persistent);
	}

	private Map<?, ?> toMap_DetachedObject(Persistent persistent) {
		ObjectId oid = persistent.getObjectId();

		// returning null here is for backwards compatibility. Should we throw instead?
		return (oid != null) ? oid.getIdSnapshot() : null;
	}

	/**
	 * Creates a copy of this expression node, without copying children.
	 */
	@Override
	public Expression shallowCopy() {
		ASTDbPath copy = new ASTDbPath(id);
		copy.path = path;
		copy.setPathAliases(pathAliases);
		return copy;
	}

	/**
	 * @since 4.0
	 */
	@Override
	public void appendAsEJBQL(List<Object> parameterAccumulator, Appendable out, String rootId) throws IOException {
		// warning: non-standard EJBQL...
		out.append(DB_PREFIX);
		out.append(rootId);
		out.append('.');
		out.append(path.value());
	}

	/**
	 * @since 4.0
	 */
	@Override
	public void appendAsString(Appendable out) throws IOException {
		out.append(DB_PREFIX).append(path.value());
	}

	@Override
	public int getType() {
		return Expression.DB_PATH;
	}

	/**
	 * Helper method to evaluate path expression with Cayenne Entity.
	 */
	@Override
	protected CayenneMapEntry evaluateEntityNode(Entity<?,?,?> entity) {
		if(entity instanceof ObjEntity) {
			entity = ((ObjEntity) entity).getDbEntity();
		}
		return super.evaluateEntityNode(entity);
	}
}
