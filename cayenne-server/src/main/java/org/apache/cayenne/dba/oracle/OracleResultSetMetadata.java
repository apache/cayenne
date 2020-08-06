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
package org.apache.cayenne.dba.oracle;

import java.math.BigDecimal;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

class OracleResultSetMetadata implements ResultSetMetaData {

	private ResultSetMetaData delegate;

	OracleResultSetMetadata(ResultSetMetaData delegate) {
		this.delegate = delegate;
	}

	@Override
	public String getCatalogName(int column) throws SQLException {
		return delegate.getCatalogName(column);
	}

	@Override
	public String getColumnClassName(int column) throws SQLException {
		String className = delegate.getColumnClassName(column);

		if (BigDecimal.class.getName().equals(className) && getColumnType(column) == Types.INTEGER) {
			className = Integer.class.getName();
		}

		return className;
	}

	@Override
	public int getColumnCount() throws SQLException {
		return delegate.getColumnCount();
	}

	@Override
	public int getColumnDisplaySize(int column) throws SQLException {
		return delegate.getColumnDisplaySize(column);
	}

	@Override
	public String getColumnLabel(int column) throws SQLException {
		return delegate.getColumnLabel(column);
	}

	@Override
	public String getColumnName(int column) throws SQLException {
		return delegate.getColumnName(column);
	}

	@Override
	public int getColumnType(int column) throws SQLException {
		int type = delegate.getColumnType(column);

		// this only detects INTEGER but not BIGINT...
		if (type == Types.NUMERIC) {
			int precision = delegate.getPrecision(column);
			if ((precision == 10 || precision == 38) && delegate.getScale(column) == 0) {
				type = Types.INTEGER;
			}
		}

		return type;
	}

	@Override
	public String getColumnTypeName(int column) throws SQLException {
		return delegate.getColumnTypeName(column);
	}

	@Override
	public int getPrecision(int column) throws SQLException {
		return delegate.getPrecision(column);
	}

	@Override
	public int getScale(int column) throws SQLException {
		return delegate.getScale(column);
	}

	@Override
	public String getSchemaName(int column) throws SQLException {
		return delegate.getSchemaName(column);
	}

	@Override
	public String getTableName(int column) throws SQLException {
		return delegate.getTableName(column);
	}

	@Override
	public boolean isAutoIncrement(int column) throws SQLException {
		return delegate.isAutoIncrement(column);
	}

	@Override
	public boolean isCaseSensitive(int column) throws SQLException {
		return delegate.isCaseSensitive(column);
	}

	@Override
	public boolean isCurrency(int column) throws SQLException {
		return delegate.isCurrency(column);
	}

	@Override
	public boolean isDefinitelyWritable(int column) throws SQLException {
		return delegate.isDefinitelyWritable(column);
	}

	@Override
	public int isNullable(int column) throws SQLException {
		return delegate.isNullable(column);
	}

	@Override
	public boolean isReadOnly(int column) throws SQLException {
		return delegate.isReadOnly(column);
	}

	@Override
	public boolean isSearchable(int column) throws SQLException {
		return delegate.isSearchable(column);
	}

	@Override
	public boolean isSigned(int column) throws SQLException {
		return delegate.isSigned(column);
	}

	@Override
	public boolean isWritable(int column) throws SQLException {
		return delegate.isWritable(column);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		// TODO
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		// TODO
		throw new UnsupportedOperationException();
	}
}