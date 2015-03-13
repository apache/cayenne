package de.jexp.jequel.jdbc.beanprocessor;

public interface BeanRowHandler<I> extends BeanRowProcessor<I> {

    void handleBean(I bean);
}