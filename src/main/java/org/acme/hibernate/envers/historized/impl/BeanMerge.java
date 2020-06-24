package org.acme.hibernate.envers.historized.impl;

import lombok.extern.slf4j.Slf4j;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Slf4j
public final class BeanMerge {

    /**
     * copy all non-null values from overlay onto base without changing base!
     * actually, it copies all property values from base to overlay unless overlay has a value for the property already.
     *
     * @param base
     * @param overlay
     * @param <M>
     * @return
     */
    public static <M> M merge2On1(M base, M overlay) {
        assert null != base && null != overlay;

        log.info("bean.merge with base={}", base);
        log.info("bean.merge with overlay={}", overlay);

        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(base.getClass());

            // Iterate over all the attributes
            for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
                // Only copy writable attributes
                if (descriptor.getWriteMethod() != null) {
                    Method readMethod = descriptor.getReadMethod();
                    Object overlayValue = readMethod.invoke(
                            overlay);
                    if (overlayValue == null) {
                        // copy non-null base value onto overlay null-value property:
                        Object baseValue = readMethod.invoke(
                                base);
                        descriptor.getWriteMethod().invoke(overlay, baseValue);
                    }

                }
            }
        } catch (IntrospectionException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return overlay;
    }
}
