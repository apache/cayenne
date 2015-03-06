package de.jexp.jequel.jdbc.beanprocessor;

/**
 * @author mh14 @ jexp.de
 * @since 02.11.2007 20:30:51 (c) 2007 jexp.de
 */
public interface BeanRowHandler<I> extends BeanRowProcessor<I> {
    String HANDLE_BEAN_METHOD = "handleBean";

    void handleBean(I bean);
}