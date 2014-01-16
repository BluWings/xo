package com.buschmais.cdo.impl;

import com.buschmais.cdo.api.*;
import com.buschmais.cdo.impl.query.CdoQueryImpl;
import com.buschmais.cdo.impl.transaction.TransactionalResultIterable;
import com.buschmais.cdo.spi.datastore.DatastoreEntityMetadata;
import com.buschmais.cdo.spi.datastore.DatastoreRelationMetadata;
import com.buschmais.cdo.spi.datastore.DatastoreSession;
import com.buschmais.cdo.spi.datastore.TypeMetadataSet;
import com.buschmais.cdo.spi.metadata.type.EntityTypeMetadata;
import com.buschmais.cdo.spi.metadata.type.RelationTypeMetadata;

import javax.validation.ConstraintViolation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.buschmais.cdo.api.Query.Result.CompositeRowObject;

/**
 * Generic implementation of a {@link CdoManager}.
 *
 * @param <EntityId>              The type of entity ids as provided by the datastore.
 * @param <Entity>                The type entities as provided by the datastore.
 * @param <EntityMetadata>        The type of entity metadata as provided by the datastore.
 * @param <EntityDiscriminator>   The type of discriminators as provided by the datastore.
 * @param <RelationId>            The type of relation ids as provided by the datastore.
 * @param <Relation>              The type of relations as provided by the datastore.
 * @param <RelationMetadata>      The type of relation metadata as provided by the datastore.
 * @param <RelationDiscriminator> The type of relation discriminators as provided by the datastore.
 */
public class CdoManagerImpl<EntityId, Entity, EntityMetadata extends DatastoreEntityMetadata<EntityDiscriminator>, EntityDiscriminator, RelationId, Relation, RelationMetadata extends DatastoreRelationMetadata<RelationDiscriminator>, RelationDiscriminator> implements CdoManager {

    private SessionContext<EntityId, Entity, EntityMetadata, EntityDiscriminator, RelationId, Relation, RelationMetadata, RelationDiscriminator> sessionContext;

    /**
     * Constructor.
     *
     * @param sessionContext The associated {@link SessionContext}.
     */
    public CdoManagerImpl(SessionContext<EntityId, Entity, EntityMetadata, EntityDiscriminator, RelationId, Relation, RelationMetadata, RelationDiscriminator> sessionContext) {
        this.sessionContext = sessionContext;
    }

    @Override
    public CdoTransaction currentTransaction() {
        return sessionContext.getCdoTransaction();
    }

    @Override
    public Set<ConstraintViolation<Object>> validate() {
        return sessionContext.getInstanceValidator().validate();
    }

    @Override
    public <T> ResultIterable<T> find(final Class<T> type, final Object value) {
        EntityTypeMetadata<EntityMetadata> entityTypeMetadata = sessionContext.getMetadataProvider().getEntityMetadata(type);
        EntityDiscriminator entityDiscriminator = entityTypeMetadata.getDatastoreMetadata().getDiscriminator();
        if (entityDiscriminator == null) {
            throw new CdoException("Type " + type.getName() + " has no discriminator (i.e. cannot be identified in datastore).");
        }
        final ResultIterator<Entity> iterator = sessionContext.getDatastoreSession().find(entityTypeMetadata, entityDiscriminator, value);
        return new TransactionalResultIterable<>(new AbstractResultIterable<T>() {
            @Override
            public ResultIterator<T> iterator() {
                return new ResultIterator<T>() {

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public T next() {
                        Entity entity = iterator.next();
                        InstanceManager<EntityId, Entity> entityInstanceManager = sessionContext.getEntityInstanceManager();
                        return entityInstanceManager.getInstance(entity);
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("Cannot remove instance.");
                    }

                    @Override
                    public void close() {
                        iterator.close();
                    }
                };
            }
        }, sessionContext.getCdoTransaction());
    }

    @Override
    public CompositeObject create(Class type, Class<?>... types) {
        TypeMetadataSet<EntityTypeMetadata<EntityMetadata>> effectiveTypes = getEffectiveTypes(type, types);
        Set<EntityDiscriminator> entityDiscriminators = sessionContext.getMetadataProvider().getEntityDiscriminators(effectiveTypes);
        DatastoreSession<EntityId, Entity, EntityMetadata, EntityDiscriminator, RelationId, Relation, RelationMetadata, RelationDiscriminator> datastoreSession = sessionContext.getDatastoreSession();
        Entity entity = datastoreSession.create(effectiveTypes, entityDiscriminators);
        InstanceManager<EntityId, Entity> entityInstanceManager = sessionContext.getEntityInstanceManager();
        return entityInstanceManager.getInstance(entity);
    }

