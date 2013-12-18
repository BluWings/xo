package com.buschmais.cdo.store.json.impl;

import com.buschmais.cdo.spi.datastore.Datastore;
import com.buschmais.cdo.spi.datastore.DatastoreMetadataFactory;
import com.buschmais.cdo.spi.metadata.TypeMetadata;
import com.buschmais.cdo.store.json.impl.metadata.JsonNodeMetadata;

import java.io.File;
import java.util.Collection;

public class JsonFileDatastore implements Datastore<JsonFileDatastoreSession, JsonNodeMetadata, String> {

    private File directory;

    public JsonFileDatastore(String directory) {
        this.directory = new File(directory);
        this.directory.mkdirs();
    }

    @Override
    public DatastoreMetadataFactory<JsonNodeMetadata, String> getMetadataFactory() {
        return new JsonMetadataFactory();
    }

    @Override
    public JsonFileDatastoreSession createSession() {
        return new JsonFileDatastoreSession(directory);
    }

    @Override
    public void close() {
    }

    @Override
    public void init(Collection<TypeMetadata<JsonNodeMetadata>> registeredMetadata) {
    }
}