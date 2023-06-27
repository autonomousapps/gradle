/*
 * Copyright 2017 the original author or authors.
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
package org.gradle.plugins.ide.internal.resolver;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import org.gradle.api.artifacts.ArtifactCollection;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ComponentSelector;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.UnresolvedDependencyResult;
import org.gradle.api.attributes.DocsType;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.specs.Spec;
import org.gradle.api.specs.Specs;
import org.gradle.internal.jvm.JavaModuleDetector;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.gradle.api.internal.artifacts.dsl.dependencies.DependencyFactoryInternal.ClassPathNotation.GRADLE_API;
import static org.gradle.api.internal.artifacts.dsl.dependencies.DependencyFactoryInternal.ClassPathNotation.GRADLE_TEST_KIT;
import static org.gradle.api.internal.artifacts.dsl.dependencies.DependencyFactoryInternal.ClassPathNotation.LOCAL_GROOVY;

/**
 * Adapts Gradle's dependency resolution engine to the special needs of the IDE plugins.
 * Allows adding and subtracting {@link Configuration}s, working in offline mode and downloading sources/javadoc.
 */
public class IdeDependencySet {
    private final ObjectFactory objectFactory;
    private final JavaModuleDetector javaModuleDetector;
    private final Collection<Configuration> plusConfigurations;
    private final Collection<Configuration> minusConfigurations;
    private final boolean inferModulePath;
    private final GradleApiSourcesResolver gradleApiSourcesResolver;
    private final Collection<Configuration> testConfigurations;

    public IdeDependencySet(ObjectFactory objectFactory, JavaModuleDetector javaModuleDetector, Collection<Configuration> plusConfigurations, Collection<Configuration> minusConfigurations, boolean inferModulePath, GradleApiSourcesResolver gradleApiSourcesResolver) {
        this(objectFactory, javaModuleDetector, plusConfigurations, minusConfigurations, inferModulePath, gradleApiSourcesResolver, Collections.emptySet());
    }

    public IdeDependencySet(ObjectFactory objectFactory, JavaModuleDetector javaModuleDetector, Collection<Configuration> plusConfigurations, Collection<Configuration> minusConfigurations, boolean inferModulePath, GradleApiSourcesResolver gradleApiSourcesResolver, Collection<Configuration> testConfigurations) {
        this.objectFactory = objectFactory;
        this.javaModuleDetector = javaModuleDetector;
        this.plusConfigurations = plusConfigurations;
        this.minusConfigurations = minusConfigurations;
        this.inferModulePath = inferModulePath;
        this.gradleApiSourcesResolver = gradleApiSourcesResolver;
        this.testConfigurations = testConfigurations;
    }

    public void visit(IdeDependencyVisitor visitor) {
        if (plusConfigurations.isEmpty()) {
            return;
        }
        new IdeDependencyResult().visit(visitor);
    }

    /*
     * Tries to minimize the number of requests to the resolution engine by batching up requests
     * for sources/javadoc.
     *
     * There is still some inefficiency because the ArtifactCollection interface does not provide
     * detailed failure results, so we have to fall back to the more expensive ResolutionResult API.
     * We should fix this, as other IDE vendors will face the same problem.
     */
    private class IdeDependencyResult {
        private final Map<ComponentArtifactIdentifier, ResolvedArtifactResult> resolvedArtifacts = Maps.newLinkedHashMap();
        private final SetMultimap<ComponentArtifactIdentifier, Configuration> configurations = MultimapBuilder.hashKeys().linkedHashSetValues().build();
        private final Map<ComponentSelector, UnresolvedDependencyResult> unresolvedDependencies = Maps.newLinkedHashMap();
        private final SetMultimap<ComponentArtifactIdentifier, ResolvedArtifactResult> sources = MultimapBuilder.hashKeys().linkedHashSetValues().build();
        private final SetMultimap<ComponentArtifactIdentifier, ResolvedArtifactResult> javadoc = MultimapBuilder.hashKeys().linkedHashSetValues().build();