    public <T> T create(Class<T> type) {
        return create(type, new Class<?>[0]).as(type);
    }

    @Override
    public <S, R, T> R create(S source, Class<R> relationType, T target) {
        InstanceManager<EntityId, Entity> entityInstanceManager = sessionContext.getEntityInstanceManager();
        Entity sourceEntity = entityInstanceManager.getDatastoreType(source);
        Set<Class<?>> sourceTypes = new HashSet<>(Arrays.asList(source.getClass().getInterfaces()));
        MetadataProvider<EntityMetadata, EntityDiscriminator, RelationMetadata, RelationDiscriminator> metadataProvider = sessionContext.getMetadataProvider();
        RelationTypeMetadata<RelationMetadata> relationTypeMetadata = metadataProvider.getRelationMetadata(relationType);
        Entity targetEntity = entityInstanceManager.getDatastoreType(target);
        Set<Class<?>> targetTypes = new HashSet<>(Arrays.asList(target.getClass().getInterfaces()));
        RelationTypeMetadata.Direction direction = metadataProvider.getRelationDirection(sourceTypes, relationTypeMetadata, targetTypes);
        PropertyManager<EntityId, Entity, RelationId, Relation> propertyManager = sessionContext.getPropertyManager();
        Relation relation = propertyManager.createSingleRelation(sourceEntity, relationTypeMetadata, direction, targetEntity);
        InstanceManager<RelationId, Relation> relationInstanceManager = sessionContext.getRelationInstanceManager();
        return relationInstanceManager.getInstance(relation);
    }

    @Override
    public <T, M> CompositeObject migrate(T instance, MigrationStrategy<T, M> migrationStrategy, Class<M> targetType, Class<?>... targetTypes) {
        InstanceManager<EntityId, Entity> entityInstanceManager = sessionContext.getEntityInstanceManager();
        Entity entity = entityInstanceManager.getDatastoreType(instance);
        DatastoreSession<EntityId, Entity, EntityMetadata, EntityDiscriminator, RelationId, Relation, RelationMetadata, RelationDiscriminator> datastoreSession = sessionContext.getDatastoreSession();
        Set<EntityDiscriminator> entityDiscriminators = datastoreSession.getEntityDiscriminators(entity);
        MetadataProvider<EntityMetadata, EntityDiscriminator, RelationMetadata, RelationDiscriminator> metadataProvider = sessionContext.getMetadataProvider();
        TypeMetadataSet<EntityTypeMetadata<EntityMetadata>> types = metadataProvider.getTypes(entityDiscriminators);
        TypeMetadataSet<EntityTypeMetadata<EntityMetadata>> effectiveTargetTypes = getEffectiveTypes(targetType, targetTypes);
        Set<EntityDiscriminator> targetEntityDiscriminators = metadataProvider.getEntityDiscriminators(effectiveTargetTypes);
        datastoreSession.migrate(entity, types, entityDiscriminators, effectiveTargetTypes, targetEntityDiscriminators);
        entityInstanceManager.removeInstance(instance);
        CompositeObject migratedInstance = entityInstanceManager.getInstance(entity);
        if (migrationStrategy != null) {
            migrationStrategy.migrate(instance, migratedInstance.as(targetType));
        }
        entityInstanceManager.destroyInstance(instance);
        return migratedInstance;
    }

    @Override
    public <T, M> CompositeObject migrate(T instance, Class<M> targetType, Class<?>... targetTypes) {
        return migrate(instance, null, targetType, targetTypes);
    }

    @Override
    public <T, M> M migrate(T instance, MigrationStrategy<T, M> migrationStrategy, Class<M> targetType) {
        return migrate(instance, migrationStrategy, targetType, new Class<?>[0]).as(targetType);
    }

    @Override
    public <T, M> M migrate(T instance, Class<M> targetType) {
        return migrate(instance, null, targetType);
    }

