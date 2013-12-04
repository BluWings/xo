package com.buschmais.cdo.neo4j.test;

import com.buschmais.cdo.api.CdoException;
import com.buschmais.cdo.api.CdoManager;
import com.buschmais.cdo.api.CdoManagerFactory;
import com.buschmais.cdo.api.Query;
import com.buschmais.cdo.api.bootstrap.CdoUnit;
import com.buschmais.cdo.neo4j.impl.AbstractNeo4jCdoManagerFactoryImpl;
import org.junit.After;
import org.junit.Before;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static com.buschmais.cdo.api.Query.Result.CompositeRowObject;

public abstract class AbstractCdoManagerTest {

    private CdoManagerFactory cdoManagerFactory;
    private CdoManager cdoManager = null;

    @Before
    public void createNodeManagerFactory() throws MalformedURLException {
        cdoManagerFactory = getNeo4jCdoManagerFactory(getTypes());
        dropDatabase();
    }

    protected abstract AbstractNeo4jCdoManagerFactoryImpl getNeo4jCdoManagerFactory(Class<?>[] types) throws MalformedURLException;

    protected abstract Class<?>[] getTypes();

    private void dropDatabase() {
        CdoManager manager = getCdoManager();
        manager.begin();
        manager.createQuery("MATCH (n)-[r]-() DELETE r").execute();
        manager.createQuery("MATCH (n) DELETE n").execute();
        manager.commit();
    }

    @After
    public void closeNodeManagerFactory() {
        closeCdoManager();
        cdoManagerFactory.close();
    }

    protected CdoUnit createCdoUnit(URL url, Class<?>[] types) {
        return new CdoUnit("test", "test unit", url, null, new HashSet<>(Arrays.asList(types)), CdoUnit.ValidationMode.AUTO, CdoUnit.TransactionAttribute.MANDATORY, new Properties());
    }

    protected CdoUnit createCdoUnit(String url, Class<?>[] types) {
        try {
            return createCdoUnit(new URL(url), types);
        } catch (MalformedURLException e) {
            throw new CdoException("Invalid url.", e);
        }
    }

    /**
     * Executes a createQuery and returns a {@link TestResult}.
     *
     * @param query The createQuery.
     * @return The {@link TestResult}.
     */
    protected TestResult executeQuery(String query) {
        return executeQuery(query, Collections.<String, Object>emptyMap());
    }

    /**
     * Executes a createQuery and returns a {@link TestResult}.
     *
     * @param query      The createQuery.
     * @param parameters The createQuery parameters.
     * @return The {@link TestResult}.
     */
    protected TestResult executeQuery(String query, Map<String, Object> parameters) {
        Query.Result<CompositeRowObject> result = cdoManager.createQuery(query).withParameters(parameters).execute();
        Map<String, List<Object>> columns = new HashMap<>();
        for (CompositeRowObject row : result) {
            Iterable<String> columnNames = row.getColumns();
            for (String columnName : columnNames) {
                List<Object> columnValues = columns.get(columnName);
                if (columnValues == null) {
                    columnValues = new ArrayList<>();
                    columns.put(columnName, columnValues);
                }
                columnValues.add(row.get(columnName, Object.class));
            }
        }
        return new TestResult(columns);
    }

    protected CdoManagerFactory getCdoManagerFactory() {
        return cdoManagerFactory;
    }

    protected CdoManager getCdoManager() {
        if (cdoManager == null) {
            cdoManager = getCdoManagerFactory().createCdoManager();
        }
        return cdoManager;
    }

    protected void closeCdoManager() {
        if (cdoManager != null) {
            cdoManager.close();
            cdoManager = null;
        }
    }


    /**
     * Represents a test result which allows fetching values by row or columns.
     */
    protected class TestResult {

        private Map<String, List<Object>> columns;

        TestResult(Map<String, List<Object>> columns) {
            this.columns = columns;
        }

        /**
         * Return a column identified by its name.
         *
         * @param <T> The expected type.
         * @return All columns.
         */
        public <T> List<T> getColumn(String name) {
            return (List<T>) columns.get(name);
        }
    }
}
