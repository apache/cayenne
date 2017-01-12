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
package org.apache.cayenne.access.translator.select;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.translator.DbAttributeBinding;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.exp.TraversalHelper;
import org.apache.cayenne.exp.parser.ASTAggregateFunctionCall;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.JoinType;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.PathComponent;
import org.apache.cayenne.query.PrefetchSelectQuery;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.util.EqualsBuilder;
import org.apache.cayenne.util.HashCodeBuilder;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @since 4.0
 */
public class DefaultSelectTranslator extends QueryAssembler implements SelectTranslator {

	protected static final int[] UNSUPPORTED_DISTINCT_TYPES = { Types.BLOB, Types.CLOB, Types.NCLOB,
			Types.LONGVARBINARY, Types.LONGVARCHAR, Types.LONGNVARCHAR };

	protected static boolean isUnsupportedForDistinct(int type) {
		for (int unsupportedDistinctType : UNSUPPORTED_DISTINCT_TYPES) {
			if (unsupportedDistinctType == type) {
				return true;
			}
		}

		return false;
	}

	JoinStack joinStack;
	List<ColumnDescriptor> resultColumns;
	Map<ObjAttribute, ColumnDescriptor> attributeOverrides;
	Map<ColumnDescriptor, ObjAttribute> defaultAttributesByColumn;

	boolean suppressingDistinct;

	/**
	 * If set to <code>true</code>, indicates that distinct select query is
	 * required no matter what the original query settings where. This flag can
	 * be set when joins are created using "to-many" relationships.
	 */
	boolean forcingDistinct;

	/**
	 * Does this SQL have any aggregate function
	 */
	boolean haveAggregate;
	Map<ColumnDescriptor, List<DbAttributeBinding>> groupByColumns;


	public DefaultSelectTranslator(Query query, DbAdapter adapter, EntityResolver entityResolver) {
		super(query, adapter, entityResolver);
	}

	protected JoinStack getJoinStack() {
		if (joinStack == null) {
			joinStack = createJoinStack();
		}
		return joinStack;
	}

	protected JoinStack createJoinStack() {
		return new JoinStack(getAdapter(), queryMetadata.getDataMap(), this);
	}

	@Override
	protected void doTranslate() {

		DataMap dataMap = queryMetadata.getDataMap();
		JoinStack joins = getJoinStack();

		QuotingStrategy strategy = getAdapter().getQuotingStrategy();
		forcingDistinct = false;

		// build column list
		this.resultColumns = buildResultColumns();

		// build qualifier
		QualifierTranslator qualifierTranslator = adapter.getQualifierTranslator(this);
		StringBuilder whereQualifierBuffer = qualifierTranslator.appendPart(new StringBuilder());

		// build ORDER BY
		OrderingTranslator orderingTranslator = new OrderingTranslator(this);
		StringBuilder orderingBuffer = orderingTranslator.appendPart(new StringBuilder());

		// assemble
		StringBuilder queryBuf = new StringBuilder();
		queryBuf.append("SELECT ");

		// check if DISTINCT is appropriate
		// side effect: "suppressingDistinct" flag may end up being flipped here
		if (forcingDistinct || getSelectQuery().isDistinct()) {
			suppressingDistinct = false;

			for (ColumnDescriptor column : resultColumns) {
				if (isUnsupportedForDistinct(column.getJdbcType())) {
					suppressingDistinct = true;
					break;
				}
			}

			if (!suppressingDistinct) {
				queryBuf.append(buildDistinctStatement() + " ");
			}
		}

		// convert ColumnDescriptors to column names
		List<String> selectColumnExpList = new ArrayList<>();
		for (ColumnDescriptor column : resultColumns) {
			String fullName = strategy.quotedIdentifier(dataMap, column.getNamePrefix(), column.getName());
			selectColumnExpList.add(fullName);
		}

		// append any column expressions used in the order by if this query
		// uses the DISTINCT modifier
		if (forcingDistinct || getSelectQuery().isDistinct()) {
			List<String> orderByColumnList = orderingTranslator.getOrderByColumnList();
			for (String orderByColumnExp : orderByColumnList) {
				// Convert to ColumnDescriptors??
				if (!selectColumnExpList.contains(orderByColumnExp)) {
					selectColumnExpList.add(orderByColumnExp);
				}
			}
		}

		appendSelectColumns(queryBuf, selectColumnExpList);

		// append from clause
		queryBuf.append(" FROM ");

		// append tables and joins
		joins.appendRootWithQuoteSqlIdentifiers(queryBuf, getQueryMetadata().getDbEntity());

		joins.appendJoins(queryBuf);
		joins.appendQualifier(whereQualifierBuffer, whereQualifierBuffer.length() == 0);

		// append qualifier
		if (whereQualifierBuffer.length() > 0) {
			queryBuf.append(" WHERE ");
			queryBuf.append(whereQualifierBuffer);
		}

		if(haveAggregate && !groupByColumns.isEmpty()) {
			queryBuf.append(" GROUP BY ");
			appendGroupByColumns(queryBuf, groupByColumns);
		}

		// append HAVING qualifier
		QualifierTranslator havingQualifierTranslator = adapter.getQualifierTranslator(this);
		Expression havingQualifier = ((SelectQuery)query).getHavingQualifier();
		if(havingQualifier != null) {
			havingQualifierTranslator.setQualifier(havingQualifier);
			StringBuilder havingQualifierBuffer = havingQualifierTranslator.appendPart(new StringBuilder());
			if(havingQualifierBuffer.length() > 0) {
				queryBuf.append(" HAVING ");
				queryBuf.append(havingQualifierBuffer);
			}
		}

		// append prebuilt ordering
		if (orderingBuffer.length() > 0) {
			queryBuf.append(" ORDER BY ").append(orderingBuffer);
		}

		if (!isSuppressingDistinct()) {
			appendLimitAndOffsetClauses(queryBuf);
		}

		this.sql = queryBuf.toString();
	}

