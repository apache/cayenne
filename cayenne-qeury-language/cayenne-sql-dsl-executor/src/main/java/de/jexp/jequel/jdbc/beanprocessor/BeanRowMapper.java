package de.jexp.jequel.jdbc.beanprocessor;

/**
 * @author mh14 @ jexp.de
 * @since 02.11.2007 20:30:51 (c) 2007 jexp.de
 */
public interface BeanRowMapper<I, O> extends BeanRowProcessor<I> {
    String MAP_BEAN_METHOD = "mapBean";

    O mapBean(I bean);
}
