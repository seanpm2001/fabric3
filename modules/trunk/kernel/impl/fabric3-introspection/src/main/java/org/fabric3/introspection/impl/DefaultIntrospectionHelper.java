/*
 * Fabric3
 * Copyright (C) 2009 Metaform Systems
 *
 * Fabric3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version, with the
 * following exception:
 *
 * Linking this software statically or dynamically with other
 * modules is making a combined work based on this software.
 * Thus, the terms and conditions of the GNU General Public
 * License cover the whole combination.
 *
 * As a special exception, the copyright holders of this software
 * give you permission to link this software with independent
 * modules to produce an executable, regardless of the license
 * terms of these independent modules, and to copy and distribute
 * the resulting executable under terms of your choice, provided
 * that you also meet, for each linked independent module, the
 * terms and conditions of the license of that module. An
 * independent module is a module which is not derived from or
 * based on this software. If you modify this software, you may
 * extend this exception to your version of the software, but
 * you are not obligated to do so. If you do not wish to do so,
 * delete this exception statement from your version.
 *
 * Fabric3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the
 * GNU General Public License along with Fabric3.
 * If not, see <http://www.gnu.org/licenses/>.
 *
 * ----------------------------------------------------
 *
 * Portions originally based on Apache Tuscany 2007
 * licensed under the Apache 2.0 license.
 *
 */
package org.fabric3.introspection.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;

import org.oasisopen.sca.ComponentContext;
import org.oasisopen.sca.RequestContext;
import org.oasisopen.sca.annotation.Callback;
import org.oasisopen.sca.annotation.Remotable;
import org.oasisopen.sca.annotation.Service;
import org.osoa.sca.ServiceReference;

import org.fabric3.model.type.component.ServiceDefinition;
import org.fabric3.model.type.java.InjectableAttributeType;
import org.fabric3.model.type.java.Signature;
import org.fabric3.model.type.service.DataType;
import org.fabric3.model.type.service.Operation;
import org.fabric3.spi.introspection.ImplementationNotFoundException;
import org.fabric3.spi.introspection.IntrospectionHelper;
import org.fabric3.spi.introspection.TypeMapping;

/**
 * @version $Rev$ $Date$
 */
public class DefaultIntrospectionHelper implements IntrospectionHelper {
    // the wrapper classes we understand and which all have a single type parameter
    private static final Set<Class<?>> WRAPPERS;

    static {
        WRAPPERS = new HashSet<Class<?>>();
        WRAPPERS.add(Collection.class);
        WRAPPERS.add(List.class);
        WRAPPERS.add(Queue.class);
        WRAPPERS.add(Set.class);
        WRAPPERS.add(SortedSet.class);
        WRAPPERS.add(ServiceReference.class);
        WRAPPERS.add(org.oasisopen.sca.ServiceReference.class);
    }

