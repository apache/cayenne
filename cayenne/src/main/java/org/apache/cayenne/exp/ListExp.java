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

package org.apache.cayenne.exp;

import org.apache.cayenne.Persistent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A leaf expression representing an immutable collection of values.
 * 
 * @since 5.0
 */
public final class ListExp extends Expression {
	protected Object[] values;

	/**
	 * Creates a list of the given elements. A single Collection, array or Iterator argument is taken as the elements
	 * themselves.
	 */
	public ListExp(Object... elements) {
		if (elements != null && elements.length == 1 && (elements[0] instanceof Collection<?>
			    || elements[0] instanceof Object[] || elements[0] instanceof Iterator<?>)) {
			setValues(elements[0]);
		} else {
			setValues(elements);
		}
	}

	/**
	 * Creates a copy of this expression node, without copying children.
	 */
	@Override
	public Expression shallowCopy() {
		return new ListExp();
	}

	@Override
	protected Object evaluateNode(Object o) throws Exception {
		return values;
	}

	@Override
	protected String getExpressionOperator(int index) {
		return ",";
	}

	@Override
	public void appendAsString(Appendable out) throws IOException {

		out.append('(');

		if ((values != null) && (values.length > 0)) {
			for (int i = 0; i < values.length; ++i) {
				if (i > 0) {
					out.append(getExpressionOperator(i));
					out.append(' ');
				}

				if (values[i] instanceof Expression) {
					((Expression) values[i]).appendAsString(out);
				} else {
					ExpHelper.appendScalarAsString(out, values[i], '\"');
				}
			}
		}

		out.append(')');
	}

	@Override
	public int getOperandCount() {
		return 1;
	}

	@Override
	public Object getOperand(int index) {
		if (index == 0) {
			return values;
		}

		throw new ArrayIndexOutOfBoundsException(index);
	}

	@Override
	public void setOperand(int index, Object value) {
		if (index != 0) {
			throw new ArrayIndexOutOfBoundsException(index);
		}

		setValues(value);
	}

	/**
	 * Sets an internal collection of values. Value argument can be an Object[],
	 * a Collection or an iterator.
	 */
	protected void setValues(Object value) {
		if (value == null) {
			this.values = null;
		} else if (value instanceof Object[]) {
			int size = ((Object[]) value).length;
			this.values = new Object[size];
			System.arraycopy((Object[]) value, 0, this.values, 0, size);
		} else if (value instanceof Collection) {
			Collection<?> c = (Collection<?>) value;
			this.values = c.toArray(new Object[0]);
		} else if (value instanceof Iterator) {
			List<Object> values = new ArrayList<>();
			Iterator<?> it = (Iterator<?>) value;
			while (it.hasNext()) {
				values.add(it.next());
			}

			this.values = values.toArray();
		} else {
			throw new IllegalArgumentException("Invalid value class '" + value.getClass().getName()
					+ "', expected null, Object[], Collection, Iterator");
		}
		convertValues();
	}

	private void convertValues() {
		if(values == null) {
			return;
		}
		for (int i = 0; i < values.length; i++) {
			if (values[i] instanceof Persistent persistent) {
				values[i] = persistent.getObjectId();
			}
		}
	}

	/**
	 * Sets the elements of this list. This is how the parser passes the parsed elements.
	 */
	@Override
	public void setOperands(Object... elements) {
		setValues(elements);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(values);
	}

	@Override
	protected boolean parenthesizeAsOperand() {
		// prints its own parentheses
		return false;
	}
}