        public void visit(IdeDependencyVisitor visitor) {
            resolvePlusConfigurations(visitor);
            resolveMinusConfigurations(visitor);
            visitArtifacts(visitor);
            visitUnresolvedDependencies(visitor);
        }

        private void resolvePlusConfigurations(IdeDependencyVisitor visitor) {
            for (Configuration configuration : plusConfigurations) {
                ArtifactCollection artifacts = getResolvedArtifacts(configuration, visitor.isOffline());
                for (ResolvedArtifactResult resolvedArtifact : artifacts) {
                    resolvedArtifacts.put(resolvedArtifact.getId(), resolvedArtifact);
                    configurations.put(resolvedArtifact.getId(), configuration);
                }

                if (!visitor.isOffline() && !artifacts.getFailures().isEmpty()) {
                    for (UnresolvedDependencyResult unresolvedDependency : getUnresolvedDependencies(configuration)) {
                        unresolvedDependencies.put(unresolvedDependency.getAttempted(), unresolvedDependency);
                    }
                }

                if (!visitor.isOffline() && visitor.downloadSources()) {
                    for (ResolvedArtifactResult sourcesArtifact : getSources(configuration)) {
                        sources.put(sourcesArtifact.getId(), sourcesArtifact);
                    }
                }

                if (!visitor.isOffline() && visitor.downloadJavaDoc()) {
                    for (ResolvedArtifactResult javadocArtifact : getJavadoc(configuration)) {
                        javadoc.put(javadocArtifact.getId(), javadocArtifact);
                    }
                }
            }
        }

        private void resolveMinusConfigurations(IdeDependencyVisitor visitor) {
            for (Configuration configuration : minusConfigurations) {
                ArtifactCollection artifacts = getResolvedArtifacts(configuration, visitor.isOffline());
                for (ResolvedArtifactResult resolvedArtifact : artifacts) {
                    resolvedArtifacts.remove(resolvedArtifact.getId());
                    configurations.removeAll(resolvedArtifact.getId());
                }

                if (!visitor.isOffline() && !artifacts.getFailures().isEmpty()) {
                    for (UnresolvedDependencyResult unresolvedDependency : getUnresolvedDependencies(configuration)) {
                        unresolvedDependencies.remove(unresolvedDependency.getAttempted());
                    }
                }

                if (!visitor.isOffline() && visitor.downloadSources()) {
                    for (ResolvedArtifactResult sourcesArtifact : getSources(configuration)) {
                        sources.removeAll(sourcesArtifact.getId());
                    }
                }

                if (!visitor.isOffline() && visitor.downloadJavaDoc()) {
                    for (ResolvedArtifactResult javadocArtifact : getJavadoc(configuration)) {
                        javadoc.removeAll(javadocArtifact.getId());
                    }
                }
            }
        }

        private ArtifactCollection getResolvedArtifacts(Configuration configuration, boolean isOffline) {
            return configuration.getIncoming().artifactView(view -> {
                view.lenient(true);
                view.componentFilter(getComponentFilter(isOffline));
            }).getArtifacts();
        }

        private Spec<ComponentIdentifier> getComponentFilter(boolean isOffline) {
            if (isOffline) {
                return id -> !(id instanceof ModuleComponentIdentifier);
            } else {
                return Specs.satisfyAll();
            }
        }

        private ArtifactCollection getSources(Configuration configuration) {
            return configuration.getIncoming().artifactView(view -> {
                view.lenient(true);
                view.withVariantReselection();
                view.attributes(attrs -> {
                    attrs.attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objectFactory.named(DocsType.class, DocsType.SOURCES));
                });
            }).getArtifacts();
        }

