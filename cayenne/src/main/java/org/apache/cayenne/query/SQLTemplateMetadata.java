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
package org.apache.cayenne.query;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.SQLResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @since 3.0
 */
public class SQLTemplateMetadata extends BaseQueryMetadata {

	private boolean isSingleResultSetMapping;
	private Function<?, ?> resultMapper;

	@Override
	public boolean isSingleResultSetMapping() {
		return isSingleResultSetMapping;
	}

	boolean resolve(Object root, EntityResolver resolver, SQLTemplate query) {

		if (super.resolve(root, resolver)) {

			if((!query.isUseScalar() && !query.isFetchingDataRows()) && (query.getResultColumnsTypes() != null && !query.getResultColumnsTypes().isEmpty())) {
				throw new CayenneRuntimeException("Error caused by using root in query with resultColumnTypes without scalar or dataRow.");
			}

			if(query.getResult() != null && query.getResultColumnsTypes() != null) {
				throw new CayenneRuntimeException("Caused by trying to override result column types of query.");
			}

			if(query.isFetchingDataRows() && query.isUseScalar()) {
				throw new CayenneRuntimeException("Can't set both use scalar and fetching data rows.");
			}

			buildResultSetMappingForColumns(query);
			resultSetMapping = query.getResult() != null ?
					query.getResult().getResolvedComponents(resolver) :
					query.isUseScalar() ? new ArrayList<>() : null;
			isSingleResultSetMapping = resultSetMapping != null && resultSetMapping.size() == 1;

			// generate unique cache key...
			if (QueryCacheStrategy.NO_CACHE == getCacheStrategy()) {

			} else {

				// create a unique key based on entity, SQL, and parameters

				StringBuilder key = new StringBuilder();
				ObjEntity entity = getObjEntity();
				if (entity != null) {
					key.append(entity.getName());
				} else if (dbEntity != null) {
					key.append("db:").append(dbEntity.getName());
				}

				if (query.getDefaultTemplate() != null) {
					key.append('/').append(query.getDefaultTemplate());
				}

				Map<String, ?> parameters = query.getParams();
				if (!parameters.isEmpty()) {

					List<String> keys = new ArrayList<>(parameters.keySet());
					Collections.sort(keys);

					for (String parameterKey : keys) {
						key.append('/').append(parameterKey).append('=').append(parameters.get(parameterKey));
					}
				}

				for (Object parameter : query.getPositionalParams()) {
					key.append("/p:").append(parameter);
				}

				if (query.getFetchOffset() > 0 || query.getFetchLimit() > 0) {
					key.append('/');
					if (query.getFetchOffset() > 0) {
						key.append('o').append(query.getFetchOffset());
					}
					if (query.getFetchLimit() > 0) {
						key.append('l').append(query.getFetchLimit());
					}
				}

				this.cacheKey = key.toString();
			}

			return true;
		}

		return false;
	}

	private void buildResultSetMappingForColumns(SQLTemplate query) {
		if(query.getResultColumnsTypes() == null || query.getResultColumnsTypes().isEmpty() || !query.isUseScalar()) {
			return;
		}
		SQLResult result = new SQLResult();
		for(int i = 0; i < query.getResultColumnsTypes().size(); i++) {
			result.addColumnResult(String.valueOf(i));
		}
		query.setResult(result);
	}

	void setResultMapper(Function<?,?> resultMapper) {
		if(this.resultMapper != null) {
			this.resultMapper = this.resultMapper.andThen((Function)resultMapper);
		} else {
			this.resultMapper = resultMapper;
		}
    }

	@Override
	public Function<?, ?> getResultMapper() {
		return resultMapper;
	}
}
