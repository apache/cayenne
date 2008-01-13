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

package org.apache.cayenne.map.event;

import org.apache.cayenne.access.DataDomain;

/** 
 * Represents events resulted from DataDomain changes 
 * in CayenneModeler.
 * 
 * @author Misha Shengaout
 * @author Andrus Adamchik
 */
public class DomainEvent extends MapEvent {
	protected DataDomain domain;

	/** Creates a domain change event. */
	public DomainEvent(Object src, DataDomain domain) {
		super(src);
		this.domain = domain;
	}

	/** Creates a domain event of a specified type. */
	public DomainEvent(Object src, DataDomain domain, int id) {
		this(src, domain);
		setId(id);
	}

	/** Creates a domain name change event.*/
	public DomainEvent(Object src, DataDomain domain, String oldName) {
		this(src, domain);	
		setOldName(oldName);
	}

	/** Returns domain object associated with this event. */
	public DataDomain getDomain() {
		return domain;
	}

	/**
	 * Sets domain object associated with this event.
	 * 
	 * @param domain The domain to set
	 */
	public void setDomain(DataDomain domain) {
		this.domain = domain;
	}
	
	@Override
    public String getNewName() {
		return (domain != null) ? domain.getName() : null;
	}
}