	/**
	 * Allows subclasses to insert their own dialect of DISTINCT statement to
	 * improve performance.
	 *
	 * @return string representing the DISTINCT statement
	 * @since 4.0
	 */
	protected String buildDistinctStatement() {
		return "DISTINCT";
	}

	/**
	 * @since 3.1
	 */
	protected void appendSelectColumns(StringBuilder buffer, List<String> selectColumnExpList) {

		// append columns (unroll the loop's first element)
		int columnCount = selectColumnExpList.size();
		buffer.append(selectColumnExpList.get(0));

		// assume there is at least 1 element
		for (int i = 1; i < columnCount; i++) {
			buffer.append(", ");
			buffer.append(selectColumnExpList.get(i));
		}
	}

	/**
	 * Append columns to GROUP BY clause
	 * @since 4.0
	 */
	protected void appendGroupByColumns(StringBuilder buffer, Map<ColumnDescriptor, List<DbAttributeBinding>>  groupByColumns) {
		Iterator<Map.Entry<ColumnDescriptor, List<DbAttributeBinding>>> it = groupByColumns.entrySet().iterator();
		Map.Entry<ColumnDescriptor, List<DbAttributeBinding>> entry = it.next();
		appendGroupByColumn(buffer, entry);
		while(it.hasNext()) {
			entry = it.next();
			buffer.append(", ");
			appendGroupByColumn(buffer, entry);
		}
	}

	/**
	 * Append single column to GROUP BY clause
	 * @since 4.0
	 */
	protected void appendGroupByColumn(StringBuilder buffer, Map.Entry<ColumnDescriptor, List<DbAttributeBinding>> entry) {
		if(entry.getKey().getDataRowKey().equals(entry.getKey().getName())) {
			buffer.append(entry.getKey().getName());
            for (DbAttributeBinding binding : entry.getValue()) {
                addToParamList(binding.getAttribute(), binding.getValue());
            }
        } else {
            buffer.append(entry.getKey().getDataRowKey());
        }
	}

	/**
	 * Handles appending optional limit and offset clauses. This implementation
	 * does nothing, deferring to subclasses to define the LIMIT/OFFSET clause
	 * syntax.
	 * 
	 * @since 3.0
	 */
	protected void appendLimitAndOffsetClauses(StringBuilder buffer) {

	}

	@Override
	public String getCurrentAlias() {
		return getJoinStack().getCurrentAlias();
	}

	/**
	 * Returns a list of ColumnDescriptors for the query columns.
	 * 
	 * @since 1.2
	 */
	public ColumnDescriptor[] getResultColumns() {
		if (resultColumns == null || resultColumns.isEmpty()) {
			return new ColumnDescriptor[0];
		}

		return resultColumns.toArray(new ColumnDescriptor[resultColumns.size()]);
	}

	/**
	 * Returns a map of ColumnDescriptors keyed by ObjAttribute for columns that
	 * may need to be reprocessed manually due to incompatible mappings along
	 * the inheritance hierarchy.
	 * 
	 * @since 1.2
	 */
	public Map<ObjAttribute, ColumnDescriptor> getAttributeOverrides() {
		if (attributeOverrides != null) {
			return attributeOverrides;
		} else {
			return Collections.emptyMap();
		}
	}

