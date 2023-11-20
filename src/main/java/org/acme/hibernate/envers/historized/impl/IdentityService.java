package org.acme.hibernate.envers.historized.impl;

import java.util.Set;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@RequestScoped
@Named(IdentityService.HISTORIZED_IDENTITYSERVICE)
@Slf4j
public class IdentityService {

    public static final String HISTORIZED_IDENTITYSERVICE = "historized.identityservice";

    @Inject
    @Getter
    SecurityIdentity identity;

    /**
     * This static method allows to access client identity from non cdi beans.
     */
    public static SecurityIdentity get() {

        try {
            final BeanManager beanManager = CDI.current().getBeanManager();
            final Set<Bean<?>> bearerService = beanManager.getBeans(HISTORIZED_IDENTITYSERVICE);
            final Bean<?> bean = bearerService.toArray(new Bean<?>[0])[0];
            CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
            final IdentityService identityService = (IdentityService) beanManager.getReference(bean, IdentityService.class, ctx);

            return identityService.getIdentity();

        } catch (Exception e) {
            log.error("failed to get cdi bean: " + IdentityService.class.getName());
            throw e;
        }
    }

    /**
     * when this class is RequestScope, it will initialize a new instance once per request.
     */
    @PostConstruct
    void onCreate() {
        // nothing to do here
    }
}
