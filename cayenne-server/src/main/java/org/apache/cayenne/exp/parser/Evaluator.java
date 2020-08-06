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
package org.apache.cayenne.exp.parser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.util.ConversionUtil;

/**
 * Performs argument conversions for a calling binary expression, so that the
 * expression could eval the arguments of the same type.
 * 
 * @since 4.0
 */
abstract class Evaluator {

    private static final ConcurrentMap<Class<?>, Evaluator> evaluators;
    private static final Evaluator NULL_LHS_EVALUATOR;
    private static final Evaluator DEFAULT_EVALUATOR;
    private static final Evaluator PERSISTENT_EVALUATOR;
    private static final Evaluator BIG_DECIMAL_EVALUATOR;
    private static final Evaluator NUMBER_EVALUATOR;
    private static final Evaluator COMPARABLE_EVALUATOR;
	
    private static final double EPSILON = 0.0000001;

    /**
     * A decorator of an evaluator that presumes non-null 'lhs' argument and
     * allows for null 'rhs'.
     */
    static class NonNullLhsEvaluator extends Evaluator {
        final Evaluator delegate;

        NonNullLhsEvaluator(Evaluator delegate) {
            this.delegate = delegate;
        }

        @Override
        Integer compare(Object lhs, Object rhs) {
            if (rhs == null) {
                return null;
            }
            return delegate.compare(lhs, rhs);
        }

        @Override
        boolean eq(Object lhs, Object rhs) {
            return rhs != null && delegate.eq(lhs, rhs);
        }
    }

    static {
        evaluators = new ConcurrentHashMap<>();

        NULL_LHS_EVALUATOR = new Evaluator() {
            @Override
            Integer compare(Object lhs, Object rhs) {
                return null;
            }

            @Override
            boolean eq(Object lhs, Object rhs) {
                return rhs == null;
            }
        };

        DEFAULT_EVALUATOR = new NonNullLhsEvaluator(new Evaluator() {
            @Override
            boolean eq(Object lhs, Object rhs) {
                return lhs.equals(rhs);
            }

            @Override
            Integer compare(Object lhs, Object rhs) {
                return null;
            }
        });

        PERSISTENT_EVALUATOR = new NonNullLhsEvaluator(new Evaluator() {

            @Override
            Integer compare(Object lhs, Object rhs) {
                return null;
            }

            @Override
            boolean eq(Object lhs, Object rhs) {

                Persistent lhsPersistent = (Persistent) lhs;

                if (rhs instanceof Persistent) {
                    return lhsPersistent.getObjectId().equals(((Persistent) rhs).getObjectId());
                }

                if (rhs instanceof ObjectId) {
                    return lhsPersistent.getObjectId().equals(rhs);
                }

                if (rhs instanceof Map) {
                    return lhsPersistent.getObjectId().getIdSnapshot().equals(rhs);
                }

                if (lhsPersistent.getObjectId().getIdSnapshot().size() != 1) {
                    // the only options left below are for the single key IDs
                    return false;
                }

                if (rhs instanceof Number) {

                    // only care about whole numbers
                    if (rhs instanceof Integer) {
                        return Cayenne.longPKForObject(lhsPersistent) == ((Number) rhs).longValue();
                    }

                    if (rhs instanceof Long) {
                        return Cayenne.longPKForObject(lhsPersistent) == ((Number) rhs).longValue();
                    }
                }

                return Cayenne.pkForObject(lhsPersistent).equals(rhs);
            }
        });

        BIG_DECIMAL_EVALUATOR = new NonNullLhsEvaluator(new Evaluator() {

            @Override
            Integer compare(Object lhs, Object rhs) {
                return ((BigDecimal) lhs).compareTo(ConversionUtil.toBigDecimal(rhs));
            }

            @Override
            boolean eq(Object lhs, Object rhs) {
                // BigDecimals must be compared using compareTo (see CAY-280 and BigDecimal.equals JavaDoc)
                Integer c = compare(lhs, rhs);
                return c == 0;
            }
        });

        NUMBER_EVALUATOR = new NonNullLhsEvaluator(new Evaluator() {

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            Integer compare(Object lhs, Object rhs) {
                return compareNumbers((Number)lhs, rhs);
            }

            @Override
            boolean eq(Object lhs, Object rhs) {
                return equalNumbers((Number)lhs, rhs);
            }
            
            private final List WHOLE_NUMBER_CLASSES = Arrays.asList(Byte.class, Short.class, Integer.class, AtomicInteger.class, Long.class, AtomicLong.class, BigInteger.class);

            /**
             * Returns the widest primitive wrapper class given the two operands, used in preparation for 
             * comparing two boxed numbers of different types, like java.lang.Short and java.lang.Integer.
             */
            Class<?> widestNumberType(Number lhs, Number rhs) {
            	if (lhs.getClass().equals(rhs.getClass())) return lhs.getClass();
            	
            	int lhsIndex = WHOLE_NUMBER_CLASSES.indexOf(lhs.getClass());
            	int rhsIndex = WHOLE_NUMBER_CLASSES.indexOf(rhs.getClass());

            	Class<?> widestClass;
            	if (lhsIndex != -1 && rhsIndex != -1) {
            		widestClass = (Class<?>) WHOLE_NUMBER_CLASSES.get(Math.max(lhsIndex, rhsIndex));
            	} else {
            		widestClass = Double.class;
            	}
            	
            	return widestClass;
            }
            
            /**
             * Enables equality tests for two boxed numbers of different types, like java.lang.Short and java.lang.Integer.
             */
            boolean equalNumbers(Number lhs, Object _rhs) {
            	if (!Number.class.isAssignableFrom(_rhs.getClass())) {
            		return lhs.equals(_rhs);
            	}
            	
            	Number rhs = (Number) _rhs;
            	
            	Class<?> widestClass = widestNumberType(lhs, rhs);
            	
            	if (Integer.class.equals(widestClass) || AtomicInteger.class.equals(widestClass)) {
            		return lhs.intValue() == rhs.intValue();
            		
            	} else if (Long.class.equals(widestClass) || AtomicLong.class.equals(widestClass)) {
            		return lhs.longValue() == rhs.longValue();
            		
            	} else if (Double.class.equals(widestClass)) {
            		return Math.abs(lhs.doubleValue() - rhs.doubleValue()) < EPSILON;
            		
            	} else if (Short.class.equals(widestClass)) {
                	return lhs.shortValue() == rhs.shortValue();
                	
            	} else if (BigInteger.class.equals(widestClass)) {
            		return lhs.toString().equals(rhs.toString());
            		
            	} else {
            		return lhs.equals(rhs);
            	}
            }
            
            /**
             * Enables comparison of two boxed numbers of different types, like java.lang.Short and java.lang.Integer.
             */
            Integer compareNumbers(Number lhs, Object _rhs) {
            	if (!Number.class.isAssignableFrom(_rhs.getClass())) {
            		return null;
            	}
            	
            	Number rhs = (Number) _rhs;
            	
            	Class widestClass = widestNumberType(lhs, rhs);
            	
            	if (Integer.class.equals(widestClass) || AtomicInteger.class.equals(widestClass)) {
            		return Integer.valueOf(lhs.intValue()).compareTo(rhs.intValue());
            		
            	} else if (Long.class.equals(widestClass) || AtomicLong.class.equals(widestClass)) {
            		return Long.valueOf(lhs.longValue()).compareTo(rhs.longValue());
            		
            	} else if (Double.class.equals(widestClass)) {
            		boolean areEqual = Math.abs(lhs.doubleValue() - rhs.doubleValue()) < EPSILON;
            		return areEqual ? 0 : Double.compare(lhs.doubleValue(), rhs.doubleValue());
            		
            	} else if (Float.class.equals(widestClass)) {
            		boolean areEqual = Math.abs(lhs.floatValue() - rhs.floatValue()) < EPSILON;
            		return areEqual ? 0 : Float.compare(lhs.floatValue(), rhs.floatValue());
            		
            	} else if (Short.class.equals(widestClass)) {
                	return Short.valueOf(lhs.shortValue()).compareTo(rhs.shortValue());
                	
            	} else if (Byte.class.equals(widestClass)) {
                	return Byte.valueOf(lhs.byteValue()).compareTo(rhs.byteValue());
                	
            	} else if (BigInteger.class.equals(widestClass)) {
            		BigInteger left = lhs instanceof BigInteger ? (BigInteger)lhs : new BigInteger(lhs.toString());
            		BigInteger right = rhs instanceof BigInteger ? (BigInteger)rhs : new BigInteger(rhs.toString());
            		return left.compareTo(right);
            		
            	} else if (Comparable.class.isAssignableFrom(lhs.getClass())) {
                    return ((Comparable)lhs).compareTo(rhs);
                    
                } else {
            		return null;
            	}
            }
        });
        
        COMPARABLE_EVALUATOR = new NonNullLhsEvaluator(new Evaluator() {

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            Integer compare(Object lhs, Object rhs) {
                return ((Comparable) lhs).compareTo(ConversionUtil.toComparable(rhs));
            }

            @Override
            boolean eq(Object lhs, Object rhs) {
                return lhs.equals(rhs);
            }
        });
    }

