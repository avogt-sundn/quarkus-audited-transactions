package org.acme.hibernate.orm.envers;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

public final class BeanMerge {

    public static <M> void merge(M target, M source) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(target.getClass());
            // Iterate over all the attributes
            for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
                // Only copy writable attributes
                if (descriptor.getWriteMethod() != null) {
                    Object sourceValue = descriptor.getReadMethod().invoke(
                            source);
                    // Only copy values values where the destination values is null
                    if (sourceValue != null) {
                        descriptor.getWriteMethod().invoke(target, sourceValue);
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
    }
}