	/**
	 * Returns true if SelectTranslator determined that a query requiring
	 * DISTINCT can't be run with DISTINCT keyword for internal reasons. If this
	 * method returns true, DataNode may need to do in-memory distinct
	 * filtering.
	 * 
	 * @since 1.1
	 */
	public boolean isSuppressingDistinct() {
		return suppressingDistinct;
	}

	private SelectQuery<?> getSelectQuery() {
		return (SelectQuery<?>) getQuery();
	}

	protected List<ColumnDescriptor> buildResultColumns() {

		this.defaultAttributesByColumn = new HashMap<>();

		List<ColumnDescriptor> columns = new ArrayList<>();
		SelectQuery<?> query = getSelectQuery();

		if(query.getColumns() != null && !query.getColumns().isEmpty()) {
			appendOverridedColumns(columns, query);
		} else if (query.getRoot() instanceof DbEntity) {
			appendDbEntityColumns(columns, query);
		} else if (getQueryMetadata().getPageSize() > 0) {
			appendIdColumns(columns, query);
		} else {
			appendQueryColumns(columns, query);
		}

		return columns;
	}

	/**
	 * If query contains explicit column list, use only them
	 */
	<T> List<ColumnDescriptor> appendOverridedColumns(List<ColumnDescriptor> columns, SelectQuery<T> query) {
		groupByColumns = new HashMap<>();

		QualifierTranslator qualifierTranslator = adapter.getQualifierTranslator(this);
		AccumulatingBindingListener bindingListener = new AccumulatingBindingListener();
		setAddBindingListener(bindingListener);

		for(Property<?> property : query.getColumns()) {
			qualifierTranslator.setQualifier(property.getExpression());
			StringBuilder builder = qualifierTranslator.appendPart(new StringBuilder());

			int type = TypesMapping.getSqlTypeByJava(property.getType());

			String alias = property.getAlias();
			if(alias != null) {
				builder.append(" AS ").append(alias);
			}
			ColumnDescriptor descriptor = new ColumnDescriptor(builder.toString(), type);
			descriptor.setDataRowKey(alias);
			columns.add(descriptor);

			if(isAggregate(property)) {
				haveAggregate = true;
			} else {
				groupByColumns.put(descriptor, bindingListener.getBindings());
			}
			bindingListener.reset();
		}

		setAddBindingListener(null);

		if(!haveAggregate) {
			// if no expression with aggregation function found, we don't need this information
			groupByColumns.clear();
		}

		return columns;
	}

	private boolean isAggregate(Property<?> property) {
		final boolean[] isAggregate = new boolean[1];
		Expression exp = property.getExpression();
		exp.traverse(new TraversalHelper() {
			@Override
			public void startNode(Expression node, Expression parentNode) {
				if(node instanceof ASTAggregateFunctionCall) {
					isAggregate[0] = true;
				}
			}
		});

		return isAggregate[0];
	}

	<T> List<ColumnDescriptor> appendDbEntityColumns(List<ColumnDescriptor> columns, SelectQuery<T> query) {

		Set<ColumnTracker> attributes = new HashSet<>();

		DbEntity table = getQueryMetadata().getDbEntity();
		for (DbAttribute dba : table.getAttributes()) {
			appendColumn(columns, null, dba, attributes, null);
		}

		return columns;
	}

