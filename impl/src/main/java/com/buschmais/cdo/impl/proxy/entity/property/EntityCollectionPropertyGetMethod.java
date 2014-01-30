package com.buschmais.cdo.impl.proxy.entity.property;

import com.buschmais.cdo.api.CdoException;
import com.buschmais.cdo.impl.AbstractPropertyManager;
import com.buschmais.cdo.impl.SessionContext;
import com.buschmais.cdo.impl.proxy.collection.EntityCollectionProxy;
import com.buschmais.cdo.impl.proxy.collection.ListProxy;
import com.buschmais.cdo.impl.proxy.collection.SetProxy;
import com.buschmais.cdo.impl.proxy.common.property.AbstractPropertyMethod;
import com.buschmais.cdo.spi.metadata.method.EntityCollectionPropertyMethodMetadata;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class EntityCollectionPropertyGetMethod<Entity, Relation> extends AbstractPropertyMethod<Entity, Entity, Relation, EntityCollectionPropertyMethodMetadata> {

    public EntityCollectionPropertyGetMethod(SessionContext<?, Entity, ?, ?, ?, Relation, ?, ?> sessionContext, EntityCollectionPropertyMethodMetadata<?> metadata) {
        super(sessionContext, metadata);
    }

    @Override
    protected AbstractPropertyManager<Entity, Entity, Relation> getPropertyManager() {
        return getSessionContext().getEntityPropertyManager();
    }

    @Override
    public Object invoke(Entity entity, Object instance, Object[] args) {
        EntityCollectionPropertyMethodMetadata<?> collectionPropertyMetadata = getMetadata();
        EntityCollectionProxy<?, Entity, Relation> collectionProxy = new EntityCollectionProxy<>(getSessionContext(), entity, getMetadata());
        Collection<?> collection;
        if (Set.class.isAssignableFrom(collectionPropertyMetadata.getAnnotatedMethod().getType())) {
            collection = new SetProxy<>(collectionProxy);
        } else if (List.class.isAssignableFrom(collectionPropertyMetadata.getAnnotatedMethod().getType())) {
            collection = new ListProxy<>(collectionProxy);
        } else if (Collection.class.isAssignableFrom(collectionPropertyMetadata.getAnnotatedMethod().getType())) {
            collection = collectionProxy;
        } else {
            throw new CdoException("Unsupported collection type " + collectionPropertyMetadata.getAnnotatedMethod().getType());
        }
        Collection<?> result = getSessionContext().getInterceptorFactory().addInterceptor(collection);
        return result;
    }

}