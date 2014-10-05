package org.apache.cayenne.query;

import static java.util.Collections.singletonMap;
import static org.apache.cayenne.exp.ExpressionFactory.matchAllDbExp;

import java.util.Collection;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;

/**
 * A query to select single objects by id.
 * 
 * @since 3.2
 */
public class IdSelect<T> extends IndirectQuery implements Select<T> {

	private static final long serialVersionUID = -6589464349051607583L;

	// type is not same as T, as T maybe be DataRow or scalar
	// either type or entity name is specified, but not both
	private Class<?> entityType;
	private String entityName;

	// only one of the two id forms is provided, but not both
	private Object singleId;
	private Map<String, ?> mapId;

	private boolean fetchingDataRows;
	private QueryCacheStrategy cacheStrategy;
	private String[] cacheGroups;

	public static <T> IdSelect<T> query(Class<T> entityType, Object id) {
		IdSelect<T> q = new IdSelect<T>();

		q.entityType = entityType;
		q.singleId = id;
		q.fetchingDataRows = false;

		return q;
	}

	public static <T> IdSelect<T> query(Class<T> entityType, Map<String, ?> id) {
		IdSelect<T> q = new IdSelect<T>();

		q.entityType = entityType;
		q.mapId = id;
		q.fetchingDataRows = false;

		return q;
	}

	public static <T> IdSelect<T> query(Class<T> entityType, ObjectId id) {
		checkObjectId(id);

		IdSelect<T> q = new IdSelect<T>();

		q.entityName = id.getEntityName();
		q.mapId = id.getIdSnapshot();
		q.fetchingDataRows = false;

		return q;
	}

	public static IdSelect<DataRow> dataRowQuery(Class<?> entityType, Object id) {
		IdSelect<DataRow> q = new IdSelect<DataRow>();

		q.entityType = entityType;
		q.singleId = id;
		q.fetchingDataRows = true;

		return q;
	}

	public static IdSelect<DataRow> dataRowQuery(Class<?> entityType, Map<String, Object> id) {
		IdSelect<DataRow> q = new IdSelect<DataRow>();

		q.entityType = entityType;
		q.mapId = id;
		q.fetchingDataRows = true;

		return q;
	}

	public static IdSelect<DataRow> dataRowQuery(ObjectId id) {
		checkObjectId(id);

		IdSelect<DataRow> q = new IdSelect<DataRow>();

		q.entityName = id.getEntityName();
		q.mapId = id.getIdSnapshot();
		q.fetchingDataRows = true;

		return q;
	}

	private static void checkObjectId(ObjectId id) {
		if (id.isTemporary() && !id.isReplacementIdAttached()) {
			throw new CayenneRuntimeException("Can't build a query for temporary id: " + id);
		}
	}

	/**
	 * Selects a single object using provided context. Essentially the inversion
	 * of "ObjectContext.selectOne(Select)".
	 */
	public T selectOne(ObjectContext context) {
		return context.selectOne(this);
	}

	/**
	 * Instructs Cayenne to look for query results in the "local" cache when
	 * running the query. This is a short-hand notation for:
	 * 
	 * <pre>
	 * query.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
	 * query.setCacheGroups(&quot;group1&quot;, &quot;group2&quot;);
	 * </pre>
	 * 
	 * @since 3.2
	 */
	public IdSelect<T> useLocalCache(String... cacheGroups) {
		cacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
		cacheGroups(cacheGroups);
		return this;
	}

	public IdSelect<T> useSharedCache(String... cacheGroups) {
		return cacheStrategy(QueryCacheStrategy.SHARED_CACHE).cacheGroups(cacheGroups);
	}

	public QueryCacheStrategy getCacheStrategy() {
		return cacheStrategy;
	}

	public IdSelect<T> cacheStrategy(QueryCacheStrategy strategy) {
		if (this.cacheStrategy != strategy) {
			this.cacheStrategy = strategy;
			this.replacementQuery = null;
		}

		return this;
	}

	public String[] getCacheGroups() {
		return cacheGroups;
	}

	public IdSelect<T> cacheGroups(String... cacheGroups) {
		this.cacheGroups = cacheGroups;
		this.replacementQuery = null;
		return this;
	}

	public boolean isFetchingDataRows() {
		return fetchingDataRows;
	}

	@Override
	protected Query createReplacementQuery(EntityResolver resolver) {

		ObjEntity entity = resolveEntity(resolver);
		Map<String, ?> id = resolveId(entity);

		SelectQuery<Object> query = new SelectQuery<Object>();
		query.setRoot(entity);
		query.setFetchingDataRows(fetchingDataRows);
		query.setQualifier(matchAllDbExp(id, Expression.EQUAL_TO));

		// note on caching... this hits query cache instead of object cache...
		// until we merge the two this may result in not using the cache
		// optimally - object cache may have an object, but query cache will not
		query.setCacheGroups(cacheGroups);
		query.setCacheStrategy(cacheStrategy);

		return query;
	}

	protected Map<String, ?> resolveId(ObjEntity entity) {

		if (singleId == null && mapId == null) {
			throw new CayenneRuntimeException("Misconfigured query. Either singleId or mapId must be set");
		}

		if (mapId != null) {
			return mapId;
		}

		Collection<String> pkAttributes = entity.getPrimaryKeyNames();
		if (pkAttributes.size() != 1) {
			throw new CayenneRuntimeException("PK contains " + pkAttributes.size() + " columns, expected 1.");
		}

		String pk = pkAttributes.iterator().next();
		return singletonMap(pk, singleId);
	}

	protected ObjEntity resolveEntity(EntityResolver resolver) {

		if (entityName == null && entityType == null) {
			throw new CayenneRuntimeException("Misconfigured query. Either entityName or entityType must be set");
		}

		return entityName != null ? resolver.getObjEntity(entityName) : resolver.getObjEntity(entityType);
	}
}