	/**
	 * Appends columns needed for object SelectQuery to the provided columns
	 * list.
	 */
	<T> List<ColumnDescriptor> appendQueryColumns(final List<ColumnDescriptor> columns, SelectQuery<T> query) {

		final Set<ColumnTracker> attributes = new HashSet<ColumnTracker>();

		// fetched attributes include attributes that are either:
		//
		// * class properties
		// * PK
		// * FK used in relationship
		// * joined prefetch PK

		ClassDescriptor descriptor = queryMetadata.getClassDescriptor();
		ObjEntity oe = descriptor.getEntity();

		PropertyVisitor visitor = new PropertyVisitor() {

			public boolean visitAttribute(AttributeProperty property) {
				ObjAttribute oa = property.getAttribute();

				resetJoinStack();
				Iterator<CayenneMapEntry> dbPathIterator = oa.getDbPathIterator();
				while (dbPathIterator.hasNext()) {
					Object pathPart = dbPathIterator.next();

					if (pathPart == null) {
						throw new CayenneRuntimeException("ObjAttribute has no component: " + oa.getName());
					} else if (pathPart instanceof DbRelationship) {
						DbRelationship rel = (DbRelationship) pathPart;
						dbRelationshipAdded(rel, JoinType.LEFT_OUTER, null);
					} else if (pathPart instanceof DbAttribute) {
						DbAttribute dbAttr = (DbAttribute) pathPart;

						appendColumn(columns, oa, dbAttr, attributes, null);
					}
				}
				return true;
			}

			public boolean visitToMany(ToManyProperty property) {
				visitRelationship(property);
				return true;
			}

			public boolean visitToOne(ToOneProperty property) {
				visitRelationship(property);
				return true;
			}

			private void visitRelationship(ArcProperty property) {
				resetJoinStack();

				ObjRelationship rel = property.getRelationship();
				DbRelationship dbRel = rel.getDbRelationships().get(0);

				List<DbJoin> joins = dbRel.getJoins();
				for (DbJoin join : joins) {
					DbAttribute src = join.getSource();
					appendColumn(columns, null, src, attributes, null);
				}
			}
		};

		descriptor.visitAllProperties(visitor);

		// stack should be reset, because all root table attributes go with "t0"
		// table alias
		resetJoinStack();

		// add remaining needed attrs from DbEntity
		DbEntity table = getQueryMetadata().getDbEntity();
		for (DbAttribute dba : table.getPrimaryKeys()) {
			appendColumn(columns, null, dba, attributes, null);
		}

		// special handling of a disjoint query...

		if (query instanceof PrefetchSelectQuery) {

			// for each relationship path add PK of the target entity...
			for (String path : ((PrefetchSelectQuery) query).getResultPaths()) {

				ASTDbPath pathExp = (ASTDbPath) oe.translateToDbPath(ExpressionFactory.exp(path));

				// add joins and find terminating element

				resetJoinStack();

				PathComponent<DbAttribute, DbRelationship> lastComponent = null;
				for (PathComponent<DbAttribute, DbRelationship> component : table
						.resolvePath(pathExp, getPathAliases())) {

					if (component.getRelationship() != null) {
						// do not invoke dbRelationshipAdded(), invoke
						// pushJoin() instead. This is to prevent
						// 'forcingDistinct' flipping to true, that will result
						// in unneeded extra processing and sometimes in invalid
						// results (see CAY-1979). Distinctness of each row is
						// guaranteed by the prefetch query semantics - we
						// include target ID in the result columns
						getJoinStack().pushJoin(component.getRelationship(), component.getJoinType(), null);
					}

					lastComponent = component;
				}

				// process terminating element
				if (lastComponent != null) {

					DbRelationship relationship = lastComponent.getRelationship();

					if (relationship != null) {

						String labelPrefix = pathExp.getPath();
						DbEntity targetEntity = relationship.getTargetEntity();

						for (DbAttribute pk : targetEntity.getPrimaryKeys()) {

							// note that we my select a source attribute, but
							// label it as
							// target for simplified snapshot processing
							appendColumn(columns, null, pk, attributes, labelPrefix + '.' + pk.getName());
						}
					}
				}
			}
		}

		// handle joint prefetches directly attached to this query...
		if (query.getPrefetchTree() != null) {

			for (PrefetchTreeNode prefetch : query.getPrefetchTree().adjacentJointNodes()) {

				// for each prefetch add all joins plus columns from the target
				// entity
				Expression prefetchExp = ExpressionFactory.exp(prefetch.getPath());
				ASTDbPath dbPrefetch = (ASTDbPath) oe.translateToDbPath(prefetchExp);

				resetJoinStack();
				DbRelationship r = null;
				for (PathComponent<DbAttribute, DbRelationship> component : table.resolvePath(dbPrefetch,
						getPathAliases())) {
					r = component.getRelationship();
					dbRelationshipAdded(r, JoinType.LEFT_OUTER, null);
				}

				if (r == null) {
					throw new CayenneRuntimeException("Invalid joint prefetch '" + prefetch + "' for entity: "
							+ oe.getName());
				}

				// add columns from the target entity, including those that are
				// matched
				// against the FK of the source entity. This is needed to
				// determine
				// whether optional relationships are null

				// go via target OE to make sure that Java types are mapped
				// correctly...
				ObjRelationship targetRel = (ObjRelationship) prefetchExp.evaluate(oe);
				ObjEntity targetEntity = targetRel.getTargetEntity();

				String labelPrefix = dbPrefetch.getPath();
				for (ObjAttribute oa : targetEntity.getAttributes()) {
					Iterator<CayenneMapEntry> dbPathIterator = oa.getDbPathIterator();
					while (dbPathIterator.hasNext()) {
						Object pathPart = dbPathIterator.next();

						if (pathPart == null) {
							throw new CayenneRuntimeException("ObjAttribute has no component: " + oa.getName());
						} else if (pathPart instanceof DbRelationship) {
							DbRelationship rel = (DbRelationship) pathPart;
							dbRelationshipAdded(rel, JoinType.INNER, null);
						} else if (pathPart instanceof DbAttribute) {
							DbAttribute attribute = (DbAttribute) pathPart;

							appendColumn(columns, oa, attribute, attributes, labelPrefix + '.' + attribute.getName());
						}
					}
				}

				// append remaining target attributes such as keys
				DbEntity targetDbEntity = r.getTargetEntity();
				for (DbAttribute attribute : targetDbEntity.getAttributes()) {
					appendColumn(columns, null, attribute, attributes, labelPrefix + '.' + attribute.getName());
				}
			}
		}

		return columns;
	}