        private ArtifactCollection getJavadoc(Configuration configuration) {
            return configuration.getIncoming().artifactView(view -> {
                view.lenient(true);
                view.withVariantReselection();
                view.attributes(attrs -> {
                    attrs.attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objectFactory.named(DocsType.class, DocsType.JAVADOC));
                });
            }).getArtifacts();
        }

        private Iterable<UnresolvedDependencyResult> getUnresolvedDependencies(Configuration configuration) {
            return Iterables.filter(configuration.getIncoming().getResolutionResult().getRoot().getDependencies(), UnresolvedDependencyResult.class);
        }

        private void visitArtifacts(IdeDependencyVisitor visitor) {

            SetMultimap<ComponentIdentifier, ResolvedArtifactResult> sourcesByComponent = sortByComponent(sources);
            SetMultimap<ComponentIdentifier, ResolvedArtifactResult> javadocByComponent = sortByComponent(javadoc);

            for (ResolvedArtifactResult artifact : resolvedArtifacts.values()) {
                ComponentIdentifier componentIdentifier = artifact.getId().getComponentIdentifier();
                boolean testOnly = isTestConfiguration(configurations.get(artifact.getId()));
                boolean asModule = isModule(testOnly, artifact.getFile());
                if (componentIdentifier instanceof ProjectComponentIdentifier) {
                    visitor.visitProjectDependency(artifact, testOnly, asModule);
                } else {
                    if (componentIdentifier instanceof ModuleComponentIdentifier) {
                        Set<ResolvedArtifactResult> sources = sourcesByComponent.get(componentIdentifier);
                        Set<ResolvedArtifactResult> javadoc = javadocByComponent.get(componentIdentifier);
                        visitor.visitModuleDependency(artifact, sources, javadoc, testOnly, asModule);
                    } else if (isLocalGroovyDependency(artifact)) {
                        File localGroovySources = shouldDownloadSources(visitor) ? gradleApiSourcesResolver.resolveLocalGroovySources(artifact.getFile().getName()) : null;
                        visitor.visitGradleApiDependency(artifact, localGroovySources, testOnly);
                    } else {
                        visitor.visitFileDependency(artifact, testOnly);
                    }
                }
            }
        }

        private SetMultimap<ComponentIdentifier, ResolvedArtifactResult> sortByComponent(
            SetMultimap<ComponentArtifactIdentifier, ResolvedArtifactResult> artifacts
        ) {
            ImmutableSetMultimap.Builder<ComponentIdentifier, ResolvedArtifactResult> result = ImmutableSetMultimap.builder();
            for (Map.Entry<ComponentArtifactIdentifier, ResolvedArtifactResult> entry : artifacts.entries()) {
                result.put(entry.getKey().getComponentIdentifier(), entry.getValue());
            }
            return result.build();
        }

        private boolean isModule(boolean testOnly, File artifact) {
            // Test code is not treated as modules, as Eclipse does not support compiling two modules in one project anyway.
            // See also: https://bugs.eclipse.org/bugs/show_bug.cgi?id=520667
            //
            // We assume that a test-only dependency is not a module, which corresponds to how Eclipse does test running for modules:
            // It patches the main module with the tests and expects test dependencies to be part of the unnamed module (classpath).
            return javaModuleDetector.isModule(inferModulePath && !testOnly, artifact);
        }

        private boolean isLocalGroovyDependency(ResolvedArtifactResult artifact) {
            String artifactFileName = artifact.getFile().getName();
            String componentIdentifier = artifact.getId().getComponentIdentifier().getDisplayName();
            return (componentIdentifier.equals(GRADLE_API.displayName)
                    || componentIdentifier.equals(GRADLE_TEST_KIT.displayName)
                    || componentIdentifier.equals(LOCAL_GROOVY.displayName))
                && artifactFileName.startsWith("groovy-");
        }

        private boolean shouldDownloadSources(IdeDependencyVisitor visitor) {
            return !visitor.isOffline() && visitor.downloadSources();
        }

        private boolean isTestConfiguration(Set<Configuration> configurations) {
            return testConfigurations.containsAll(configurations);
        }

        private void visitUnresolvedDependencies(IdeDependencyVisitor visitor) {
            for (UnresolvedDependencyResult unresolvedDependency : unresolvedDependencies.values()) {
                visitor.visitUnresolvedDependency(unresolvedDependency);
            }
        }
    }

}
