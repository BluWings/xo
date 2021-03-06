package com.buschmais.xo.neo4j.test;

import java.net.URI;
import java.net.URISyntaxException;

import com.buschmais.xo.neo4j.api.Neo4jXOProvider;
import com.buschmais.xo.test.AbstractXOManagerTest;

/**
 * Defines the databases under test for Neo4j.
 */
public enum Neo4jDatabase implements AbstractXOManagerTest.Database {
    MEMORY("memory:///");
    private URI uri;

    Neo4jDatabase(String uri) {
        try {
            this.uri = new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public URI getUri() {
        return uri;
    }

    @Override
    public Class<?> getProvider() {
        return Neo4jXOProvider.class;
    }
}
