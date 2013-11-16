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

package org.apache.cayenne.reflect;

import java.sql.Timestamp;
import java.util.Date;

import org.apache.cayenne.util.ToStringBuilder;

public class TestJavaBean extends Object {

    protected String stringField;
    protected byte byteField;
    protected short shortField;
    protected int intField;
    protected long longField;
    protected float floatField;
    protected double doubleField;
    protected boolean booleanField;
    protected Integer integerField;
    protected Number numberField;
    protected byte[] byteArrayField;
    protected Object objectField;
    protected TestJavaBean related;
    protected Date dateField;
    protected Timestamp timestampField;
    protected StringBuilder stringBuilderField;
    
    public byte[] getByteArrayField() {
        return byteArrayField;
    }

    public void setByteArrayField(byte[] byteArrayField) {
        this.byteArrayField = byteArrayField;
    }

    public int getIntField() {
        return intField;
    }

    public void setIntField(int intField) {
        this.intField = intField;
    }

    public String getStringField() {
        return stringField;
    }

    public void setStringField(String stringField) {
        this.stringField = stringField;
    }

    public Integer getIntegerField() {
        return integerField;
    }

    public void setIntegerField(Integer integerField) {
        this.integerField = integerField;
    }

    public Number getNumberField() {
        return numberField;
    }

    public void setNumberField(Number numberField) {
        this.numberField = numberField;
    }

    public Object getObjectField() {
        return objectField;
    }

    public void setObjectField(Object objectField) {
        this.objectField = objectField;
    }

    
    public boolean isBooleanField() {
        return booleanField;
    }

    
    public void setBooleanField(boolean booleanField) {
        this.booleanField = booleanField;
    }

    
    public TestJavaBean getRelated() {
        return related;
    }

    
    public void setRelated(TestJavaBean related) {
        this.related = related;
    }

	public Date getDateField() {
		return dateField;
	}

	public void setDateField(Date dateField) {
		this.dateField = dateField;
	}

	public StringBuilder getStringBuilderField() {
		return stringBuilderField;
	}

	public void setStringBuilderField(StringBuilder stringBuilderField) {
		this.stringBuilderField = stringBuilderField;
	}

	public Timestamp getTimestampField() {
		return timestampField;
	}

	public void setTimestampField(Timestamp timestampField) {
		this.timestampField = timestampField;
	}

	public long getLongField() {
		return longField;
	}

	public void setLongField(long longField) {
		this.longField = longField;
	}

	public byte getByteField() {
		return byteField;
	}

	public void setByteField(byte byteField) {
		this.byteField = byteField;
	}

	public short getShortField() {
		return shortField;
	}

	public void setShortField(short shortField) {
		this.shortField = shortField;
	}

	public float getFloatField() {
		return floatField;
	}

	public void setFloatField(float floatField) {
		this.floatField = floatField;
	}

	public double getDoubleField() {
		return doubleField;
	}

	public void setDoubleField(double doubleField) {
		this.doubleField = doubleField;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this)
			.append("intField", getIntField())
			.append("objectField", getObjectField())
			.toString();
	}
}
