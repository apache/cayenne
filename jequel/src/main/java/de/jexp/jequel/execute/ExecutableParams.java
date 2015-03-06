package de.jexp.jequel.execute;

import java.util.Collection;
import java.util.Map;

public interface ExecutableParams {
    boolean hasParams();

    boolean hasOnlyNamed();

    int getParamCount();

    Collection<?> getParamValues();

    Collection<?> getParamNames();

    Map<String, Object> getNamedParams();

    void addParams(ExecutableParams executableParams);
}
