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

package org.apache.cayenne.access.translator.select;

import java.util.Map;

import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.translator.DbAttributeBinding;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.query.ObjectSelect;

/**
 * An abstraction of {@link ObjectSelect} translator.
 * 
 * @since 4.0 this is an interface.
 */
public interface SelectTranslator {

	String getSql() throws Exception;

	DbAttributeBinding[] getBindings();

	Map<ObjAttribute, ColumnDescriptor> getAttributeOverrides();

	ColumnDescriptor[] getResultColumns();

	boolean isSuppressingDistinct();

	/**
	 * @since 4.0
	 * @return do query has at least one join
	 */
	boolean hasJoins();
}
