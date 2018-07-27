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
import org.apache.cayenne.Persistent;
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
import org.apache.cayenne.query.PrefetchProcessor;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger logger = LoggerFactory.getLogger(SelectTranslator.class);

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

	/**
	 * Callback for joins creation
	 */
	AddJoinListener joinListener;

	JointPrefetchChecker jointPrefetchChecker = new JointPrefetchChecker();


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
		return new JoinStack(getAdapter(), this);
	}

	@Override
	protected void doTranslate() {

		checkLimitAndJointPrefetch();

		DataMap dataMap = queryMetadata.getDataMap();
		JoinStack joins = getJoinStack();

		QuotingStrategy strategy = getAdapter().getQuotingStrategy();
		forcingDistinct = false;

		// build column list
		this.resultColumns = buildResultColumns();

		// build qualifier
		QualifierTranslator qualifierTranslator = adapter.getQualifierTranslator(this);
		StringBuilder whereQualifierBuffer = qualifierTranslator.appendPart(new StringBuilder());

		// build having qualifier
		Expression havingQualifier = getSelectQuery().getHavingQualifier();
		StringBuilder havingQualifierBuffer = null;
		if(havingQualifier != null) {
			haveAggregate = true;
			QualifierTranslator havingQualifierTranslator = adapter.getQualifierTranslator(this);
			havingQualifierTranslator.setQualifier(havingQualifier);
			havingQualifierBuffer = havingQualifierTranslator.appendPart(new StringBuilder());
		}

		if(!haveAggregate && groupByColumns != null) {
			// if no expression with aggregation function found
			// in select columns and there is no having clause
			groupByColumns.clear();
		}

		// build ORDER BY
		OrderingTranslator orderingTranslator = new OrderingTranslator(this);
		StringBuilder orderingBuffer = orderingTranslator.appendPart(new StringBuilder());

		// assemble
		StringBuilder queryBuf = new StringBuilder();
		queryBuf.append("SELECT ");

		// check if DISTINCT is appropriate
		// side effect: "suppressingDistinct" flag may end up being flipped here
		if (forcingDistinct || getSelectQuery().isDistinct()) {
			suppressingDistinct = queryMetadata.isSuppressingDistinct();
			if(!suppressingDistinct) {
				for (ColumnDescriptor column : resultColumns) {
					if (isUnsupportedForDistinct(column.getJdbcType())) {
						suppressingDistinct = true;
						break;
					}
				}
			}

			if (!suppressingDistinct) {
				queryBuf.append(buildDistinctStatement()).append(" ");
			}
		}

		// convert ColumnDescriptors to column names
		List<String> selectColumnExpList = new ArrayList<>();
		for (ColumnDescriptor column : resultColumns) {
			String fullName;
			if(column.isExpression()) {
				fullName = column.getName();
			} else {
				fullName = strategy.quotedIdentifier(dataMap, column.getNamePrefix(), column.getName());
			}
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

		if(groupByColumns != null && !groupByColumns.isEmpty()) {
			queryBuf.append(" GROUP BY ");
			appendGroupByColumns(queryBuf, groupByColumns);
		}

		// append HAVING qualifier
		if(havingQualifierBuffer != null && havingQualifierBuffer.length() > 0) {
			queryBuf.append(" HAVING ");
			queryBuf.append(havingQualifierBuffer);
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
	 * Warn user in case query uses both limit and joint prefetch, as we don't support this combination.
	 */
	private void checkLimitAndJointPrefetch() {
		if(queryMetadata.getPrefetchTree() == null) {
			return;
		}

		if(queryMetadata.getFetchLimit() == 0 && queryMetadata.getFetchOffset() == 0) {
			return;
		}

		if(!jointPrefetchChecker.haveJointNode(queryMetadata.getPrefetchTree())) {
			return;
		}

		logger.warn("Query uses both limit and joint prefetch, this most probably will lead to incorrect result. " +
				"Either use disjointById prefetch or get full result set.");
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
		String fullName;
		if(entry.getKey().isExpression()) {
			fullName = entry.getKey().getDataRowKey();
		} else {
			QuotingStrategy strategy = getAdapter().getQuotingStrategy();
			fullName = strategy.quotedIdentifier(queryMetadata.getDataMap(),
					entry.getKey().getNamePrefix(), entry.getKey().getName());
		}

		buffer.append(fullName);

		if(entry.getKey().getDataRowKey().equals(entry.getKey().getName())) {
			for (DbAttributeBinding binding : entry.getValue()) {
				addToParamList(binding.getAttribute(), binding.getValue());
			}
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
			appendOverriddenColumns(columns, query);
		} else if (query.getRoot() instanceof DbEntity) {
			appendDbEntityColumns(columns, query);
		} else if (getQueryMetadata().getPageSize() > 0) {
			appendIdColumns(columns, queryMetadata.getClassDescriptor().getEntity());
		} else {
			appendQueryColumns(columns, query, queryMetadata.getClassDescriptor(), null);
		}

		return columns;
	}

	/**
	 * If query contains explicit column list, use only them
	 */
	<T> List<ColumnDescriptor> appendOverriddenColumns(List<ColumnDescriptor> columns, SelectQuery<T> query) {
		groupByColumns = new HashMap<>();

		QualifierTranslator qualifierTranslator = adapter.getQualifierTranslator(this);
		AccumulatingBindingListener bindingListener = new AccumulatingBindingListener();
		final String[] joinTableAliasForProperty = {null};
		// capture last alias for joined table, will use it to resolve object columns
		joinListener = () -> joinTableAliasForProperty[0] = getCurrentAlias();
		setAddBindingListener(bindingListener);

		for(Property<?> property : query.getColumns()) {
			joinTableAliasForProperty[0] = null;
			int expressionType = property.getExpression().getType();

			// forbid direct selection of toMany relationships columns
			if(property.getType() != null && (expressionType == Expression.OBJ_PATH || expressionType == Expression.DB_PATH)
					&& (Collection.class.isAssignableFrom(property.getType())
							|| Map.class.isAssignableFrom(property.getType()))) {
				throw new CayenneRuntimeException("Can't directly select toMany relationship columns. " +
						"Either select it with aggregate functions like count() or with flat() function to select full related objects.");
			}

			// evaluate ObjPath with Persistent type as toOne relations and use it as full object
			boolean objectProperty = expressionType == Expression.FULL_OBJECT
					|| (property.getType() != null
						&& expressionType == Expression.OBJ_PATH
						&& Persistent.class.isAssignableFrom(property.getType()));

			// Qualifier Translator in case of Object Columns have side effect -
			// it will create required joins, that we catch with listener above.
			// And we force created join alias for all columns of Object we select.
			qualifierTranslator.setQualifier(property.getExpression());
			qualifierTranslator.setForceJoinForRelations(objectProperty);
			StringBuilder builder = qualifierTranslator.appendPart(new StringBuilder());

			if(objectProperty) {
				// If we want full object, use appendQueryColumns method, to fully process class descriptor
				List<ColumnDescriptor> classColumns = new ArrayList<>();
				ObjEntity entity = entityResolver.getObjEntity(property.getType());
				if(getQueryMetadata().getPageSize() > 0) {
					appendIdColumns(classColumns, entity);
				} else {
					ClassDescriptor classDescriptor = entityResolver.getClassDescriptor(entity.getName());
					appendQueryColumns(classColumns, query, classDescriptor, joinTableAliasForProperty[0]);
				}
				for(ColumnDescriptor descriptor : classColumns) {
					columns.add(descriptor);
					groupByColumns.put(descriptor, Collections.<DbAttributeBinding>emptyList());
				}
			} else {
				// This property will go as scalar value
				String alias = property.getAlias();
				if (alias != null) {
					builder.append(" AS ").append(alias);
				}

				int type = getJdbcTypeForProperty(property);
				ColumnDescriptor descriptor;
				if(property.getType() != null) {
					descriptor = new ColumnDescriptor(builder.toString(), type, property.getType().getCanonicalName());
				} else {
					descriptor = new ColumnDescriptor(builder.toString(), type);
				}
				descriptor.setDataRowKey(alias);
				descriptor.setIsExpression(true);
				columns.add(descriptor);

				if (isAggregate(property)) {
					haveAggregate = true;
				} else {
					groupByColumns.put(descriptor, bindingListener.getBindings());
				}
				bindingListener.reset();
			}
		}

		setAddBindingListener(null);
		qualifierTranslator.setForceJoinForRelations(false);
		joinListener = null;

		return columns;
	}

	private int getJdbcTypeForProperty(Property<?> property) {
		int expressionType = property.getExpression().getType();
		if(expressionType == Expression.OBJ_PATH) {
			// Scan obj path, stop as soon as DbAttribute found
			for (PathComponent<ObjAttribute, ObjRelationship> component :
					getQueryMetadata().getObjEntity().resolvePath(property.getExpression(), getPathAliases())) {
				if(component.getAttribute() != null) {
					Iterator<CayenneMapEntry> dbPathIterator = component.getAttribute().getDbPathIterator();
					while (dbPathIterator.hasNext()) {
						Object pathPart = dbPathIterator.next();
						if (pathPart instanceof DbAttribute) {
							return ((DbAttribute) pathPart).getType();
						}
					}
				}
			}
		} else if(expressionType == Expression.DB_PATH) {
			// Scan db path, stop as soon as DbAttribute found
			for (PathComponent<DbAttribute, DbRelationship> component :
					getQueryMetadata().getDbEntity().resolvePath(property.getExpression(), getPathAliases())) {
				if(component.getAttribute() != null) {
					return component.getAttribute().getType();
				}
			}
		}
		// NOTE: If no attribute found or expression have some other type
	 	// return JDBC type based on Java type of the property.
		// This can lead to incorrect behavior in case we deal with some custom type
		// backed by ExtendedType with logic based on correct JDBC type provided (luckily not very common case).
		// In general we can't map any meaningful type, as we don't know outcome of the expression in the property.
		// If this ever become a problem correct type can be provided at the query call time, using meta data.
		return TypesMapping.getSqlTypeByJava(property.getType());
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
	<T> List<ColumnDescriptor> appendQueryColumns(final List<ColumnDescriptor> columns, SelectQuery<T> query, ClassDescriptor descriptor, final String tableAlias) {

		final Set<ColumnTracker> attributes = new HashSet<>();

		// fetched attributes include attributes that are either:
		//
		// * class properties
		// * PK
		// * FK used in relationship
		// * joined prefetch PK
		ObjEntity oe = descriptor.getEntity();

		PropertyVisitor visitor = new PropertyVisitor() {

			public boolean visitAttribute(AttributeProperty property) {
				ObjAttribute oa = property.getAttribute();

				resetJoinStack();
				Iterator<CayenneMapEntry> dbPathIterator = oa.getDbPathIterator();
				while (dbPathIterator.hasNext()) {
					Object pathPart = dbPathIterator.next();

					if (pathPart == null) {
						throw new CayenneRuntimeException("ObjAttribute has no component: %s", oa.getName());
					} else if (pathPart instanceof DbRelationship) {
						DbRelationship rel = (DbRelationship) pathPart;
						dbRelationshipAdded(rel, JoinType.LEFT_OUTER, null);
					} else if (pathPart instanceof DbAttribute) {
						DbAttribute dbAttr = (DbAttribute) pathPart;

						appendColumn(columns, oa, dbAttr, attributes, null, tableAlias);
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

				// add PKs for flattened tables in flattened path
				ObjRelationship rel = property.getRelationship();
				for(int i=0; i<rel.getDbRelationships().size() - 1; i++) {
					DbRelationship dbRel = rel.getDbRelationships().get(i);
					dbRelationshipAdded(dbRel, JoinType.LEFT_OUTER, null);

					// append path PK attributes
					for(DbAttribute dba : dbRel.getTargetEntity().getPrimaryKeys()) {
						appendColumn(columns, null, dba, attributes, dbRel.getName() + '.' + dba.getName());
					}
				}

				return true;
			}

			private void visitRelationship(ArcProperty property) {
				resetJoinStack();

				ObjRelationship rel = property.getRelationship();
				DbRelationship dbRel = rel.getDbRelationships().get(0);

				List<DbJoin> joins = dbRel.getJoins();
				for (DbJoin join : joins) {
					DbAttribute src = join.getSource();
					appendColumn(columns, null, src, attributes, null, tableAlias);
				}
			}
		};

		descriptor.visitAllProperties(visitor);

		// stack should be reset, because all root table attributes go with "t0"
		// table alias
		resetJoinStack();

		// add remaining needed attrs from DbEntity
		DbEntity table = oe.getDbEntity();
		for (DbAttribute dba : table.getPrimaryKeys()) {
			appendColumn(columns, null, dba, attributes, null, tableAlias);
		}

		// special handling of a disjoint query...

		if (query instanceof PrefetchSelectQuery) {

			// for each relationship path add PK of the target entity...
			for (String path : ((PrefetchSelectQuery<?>) query).getResultPaths()) {

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
			// Set entity name, in case MixedConversionStrategy will be used to select objects from this query
			// Note: all prefetch nodes will point to query root, it is not a problem until select query can't
			// perform some sort of union or sub-queries.
			for(PrefetchTreeNode prefetch : query.getPrefetchTree().getChildren()) {
				prefetch.setEntityName(oe.getName());
			}

			for (PrefetchTreeNode prefetch : query.getPrefetchTree().adjacentJointNodes()) {

				// for each prefetch add all joins plus columns from the target
				// entity
				Expression prefetchExp = ExpressionFactory.exp(prefetch.getPath());
				ASTDbPath dbPrefetch = (ASTDbPath) oe.translateToDbPath(prefetchExp);

				resetJoinStack();
				DbRelationship r = null;
				for (PathComponent<DbAttribute, DbRelationship> component :
						table.resolvePath(dbPrefetch, getPathAliases())) {
					r = component.getRelationship();
					dbRelationshipAdded(r, JoinType.LEFT_OUTER, null);
				}

				if (r == null) {
					throw new CayenneRuntimeException("Invalid joint prefetch '%s' for entity: %s"
							, prefetch, oe.getName());
				}

				// add columns from the target entity, including those that are matched
				// against the FK of the source entity.
				// This is needed to determine whether optional relationships are null

				// go via target OE to make sure that Java types are mapped correctly...
				ObjRelationship targetRel = (ObjRelationship) prefetchExp.evaluate(oe);
				ObjEntity targetEntity = targetRel.getTargetEntity();

				String labelPrefix = dbPrefetch.getPath();

				PropertyVisitor prefetchVisitor = new PropertyVisitor() {
					public boolean visitAttribute(AttributeProperty property) {
						ObjAttribute oa = property.getAttribute();
						Iterator<CayenneMapEntry> dbPathIterator = oa.getDbPathIterator();
						while (dbPathIterator.hasNext()) {
							Object pathPart = dbPathIterator.next();

							if (pathPart == null) {
								throw new CayenneRuntimeException("ObjAttribute has no component: %s", oa.getName());
							} else if (pathPart instanceof DbRelationship) {
								DbRelationship rel = (DbRelationship) pathPart;
								dbRelationshipAdded(rel, JoinType.INNER, null);
							} else if (pathPart instanceof DbAttribute) {
								DbAttribute dbAttr = (DbAttribute) pathPart;

								appendColumn(columns, oa, dbAttr, attributes, labelPrefix + '.' + dbAttr.getName());
							}
						}
						return true;
					}

					public boolean visitToMany(ToManyProperty property) {
						return true;
					}

					public boolean visitToOne(ToOneProperty property) {
						return true;
					}
				};

				ClassDescriptor prefetchClassDescriptor = entityResolver.getClassDescriptor(targetEntity.getName());
				prefetchClassDescriptor.visitAllProperties(prefetchVisitor);

				// append remaining target attributes such as keys
				DbEntity targetDbEntity = r.getTargetEntity();
				for (DbAttribute attribute : targetDbEntity.getAttributes()) {
					appendColumn(columns, null, attribute, attributes, labelPrefix + '.' + attribute.getName());
				}
			}
		}

		return columns;
	}

	<T> List<ColumnDescriptor> appendIdColumns(final List<ColumnDescriptor> columns, ObjEntity objEntity) {

		Set<ColumnTracker> skipSet = new HashSet<>();

		DbEntity dbEntity = objEntity.getDbEntity();
		for (ObjAttribute attribute : objEntity.getPrimaryKeys()) {

			// synthetic objattributes can't reliably lookup their DbAttribute,
			// so do it manually..
			DbAttribute dbAttribute = dbEntity.getAttribute(attribute.getDbAttributeName());
			appendColumn(columns, attribute, dbAttribute, skipSet, null);
		}

		return columns;
	}

	private void appendColumn(List<ColumnDescriptor> columns, ObjAttribute objAttribute, DbAttribute attribute,
							  Set<ColumnTracker> skipSet, String label) {
		appendColumn(columns, objAttribute, attribute, skipSet, label, null);
	}

	private void appendColumn(List<ColumnDescriptor> columns, ObjAttribute objAttribute, DbAttribute attribute,
			Set<ColumnTracker> skipSet, String label, String alias) {

		if(alias == null) {
			alias = getCurrentAlias();
		}

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
		if(joinListener != null) {
			joinListener.joinAdded();
		}
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
		Collection<Property<?>> columns = getSelectQuery().getColumns();
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

	/**
	 * @since 4.0
	 */
	@Override
	public boolean hasJoins() {
		return joinStack != null && joinStack.size() > 0;
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

	interface AddJoinListener {
		void joinAdded();
	}

	private static class JointPrefetchChecker implements PrefetchProcessor {
		private boolean haveJointNode;

		public JointPrefetchChecker() {
		}

		public boolean haveJointNode(PrefetchTreeNode prefetchTree) {
			haveJointNode = false;
			prefetchTree.traverse(this);
			return haveJointNode;
		}

		@Override
        public boolean startPhantomPrefetch(PrefetchTreeNode node) {
            return true;
        }

		@Override
        public boolean startDisjointPrefetch(PrefetchTreeNode node) {
            return true;
        }

		@Override
        public boolean startDisjointByIdPrefetch(PrefetchTreeNode prefetchTreeNode) {
            return true;
        }

		@Override
        public boolean startJointPrefetch(PrefetchTreeNode node) {
            haveJointNode = true;
            return false;
        }

		@Override
        public boolean startUnknownPrefetch(PrefetchTreeNode node) {
            return true;
        }

		@Override
        public void finishPrefetch(PrefetchTreeNode node) {
        }
	}
}
