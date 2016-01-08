/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.model.internal.manage.schema.extract;

import com.google.common.collect.Ordering;
import org.gradle.model.Managed;
import org.gradle.model.internal.manage.schema.ManagedImplStructSchema;
import org.gradle.model.internal.manage.schema.ModelProperty;
import org.gradle.model.internal.manage.schema.ModelSchema;
import org.gradle.model.internal.method.WeaklyTypeReferencingMethod;
import org.gradle.model.internal.type.ModelType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.gradle.model.internal.manage.schema.extract.ModelSchemaUtils.walkTypeHierarchy;

public class ManagedImplStructStrategy extends StructSchemaExtractionStrategySupport {

    protected ManagedImplStructStrategy(ModelSchemaAspectExtractor aspectExtractor) {
        super(aspectExtractor);
    }

    protected boolean isTarget(ModelType<?> type) {
        return type.isAnnotationPresent(Managed.class);
    }

    @Override
    protected <R> void validateTypeHierarchy(final ModelSchemaExtractionContext<R> extractionContext, ModelType<R> type) {
        walkTypeHierarchy(type.getConcreteClass(), new ModelSchemaUtils.TypeVisitor<R>() {
            @Override
            public void visitType(Class<? super R> type) {
                if (type.isAnnotationPresent(Managed.class)) {
                    validateManagedType(extractionContext, type);
                }
                validateType(extractionContext, type);
            }
        });
    }

    private void validateManagedType(ModelSchemaExtractionContext<?> extractionContext, Class<?> typeClass) {
        if (!typeClass.isInterface() && !Modifier.isAbstract(typeClass.getModifiers())) {
            extractionContext.add("Must be defined as an interface or an abstract class.");
        }

        if (typeClass.getTypeParameters().length > 0) {
            extractionContext.add("Cannot be a parameterized type.");
        }
    }

    private void validateType(ModelSchemaExtractionContext<?> extractionContext, Class<?> typeClass) {
        Constructor<?> customConstructor = findCustomConstructor(typeClass);
        if (customConstructor != null) {
            extractionContext.add(customConstructor, "Custom constructors are not supported.");
        }

        ensureNoInstanceScopedFields(extractionContext, typeClass);
        ensureNoProtectedOrPrivateMethods(extractionContext, typeClass);
    }

    private Constructor<?> findCustomConstructor(Class<?> typeClass) {
        Constructor<?>[] constructors = typeClass.getConstructors();
        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterTypes().length > 0) {
                return constructor;
            }
        }
        return null;
    }

    private void ensureNoInstanceScopedFields(ModelSchemaExtractionContext<?> extractionContext, Class<?> typeClass) {
        List<Field> declaredFields = Arrays.asList(typeClass.getDeclaredFields());
        for (Field field : declaredFields) {
            int fieldModifiers = field.getModifiers();
            if (!field.isSynthetic() && !(Modifier.isStatic(fieldModifiers) && Modifier.isFinal(fieldModifiers))) {
                extractionContext.add(field, "Fields must be static final.");
            }
        }
    }

    private void ensureNoProtectedOrPrivateMethods(ModelSchemaExtractionContext<?> extractionContext, Class<?> typeClass) {
        // sort for determinism
        Method[] methods = typeClass.getDeclaredMethods();
        Arrays.sort(methods, Ordering.usingToString());
        for (Method method : methods) {
            int modifiers = method.getModifiers();
            if (!method.isSynthetic() && !Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
                extractionContext.add(method, "Protected and private methods are not supported.");
            }
        }
    }

    @Override
    protected boolean selectProperty(ModelSchemaExtractionContext<?> context, ModelPropertyExtractionContext property) {
        return true;
    }

    @Override
    protected <R> ModelSchema<R> createSchema(ModelSchemaExtractionContext<R> extractionContext, Iterable<ModelProperty<?>> properties, Set<WeaklyTypeReferencingMethod<?, ?>> nonPropertyMethods, Iterable<ModelSchemaAspect> aspects) {
        return new ManagedImplStructSchema<R>(extractionContext.getType(), properties, nonPropertyMethods, aspects);
    }
}
