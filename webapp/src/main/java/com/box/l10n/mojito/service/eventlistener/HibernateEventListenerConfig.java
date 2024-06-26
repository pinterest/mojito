package com.box.l10n.mojito.service.eventlistener;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Registers hibernate event listeners.
 *
 * @author jyi
 */
@Component
public class HibernateEventListenerConfig {

  @PersistenceUnit EntityManagerFactory emf;

  @Autowired EntityCrudEventListener entityCrudEventListener;

  @PostConstruct
  public void registerListeners() {
    SessionFactoryImpl sessionFactory = emf.unwrap(SessionFactoryImpl.class);
    EventListenerRegistry registry =
        (EventListenerRegistry)
            sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
    registry
        .getEventListenerGroup(EventType.POST_COMMIT_INSERT)
        .appendListener(entityCrudEventListener);
    registry
        .getEventListenerGroup(EventType.POST_COMMIT_UPDATE)
        .appendListener(entityCrudEventListener);
    registry
        .getEventListenerGroup(EventType.POST_COMMIT_DELETE)
        .appendListener(entityCrudEventListener);
  }
}