	<T> List<ColumnDescriptor> appendIdColumns(final List<ColumnDescriptor> columns, SelectQuery<T> query) {

		Set<ColumnTracker> skipSet = new HashSet<ColumnTracker>();

		ClassDescriptor descriptor = queryMetadata.getClassDescriptor();
		ObjEntity oe = descriptor.getEntity();
		DbEntity dbEntity = oe.getDbEntity();
		for (ObjAttribute attribute : oe.getPrimaryKeys()) {

			// synthetic objattributes can't reliably lookup their DbAttribute,
			// so do it manually..
			DbAttribute dbAttribute = dbEntity.getAttribute(attribute.getDbAttributeName());
			appendColumn(columns, attribute, dbAttribute, skipSet, null);
		}

		return columns;
	}

	private void appendColumn(List<ColumnDescriptor> columns, ObjAttribute objAttribute, DbAttribute attribute,
			Set<ColumnTracker> skipSet, String label) {

		String alias = getCurrentAlias();
		if (skipSet.add(new ColumnTracker(alias, attribute))) {

			ColumnDescriptor column = (objAttribute != null) ? new ColumnDescriptor(objAttribute, attribute, alias)
					: new ColumnDescriptor(attribute, alias);

			if (label != null) {
				column.setDataRowKey(label);
			}

			columns.add(column);

			// TODO: andrus, 5/7/2006 - replace 'columns' collection with this map, as it is redundant
			defaultAttributesByColumn.put(column, objAttribute);
		} else if (objAttribute != null) {

			// record ObjAttribute override
			for (ColumnDescriptor column : columns) {
				if (attribute.getName().equals(column.getName())) {

					if (attributeOverrides == null) {
						attributeOverrides = new HashMap<>();
					}

					// kick out the original attribute
					ObjAttribute original = defaultAttributesByColumn.remove(column);

					if (original != null) {
						attributeOverrides.put(original, column);
					}

					attributeOverrides.put(objAttribute, column);
					column.setJavaClass(Void.TYPE.getName());

					break;
				}
			}
		}
	}

	/**
	 * @since 3.0
	 */
	@Override
	public void resetJoinStack() {
		getJoinStack().resetStack();
	}

	/**
	 * @since 3.0
	 */
	@Override
	public void dbRelationshipAdded(DbRelationship relationship, JoinType joinType, String joinSplitAlias) {
		if (relationship.isToMany()) {
			forcingDistinct = true;
		}

		getJoinStack().pushJoin(relationship, joinType, joinSplitAlias);
	}

	/**
	 * Always returns true.
	 */
	@Override
	public boolean supportsTableAliases() {
		return true;
	}

	@Override
	public String getAliasForExpression(Expression exp) {
		Collection<Property<?>> columns = ((SelectQuery<?>)query).getColumns();
		if(columns == null) {
			return null;
		}
		for(Property<?> property : columns) {
			if(property.getExpression().equals(exp)) {
				return property.getAlias();
			}
		}

		return null;
	}

	static final class ColumnTracker {

		private DbAttribute attribute;
		private String alias;

		ColumnTracker(String alias, DbAttribute attribute) {
			this.attribute = attribute;
			this.alias = alias;
		}

		@Override
		public boolean equals(Object object) {
			if (!(object instanceof ColumnTracker)) {
				return false;
			}

			ColumnTracker other = (ColumnTracker) object;
			return new EqualsBuilder().append(alias, other.alias).append(attribute, other.attribute).isEquals();
		}

		@Override
		public int hashCode() {
			return new HashCodeBuilder(31, 5).append(alias).append(attribute).toHashCode();
		}

	}

	static final class AccumulatingBindingListener implements AddBindingListener {
		private List<DbAttributeBinding> bindings = new ArrayList<>();

		@Override
		public void onAdd(DbAttributeBinding binding) {
			bindings.add(binding);
		}

		public void reset() {
			bindings.clear();
		}

		public List<DbAttributeBinding> getBindings() {
			return new ArrayList<>(bindings);
		}
	}
}