    public Class<?> loadClass(String name, ClassLoader cl) throws ImplementationNotFoundException {
        final Thread thread = Thread.currentThread();
        final ClassLoader oldCL = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(cl);
            return Class.forName(name, true, cl);
        } catch (ClassNotFoundException e) {
            throw new ImplementationNotFoundException(name, e);
        } catch (NoClassDefFoundError e) {
            // we trap this error as the most likely cause is a missing dependency on the classpath
            throw new ImplementationNotFoundException(name, e);
        } finally {
            thread.setContextClassLoader(oldCL);
        }
    }

    public String getSiteName(Field field, String override) {
        if (override != null && override.length() != 0) {
            return override;
        }
        return field.getName();
    }

    public String getSiteName(Method setter, String override) {
        if (override != null && override.length() != 0) {
            return override;
        }

        String name = setter.getName();
        if (name.length() > 3 && name.startsWith("set")) {
            return Character.toLowerCase(name.charAt(3)) + name.substring(4);
        } else {
            return name;
        }
    }

    public String getSiteName(Constructor<?> constructor, int index, String override) {
        if (override != null && override.length() != 0) {
            return override;
        }

        org.oasisopen.sca.annotation.Constructor oasisAnnotation = constructor.getAnnotation(org.oasisopen.sca.annotation.Constructor.class);
        if (oasisAnnotation != null) {
            String[] names = oasisAnnotation.value();
            if (names.length != 1 || names[0].length() != 0) {
                return names[index];
            }
        }
        org.osoa.sca.annotations.Constructor annotation = constructor.getAnnotation(org.osoa.sca.annotations.Constructor.class);
        if (annotation != null) {
            String[] names = annotation.value();
            if (names.length != 1 || names[0].length() != 0) {
                return names[index];
            }
        }
        return constructor.getDeclaringClass().getSimpleName() + "[" + index + ']';
    }

    public Type getGenericType(Method setter) {
        return getGenericType(setter, 0);
    }

    public Type getGenericType(Method method, int index) {
        return method.getGenericParameterTypes()[index];
    }

    public Type getGenericType(Constructor<?> constructor, int index) {
        return constructor.getGenericParameterTypes()[index];
    }

    public Type getBaseType(Type type, TypeMapping typeMapping) {
        if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            if (clazz.isArray()) {
                return clazz.getComponentType();
            } else if (WRAPPERS.contains(clazz) || Map.class.equals(clazz)) {
                return Object.class;
            } else {
                return clazz;
            }
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Class<?> clazz = (Class<?>) parameterizedType.getRawType();
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            if (WRAPPERS.contains(clazz)) {
                return typeMapping.getRawType(typeArguments[0]);
            } else if (Map.class.equals(clazz)) {
                return typeMapping.getRawType(typeArguments[1]);
            } else {
                return clazz;
            }

        } else if (type instanceof GenericArrayType) {
            GenericArrayType arrayType = (GenericArrayType) type;
            return typeMapping.getRawType(arrayType.getGenericComponentType());
        } else {
            throw new AssertionError("Unknown Type: " + type);
        }
    }

    public boolean isManyValued(TypeMapping typeMapping, Type type) {
        if (type instanceof GenericArrayType) {
            return true;
        } else {
            Class<?> clazz = typeMapping.getRawType(type);
            return clazz.isArray() || WRAPPERS.contains(clazz) || Map.class.equals(clazz);
        }
    }

    public InjectableAttributeType inferType(Type type, TypeMapping typeMapping) {
        Type baseType = getBaseType(type, typeMapping);
        Class<?> rawType = typeMapping.getRawType(baseType);

        // if it's not an interface, it must be a property
        if (!rawType.isInterface()) {
            return InjectableAttributeType.PROPERTY;
        }

        // it it's a context interfaces, it must be a context
        if (ComponentContext.class.isAssignableFrom(rawType)
                || org.osoa.sca.ComponentContext.class.isAssignableFrom(rawType)
                || RequestContext.class.isAssignableFrom(rawType)
                || org.osoa.sca.RequestContext.class.isAssignableFrom(rawType)) {
            return InjectableAttributeType.CONTEXT;
        }

        // if it's Remotable or a local Service, it must be a reference
        if (isAnnotationPresent(rawType, Remotable.class)
                || isAnnotationPresent(rawType, org.osoa.sca.annotations.Remotable.class)
                || isAnnotationPresent(rawType, Service.class)
                || isAnnotationPresent(rawType, org.osoa.sca.annotations.Service.class)) {
            return InjectableAttributeType.REFERENCE;
        }

        // if it has a Callback anotation, it's a calback
        if (isAnnotationPresent(rawType, Callback.class) || isAnnotationPresent(rawType, org.osoa.sca.annotations.Callback.class)) {
            return InjectableAttributeType.CALLBACK;
        }

        // otherwise it's a property
        return InjectableAttributeType.PROPERTY;
    }

    public boolean isAnnotationPresent(Class<?> type, Class<? extends Annotation> annotationType) {
        if (type.isAnnotationPresent(annotationType)) {
            return true;
        }
        Class<?>[] interfaces = type.getInterfaces();
        for (Class<?> superInterface : interfaces) {
            if (isAnnotationPresent(superInterface, annotationType)) {
                return true;
            }
        }
        return false;
    }

    public Set<Class<?>> getImplementedInterfaces(Class<?> type) {
        Set<Class<?>> interfaces = new HashSet<Class<?>>();
        while (type != null) {
            nextInterface:
            for (Class<?> current : (Class<?>[]) type.getInterfaces()) {
                for (Class<?> foundAlready : interfaces) {
                    if (current.isAssignableFrom(foundAlready)) {
                        continue nextInterface;
                    }
                }
                interfaces.add(current);
            }
            type = type.getSuperclass();
        }
        return interfaces;
    }

    public Set<Method> getInjectionMethods(Class<?> type, Collection<ServiceDefinition> services) {
        Set<Signature> exclude = getOperations(services);
        Set<Method> methods = new HashSet<Method>();
        while (type != null) {
            for (Method method : type.getDeclaredMethods()) {
                // check method accessibility
                int modifiers = method.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isAbstract(modifiers)) {
                    continue;
                }
                if (!(Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers))) {
                    continue;
                }
                if (!isSetter(method)) {
                    continue;
                }

                // exclude methods we have seen already (i.e. in a contract or overriden in a subclass)
                // we check using the signature as the method itself will have been declared on a different class
                Signature signature = new Signature(method);
                if (exclude.contains(signature)) {
                    continue;
                }

                exclude.add(signature);
                methods.add(method);
            }
            type = type.getSuperclass();
        }
        return methods;
    }

    private boolean isSetter(Method method) {
        // it must return void
        if (!Void.TYPE.equals(method.getReturnType())) {
            return false;
        }

        // it must have a single parameter
        if (method.getParameterTypes().length != 1) {
            return false;
        }

        // it's name must begin with "set" but not be "set"
        String name = method.getName();
        return !(name.length() < 4 || !name.startsWith("set"));

    }

    private Set<Signature> getOperations(Collection<ServiceDefinition> services) {
        Set<Signature> operations = new HashSet<Signature>();
        for (ServiceDefinition definition : services) {
            List<? extends Operation<?>> ops = definition.getServiceContract().getOperations();
            for (Operation<?> operation : ops) {
                String name = operation.getName();
                List<? extends DataType<?>> inputTypes = operation.getInputType().getLogical();
                List<String> paramTypes = new ArrayList<String>(inputTypes.size());
                for (DataType<?> inputType : inputTypes) {
                    paramTypes.add(((Class<?>) inputType.getPhysical()).getName());
                }
                operations.add(new Signature(name, paramTypes));
            }
        }
        return operations;
    }

    public Set<Field> getInjectionFields(Class<?> type) {
        Set<Field> fields = new HashSet<Field>();
        Set<String> exclude = new HashSet<String>();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                // check field accessibility
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
                    continue;
                }
                if (!(Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers))) {
                    continue;
                }

                // exclude fields already defined in a subclass
                String name = field.getName();
                if (exclude.contains(name)) {
                    continue;
                }
                exclude.add(name);
                fields.add(field);
            }

            type = type.getSuperclass();
        }
        return fields;
    }

    public TypeMapping mapTypeParameters(Class<?> type) {
        TypeMapping mapping = new TypeMapping();
        while (type != null) {
            addTypeBindings(mapping, type.getGenericSuperclass());
            for (Type interfaceType : type.getGenericInterfaces()) {
                addTypeBindings(mapping, interfaceType);
            }
            type = type.getSuperclass();
        }
        return mapping;
    }

    private void addTypeBindings(TypeMapping mapping, Type type1) {
        if (type1 instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) type1;
            Class<?> boundType = (Class<?>) type.getRawType();
            TypeVariable<? extends Class<?>>[] typeVariables = boundType.getTypeParameters();
            Type[] arguments = type.getActualTypeArguments();
            for (int i = 0; i < typeVariables.length; i++) {
                mapping.addMapping(typeVariables[i], arguments[i]);
            }
        }
    }
}
