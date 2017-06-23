package com.verygood.security.audit;

import com.google.common.base.Predicates;

import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;

import static java.util.stream.Collectors.toList;

class AuditFactory {
  private AuditFactory() {

  }

  static ModifiedEntityAudit createModifiedEntityAudit(Serializable id, Object entity, Object[] previousState, Object[] currentState, String[] propertyNames, AuditAction action) {
    List<ModifiedEntityAuditField> modifiedFields = createModifiedFields(entity, previousState, currentState, propertyNames);
    return new ModifiedEntityAudit(id, entity.getClass(), action, modifiedFields);
  }

  private static List<ModifiedEntityAuditField> createModifiedFields(Object entity,
                                                                     Object[] previousState,
                                                                     Object[] currentState,
                                                                     String[] propertyNames
  ) {
    List<ModifiedEntityAuditField> modifiedEntityAuditFields = new ArrayList<>();
    for (Field field : entity.getClass().getDeclaredFields()) {
      if (field.isAnnotationPresent(Audited.class)) {
        for (int i = 0; i < propertyNames.length; i++) {
          if (propertyNames[i].equals(field.getName())) {
            Object oldValue = previousState.length > 0 ? previousState[i] : null;
            Object newValue = currentState.length > 0 ? currentState[i] : null;

            // don't touch lazy old fields
            if (!Hibernate.isInitialized(oldValue)) {
              continue;
            }

            // transform relationships to ids
            if (oldValue instanceof Collection) {
              oldValue = retrieveCollectionIds(oldValue);
            }

            if (newValue instanceof Collection) {
              newValue = retrieveCollectionIds(newValue);
            }

            if (oldValue != null && isEntity(oldValue)) {
              oldValue = retrieveId(oldValue);
            }

            if (newValue != null && isEntity(newValue)) {
              newValue = retrieveId(newValue);
            }

            boolean areEqual = AuditUtils.areEqualOrCompareEqual(oldValue, newValue)
                || AuditUtils.areEqualEmptyCollection(oldValue, newValue);

            if (areEqual) {
              continue;
            }

            modifiedEntityAuditFields.add(new ModifiedEntityAuditField(field.getName(), oldValue, newValue));
          }
        }
      }
    }
    return modifiedEntityAuditFields;
  }

  private static boolean isEntity(Object oldValue) {
    return oldValue.getClass().isAnnotationPresent(Entity.class);
  }

  private static Object retrieveCollectionIds(Object oldValue) {
    Collection elements = (Collection) oldValue;
    boolean isEntities = elements.stream().allMatch(AuditFactory::isEntity);
    return isEntities ? entitiesToIds(elements) : oldValue;
  }

  private static Object entitiesToIds(Collection elements) {
    return elements.stream()
        .map(AuditFactory::retrieveId)
        .filter(Predicates.notNull())
        .collect(toList());
  }

  private static Object retrieveId(Object entity) {
    if (entity == null) {
      throw new IllegalArgumentException("Passed entity cannot be null");
    }
    if (entity instanceof HibernateProxy) {
      return ((HibernateProxy) entity).getHibernateLazyInitializer().getIdentifier();
    }
    return retrieveWithReflection(entity);
  }

  private static Object retrieveWithReflection(Object entity) {
    Field[] fields = entity.getClass().getDeclaredFields();
    for (Field field : fields) {
      if (field.isAnnotationPresent(Id.class)) {
        try {
          field.setAccessible(true);
          return field.get(entity);
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return null;
  }
}
