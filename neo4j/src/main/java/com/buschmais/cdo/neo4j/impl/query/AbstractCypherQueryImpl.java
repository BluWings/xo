package com.buschmais.cdo.neo4j.impl.query;

import com.buschmais.cdo.api.CdoException;
import com.buschmais.cdo.api.Query;
import com.buschmais.cdo.neo4j.impl.node.InstanceManager;
import com.buschmais.cdo.neo4j.spi.DatastoreSession;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class AbstractCypherQueryImpl<QL> implements Query {

    private QL expression;
    private DatastoreSession datastoreSession;
    private InstanceManager instanceManager;
    private List<Class<?>> types;
    private Map<String, Object> parameters = null;

    public AbstractCypherQueryImpl(QL expression, DatastoreSession datastoreSession, InstanceManager instanceManager, List<Class<?>> types) {
        this.expression = expression;
        this.datastoreSession = datastoreSession;
        this.instanceManager = instanceManager;
        this.types = types;
    }

    @Override
    public Query withParameter(String name, Object value) {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        Object oldValue = parameters.put(name, value);
        if (oldValue != null) {
            throw new CdoException("Parameter '" + name + "' has alread been assigned to value '" + value + "'.");
        }
        return this;
    }

    @Override
    public Query withParameters(Map<String, Object> parameters) {
        if (this.parameters != null) {
            throw new CdoException(("Parameters have already beed assigned: " + parameters));
        }
        this.parameters = parameters;
        return this;
    }

    @Override
    public <T> Result<T> execute() {
        String query = getQuery();
        Map<String, Object> effectiveParameters = new HashMap<>();
        if (parameters != null) {
            for (Map.Entry<String, Object> parameterEntry : parameters.entrySet()) {
                String name = parameterEntry.getKey();
                Object value = parameterEntry.getValue();
                if (instanceManager.isNode(value)) {
                    value = instanceManager.getNode(value);
                }
                effectiveParameters.put(name, value);
            }
        }
        Iterator<Map<String,Object>> iterator = datastoreSession.execute(query, effectiveParameters);
        List<Class<?>> resultTypes = getResultTypes(expression, types);
        return new IterableQueryResultImpl(instanceManager, iterator, resultTypes);
    }

    protected QL getExpression() {
        return expression;
    }

    protected abstract String getQuery();

    protected abstract List<Class<?>> getResultTypes(QL expression, List<Class<?>> types);

}
