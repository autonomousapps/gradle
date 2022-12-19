/*
 * Copyright 2019 the original author or authors.
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

package org.gradle.internal.service.scopes;

import org.gradle.cache.CacheRepository;
import org.gradle.cache.internal.CacheFactory;
import org.gradle.cache.internal.DefaultCacheRepository;
import org.gradle.cache.internal.scopes.DefaultCacheScopeMapping;
import org.gradle.initialization.layout.GlobalCacheDir;
import org.gradle.internal.hash.ClassLoaderHierarchyHasher;
import org.gradle.internal.isolation.IsolatableFactory;
import org.gradle.internal.service.ServiceRegistration;
import org.gradle.internal.snapshot.impl.DefaultIsolatableFactory;
import org.gradle.internal.state.ManagedFactoryRegistry;
import org.gradle.util.GradleVersion;

public class WorkerSharedUserHomeScopeServices {
    public void configure(ServiceRegistration registration) {
        registration.add(GlobalCacheDir.class);
    }

    CacheRepository createCacheRepository(GlobalCacheDir globalCacheDir, CacheFactory cacheFactory) {
        return new DefaultCacheRepository(new DefaultCacheScopeMapping(globalCacheDir.getDir(), GradleVersion.current()), cacheFactory);
    }

    IsolatableFactory createIsolatableFactory(
            ClassLoaderHierarchyHasher classLoaderHierarchyHasher,
            ManagedFactoryRegistry managedFactoryRegistry
    ) {
        return new DefaultIsolatableFactory(classLoaderHierarchyHasher, managedFactoryRegistry);
    }
}
