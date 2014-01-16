package com.buschmais.cdo.impl;

import com.buschmais.cdo.api.CdoTransaction;
import com.buschmais.cdo.api.TransactionAttribute;
import com.buschmais.cdo.impl.cache.EntityCacheSynchronization;
import com.buschmais.cdo.impl.cache.RelationCacheSynchronization;
import com.buschmais.cdo.impl.cache.TransactionalCache;
import com.buschmais.cdo.impl.interceptor.InterceptorFactory;
import com.buschmais.cdo.impl.validation.InstanceValidator;
import com.buschmais.cdo.impl.validation.ValidatorSynchronization;
import com.buschmais.cdo.spi.datastore.DatastoreEntityMetadata;
import com.buschmais.cdo.spi.datastore.DatastoreRelationMetadata;
import com.buschmais.cdo.spi.datastore.DatastoreSession;

import javax.validation.ValidatorFactory;

/**
 * Provides access to all session related services.
 *
 * @param <EntityId>              The type of entity ids.
 * @param <Entity>                The type of entities.
 * @param <EntityMetadata>        The type of entity metadata.
 * @param <EntityDiscriminator>   The type of entity discriminators
 * @param <RelationId>            The type of relation ids.
 * @param <Relation>              The type of relations.
 * @param <RelationMetadata>      The type of relation metadata.
 * @param <RelationDiscriminator> The type of relation discriminators.
 */
public class SessionContext<EntityId, Entity, EntityMetadata extends DatastoreEntityMetadata<EntityDiscriminator>, EntityDiscriminator, RelationId, Relation, RelationMetadata extends DatastoreRelationMetadata<RelationDiscriminator>, RelationDiscriminator> {

    private MetadataProvider<EntityMetadata, EntityDiscriminator, RelationMetadata, RelationDiscriminator> metadataProvider;

    private InstanceManager<EntityId, Entity> entityInstanceManager;

    private InstanceManager<RelationId, Relation> relationInstanceManager;

    private TransactionalCache<EntityId> entityCache;

    private TransactionalCache<RelationId> relationCache;

    private InstanceValidator instanceValidator;

    private CdoTransactionImpl cdoTransaction;

    private PropertyManager<EntityId, Entity, RelationId, Relation> propertyManager;

    private InterceptorFactory interceptorFactory;

    private ProxyFactory proxyFactory;

    private DatastoreSession<EntityId, Entity, EntityMetadata, EntityDiscriminator, RelationId, Relation, RelationMetadata, RelationDiscriminator> datastoreSession;

    public SessionContext(MetadataProvider<EntityMetadata, EntityDiscriminator, RelationMetadata, RelationDiscriminator> metadataProvider, DatastoreSession<EntityId, Entity, EntityMetadata, EntityDiscriminator, RelationId, Relation, RelationMetadata, RelationDiscriminator> datastoreSession, ValidatorFactory validatorFactory, TransactionAttribute defaultTransactionAttribute, ClassLoader classLoader) {
        this.metadataProvider = metadataProvider;
        this.datastoreSession = datastoreSession;
        this.entityCache = new TransactionalCache<>();
        this.relationCache = new TransactionalCache<>();
        this.instanceValidator = new InstanceValidator(validatorFactory, relationCache, entityCache);
        this.cdoTransaction = new CdoTransactionImpl(datastoreSession.getDatastoreTransaction());
        this.interceptorFactory = new InterceptorFactory(cdoTransaction, defaultTransactionAttribute);
        this.proxyFactory = new ProxyFactory(interceptorFactory, classLoader);
        this.propertyManager = new PropertyManager<>(datastoreSession);
        this.relationInstanceManager = new RelationInstanceManager<>(this);
        this.entityInstanceManager = new EntityInstanceManager<>(this);
        // Register default synchronizations.
        cdoTransaction.registerDefaultSynchronization(new ValidatorSynchronization(instanceValidator));
        cdoTransaction.registerDefaultSynchronization(new EntityCacheSynchronization<>(entityInstanceManager, entityCache, datastoreSession));
        cdoTransaction.registerDefaultSynchronization(new RelationCacheSynchronization<>(relationInstanceManager, relationCache, datastoreSession));
    }


    public MetadataProvider<EntityMetadata, EntityDiscriminator, RelationMetadata, RelationDiscriminator> getMetadataProvider() {
        return metadataProvider;
    }

    public InstanceManager<EntityId, Entity> getEntityInstanceManager() {
        return entityInstanceManager;
    }

    public InstanceManager<RelationId, Relation> getRelationInstanceManager() {
        return relationInstanceManager;
    }

    public TransactionalCache<EntityId> getEntityCache() {
        return entityCache;
    }

    public TransactionalCache<RelationId> getRelationCache() {
        return relationCache;
    }

    public InstanceValidator getInstanceValidator() {
        return instanceValidator;
    }

    public CdoTransaction getCdoTransaction() {
        return cdoTransaction;
    }

    public PropertyManager<EntityId, Entity, RelationId, Relation> getPropertyManager() {
        return propertyManager;
    }

    public InterceptorFactory getInterceptorFactory() {
        return interceptorFactory;
    }

    public ProxyFactory getProxyFactory() {
        return proxyFactory;
    }

    public DatastoreSession<EntityId, Entity, EntityMetadata, EntityDiscriminator, RelationId, Relation, RelationMetadata, RelationDiscriminator> getDatastoreSession() {
        return datastoreSession;
    }
}