    @Override
    public <T> void delete(T instance) {
        InstanceManager<EntityId, Entity> entityInstanceManager = sessionContext.getEntityInstanceManager();
        InstanceManager<RelationId, Relation> relationInstanceManager = sessionContext.getRelationInstanceManager();
        DatastoreSession<EntityId, Entity, EntityMetadata, EntityDiscriminator, RelationId, Relation, RelationMetadata, RelationDiscriminator> datastoreSession = sessionContext.getDatastoreSession();
        if (entityInstanceManager.isInstance(instance)) {
            Entity entity = entityInstanceManager.getDatastoreType(instance);
            datastoreSession.deleteEntity(entity);
            entityInstanceManager.removeInstance(instance);
            entityInstanceManager.destroyInstance(instance);
        } else if (relationInstanceManager.isInstance(instance)) {
            Relation relation = relationInstanceManager.getDatastoreType(instance);
            datastoreSession.getDatastorePropertyManager().deleteRelation(relation);
            relationInstanceManager.removeInstance(instance);
            relationInstanceManager.destroyInstance(instance);
        } else {
            throw new CdoException(instance + " is not a managed CDO instance.");
        }
    }

    @Override
    public Query<CompositeRowObject> createQuery(String query) {
        CdoQueryImpl<CompositeRowObject, String, Entity, Relation> cdoQuery = new CdoQueryImpl<>(sessionContext, query, Collections.<Class<?>>emptyList());
        return sessionContext.getInterceptorFactory().addInterceptor(cdoQuery);
    }

    @Override
    public <T> Query<T> createQuery(String query, Class<T> type) {
        CdoQueryImpl<T, String, Entity, Relation> cdoQuery = new CdoQueryImpl<>(sessionContext, query, Arrays.asList(new Class<?>[]{type}));
        return sessionContext.getInterceptorFactory().addInterceptor(cdoQuery);
    }

    @Override
    public Query<CompositeRowObject> createQuery(String query, Class<?> type, Class<?>... types) {
        CdoQueryImpl<CompositeRowObject, String, Entity, Relation> cdoQuery = new CdoQueryImpl<>(sessionContext, query, Arrays.asList(types));
        return sessionContext.getInterceptorFactory().addInterceptor(cdoQuery);
    }

    @Override
    public <T> Query<T> createQuery(Class<T> query) {
        CdoQueryImpl<T, Class<T>, Entity, Relation> cdoQuery = new CdoQueryImpl<>(sessionContext, query, Arrays.asList(new Class<?>[]{query}));
        return sessionContext.getInterceptorFactory().addInterceptor(cdoQuery);
    }

    @Override
    public <Q> Query<CompositeRowObject> createQuery(Class<Q> query, Class<?>... types) {
        CdoQueryImpl<CompositeRowObject, Class<Q>, Entity, Relation> cdoQuery = new CdoQueryImpl<>(sessionContext, query, Arrays.asList(types));
        return sessionContext.getInterceptorFactory().addInterceptor(cdoQuery);
    }

    @Override
    public void close() {
        sessionContext.getEntityInstanceManager().close();
        sessionContext.getRelationInstanceManager().close();
    }

    @Override
    public <DS> DS getDatastoreSession(Class<DS> sessionType) {
        return sessionType.cast(sessionContext.getDatastoreSession());
    }

    @Override
    public void flush() {
        for (Object instance : sessionContext.getRelationCache().values()) {
            Relation relation = sessionContext.getRelationInstanceManager().getDatastoreType(instance);
            sessionContext.getDatastoreSession().flushRelation(relation);
        }
        for (Object instance : sessionContext.getEntityCache().values()) {
            Entity entity = sessionContext.getEntityInstanceManager().getDatastoreType(instance);
            sessionContext.getDatastoreSession().flushEntity(entity);
        }
    }

    private TypeMetadataSet<EntityTypeMetadata<EntityMetadata>> getEffectiveTypes(Class<?> type, Class<?>... types) {
        MetadataProvider<EntityMetadata, EntityDiscriminator, RelationMetadata, RelationDiscriminator> metadataProvider = sessionContext.getMetadataProvider();
        TypeMetadataSet<EntityTypeMetadata<EntityMetadata>> effectiveTypes = new TypeMetadataSet<>();
        effectiveTypes.add(metadataProvider.getEntityMetadata(type));
        for (Class<?> otherType : types) {
            effectiveTypes.add(metadataProvider.getEntityMetadata(otherType));
        }
        return effectiveTypes;
    }
}