    static Evaluator evaluator(Object lhs) {

        if (lhs == null) {
            return NULL_LHS_EVALUATOR;
        }

        Class<?> lhsType = lhs.getClass();

        Evaluator e = evaluators.get(lhsType);

        if (e == null) {
            Evaluator created = compileEvaluator(lhsType);
            Evaluator existing = evaluators.putIfAbsent(lhsType, created);
            e = existing != null ? existing : created;
        }

        return e;
    }

    private static Evaluator compileEvaluator(Class<?> lhsType) {

        Evaluator ev = findInHierarchy(lhsType);
        if (ev != null) {
            return ev;
        }

        // check known interfaces
        if (Persistent.class.isAssignableFrom(lhsType)) {
            return PERSISTENT_EVALUATOR;
        }

        if (BigDecimal.class.isAssignableFrom(lhsType)) {
            return BIG_DECIMAL_EVALUATOR;
        }

        if (Number.class.isAssignableFrom(lhsType)) {
            return NUMBER_EVALUATOR;
        }
        
        if (Comparable.class.isAssignableFrom(lhsType)) {
            return COMPARABLE_EVALUATOR;
        }

        // nothing we recognize... return default
        return DEFAULT_EVALUATOR;
    }

    private static Evaluator findInHierarchy(Class<?> lhsType) {

        if (Object.class.equals(lhsType)) {
            return null;
        }

        Evaluator ev = evaluators.get(lhsType);
        return (ev != null) ? ev : findInHierarchy(lhsType.getSuperclass());
    }

    abstract boolean eq(Object lhs, Object rhs);

    /**
     * Returns NULL if comparison is invalid, otherwise returns positive,
     * negative or zero, with the same meaning as
     * {@link Comparable#compareTo(Object)}.
     */
    abstract Integer compare(Object lhs, Object rhs);
}
