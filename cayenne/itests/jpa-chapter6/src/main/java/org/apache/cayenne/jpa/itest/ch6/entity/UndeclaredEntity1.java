package org.apache.cayenne.jpa.itest.ch6.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class UndeclaredEntity1 {

	@Id
	protected int id;
}
