package org.apache.cayenne.jpa.itest.ch4.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class SimpleEntity {

	@Id
	protected int id;

	protected String property1;

	public int idField() {
		return id;
	}

	public void updateIdField(int id) {
		this.id = id;
	}

	public String getProperty1() {
		return property1;
	}

	public void setProperty1(String property1) {
		this.property1 = property1;
	}
}
