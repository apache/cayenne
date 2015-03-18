package de.jexp.jequel.execute;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ExecutableParams {
    boolean hasParams();

    boolean hasOnlyNamed();

    int getParamCount();

    List<Object> getParamValues();

    Collection<?> getParamNames();

    Map<String, Object> getNamedParams();

    void addParams(ExecutableParams executableParams);
}
