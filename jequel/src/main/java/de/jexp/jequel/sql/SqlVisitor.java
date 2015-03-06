package de.jexp.jequel.sql;

/**
 * @author mh14 @ jexp.de
 * @since 05.11.2007 00:43:35 (c) 2007 jexp.de
 */
public interface SqlVisitor<R> {
    R visit(SelectPartColumnListExpression sqlPartColumnTupleExpression);

    R visit(SelectPartMutableBooleanExpression selectPartMutableBooleanExpression);
}
