package org.apache.cayenne.exp;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.SortOrder;

/**
 * <p>A property in a DataObject.</p>
 * 
 * <p>Used to construct Expressions quickly and with type-safety,
 * and to construct Orderings</p>
 * 
 * <p>Instances of this class are immutable</p>
 * 
 * @param <E> The type this property returns.
 */
public class Property<E> {

	/**
	 * Name of the property in the object
	 */
	private final String name;

	/**
	 * Constructs a new property with the given name.
	 */
	public Property(String name) {
		this.name = name;
	}

	/**
	 * @return Name of the property in the object.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Constructs a property path by appending the argument to 
	 * 	       the existing property separated by a dot
	 */
	public Property<Object> dot(String property) {
		return new Property<Object>(getName() + "." + property);
	}

	/**
	 * @return Constructs a property path by appending the argument to 
	 * 	       the existing property separated by a dot
	 */
	public <T> Property<T> dot(Property<T> property) {
		return new Property<T>(getName() + "." + property.getName());
	}
	
	/**
	 * @return An expression representing null.
	 */
	public Expression isNull() {
		return ExpressionFactory.matchExp(getName(), null);
	}

	/**
	 * @return An expression representing a non-null value.
	 */
	public Expression isNotNull() {
		return ExpressionFactory.matchExp(getName(), null).notExp();
	}

	/**
	 * @return An expression representing equality to TRUE.
	 */
	public Expression isTrue() {
		return ExpressionFactory.matchExp(getName(), Boolean.TRUE);
	}

	/**
	 * @return An expression representing equality to FALSE.
	 */
	public Expression isFalse() {
		return ExpressionFactory.matchExp(getName(), Boolean.FALSE);
	}
	
	/**
	 * @return An expression representing equality to a value.
	 */
	public Expression eq(E value) {
		return ExpressionFactory.matchExp(getName(), value);
	}

	/**
	 * @return An expression representing inequality to a value.
	 */
	public Expression ne(E value) {
		return ExpressionFactory.noMatchExp(getName(), value);
	}
	
	/**
	 * @return An expression for a Database "Like" query.
	 */
	public Expression like(E value) {
		return ExpressionFactory.likeExp(getName(), value);
	}

	/**
	 * @return An expression for a case insensitive "Like" query.
	 */
	public Expression likeInsensitive(E value) {
		return ExpressionFactory.likeIgnoreCaseExp(getName(), value);
	}

	/**
	 * @return An expression checking for objects between a lower and upper bound inclusive
	 *
	 * @param lower The lower bound.
	 * @param upper The upper bound.
	 */
	public Expression between(E lower, E upper) {
		return ExpressionFactory.betweenExp(getName(), lower, upper);
	}

	/**
	 * @return An expression for finding objects with values in the given set.
	 */
	public Expression in(E... values) {
		return ExpressionFactory.inExp(getName(), values);
	}

	/**
	 * @return A greater than Expression.
	 */
	public Expression gt(E value) {
		return ExpressionFactory.greaterExp(getName(), value);
	}

	/**
	 * @return A greater than or equal to Expression.
	 */
	public Expression gte(E value) {
		return ExpressionFactory.greaterOrEqualExp(getName(), value);
	}

	/**
	 * @return A less than Expression.
	 */
	public Expression lt(E value) {
		return ExpressionFactory.lessExp(getName(), value);
	}

	/**
	 * @return A less than or equal to Expression.
	 */
	public Expression lte(E value) {
		return ExpressionFactory.lessOrEqualExp(getName(), value);
	}

	/**
	 * @return Ascending sort orderings on this property.
	 */
	public Ordering asc() {
		return new Ordering(getName(), SortOrder.ASCENDING);
	}

	/**
	 * @return Ascending sort orderings on this property.
	 */
	public List<Ordering> ascs() {
		List<Ordering> result = new ArrayList<Ordering>(1);
		result.add(asc());
		return result;
	}

	/**
	 * @return Ascending case insensitive sort orderings on this property.
	 */
	public Ordering ascInsensitive() {
		return new Ordering(getName(), SortOrder.ASCENDING_INSENSITIVE);
	}

	/**
	 * @return Ascending case insensitive sort orderings on this property.
	 */
	public List<Ordering> ascInsensitives() {
		List<Ordering> result = new ArrayList<Ordering>(1);
		result.add(ascInsensitive());
		return result;
	}

	/**
	 * @return Descending sort orderings on this property.
	 */
	public Ordering desc() {
		return new Ordering(getName(), SortOrder.DESCENDING);
	}

	/**
	 * @return Descending sort orderings on this property.
	 */
	public List<Ordering> descs() {
		List<Ordering> result = new ArrayList<Ordering>(1);
		result.add(desc());
		return result;
	}

	/**
	 * @return Descending case insensitive sort orderings on this property.
	 */
	public Ordering descInsensitive() {
		return new Ordering(getName(), SortOrder.DESCENDING_INSENSITIVE);
	}

	/**
	 * @return Descending case insensitive sort orderings on this property.
	 */
	public List<Ordering> descInsensitives() {
		List<Ordering> result = new ArrayList<Ordering>(1);
		result.add(descInsensitive());
		return result;
	}

}