/*
 * Copyright 2021 the original author or authors.
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

package org.gradle.testing.jacoco.plugins;

import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer;
import org.gradle.api.Incubating;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.artifacts.ResolvableConfiguration;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.TestSuiteType;
import org.gradle.api.attributes.VerificationType;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.jvm.JvmTestSuite;
import org.gradle.api.plugins.jvm.internal.JvmEcosystemUtilities;
import org.gradle.api.provider.Provider;
import org.gradle.api.reporting.ReportingExtension;
import org.gradle.internal.jacoco.DefaultJacocoCoverageReport;
import org.gradle.testing.base.TestSuite;
import org.gradle.testing.base.TestingExtension;
import org.gradle.testing.jacoco.tasks.JacocoReport;

import javax.inject.Inject;

/**
 * Adds configurations to for resolving variants containing JaCoCo code coverage results, which may span multiple subprojects.  Reacts to the presence of the jvm-test-suite plugin and creates
 * tasks to collect code coverage results for each named test-suite.
 *
 * @since 7.4
 * @see <a href="https://docs.gradle.org/current/userguide/jacoco_report_aggregation_plugin.html">JaCoCo Report Aggregation Plugin reference</a>
 */
@Incubating
public abstract class JacocoReportAggregationPlugin implements Plugin<Project> {

    public static final String JACOCO_AGGREGATION_CONFIGURATION_NAME = "jacocoAggregation";

    @Inject
    protected abstract JvmEcosystemUtilities getEcosystemUtilities();

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("org.gradle.reporting-base");
        project.getPluginManager().apply("jvm-ecosystem");
        project.getPluginManager().apply("jacoco");

        ConfigurationContainer configurations = project.getConfigurations();
        NamedDomainObjectProvider<DependencyScopeConfiguration> jacocoAggregation = configurations.dependencyScope(JACOCO_AGGREGATION_CONFIGURATION_NAME, conf -> {
            conf.setDescription("Collects project dependencies for purposes of JaCoCo coverage report aggregation");
            conf.setVisible(false);

            // convention for synthesizing reports based on existing test suites in "this" project
            project.getPlugins().withId("jvm-test-suite", plugin -> {
                // Depend on this project for aggregation
                conf.getDependencies().add(project.getDependencyFactory().create(project));
            });
        });

        ObjectFactory objects = project.getObjects();
        NamedDomainObjectProvider<ResolvableConfiguration> codeCoverageResultsConf = configurations.resolvable("aggregateCodeCoverageReportResults", conf -> {
            conf.setDescription("Graph needed for the aggregated JaCoCo coverage report.");
            conf.extendsFrom(jacocoAggregation.get());
            conf.setVisible(false);

            project.getPlugins().withType(JavaBasePlugin.class, plugin -> {
                // If the current project is jvm-based, aggregate dependent projects as jvm-based as well.
                getEcosystemUtilities().configureAsRuntimeClasspath(conf);
            });
        });

        Provider<FileCollection> sourceDirectories = codeCoverageResultsConf.map(conf -> conf.getIncoming().artifactView(view -> {
            view.withVariantReselection();
            view.componentFilter(id -> id instanceof ProjectComponentIdentifier);
            view.attributes(attributes -> {
                attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.class, Bundling.EXTERNAL));
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.VERIFICATION));
                attributes.attribute(VerificationType.VERIFICATION_TYPE_ATTRIBUTE, objects.named(VerificationType.class, VerificationType.MAIN_SOURCES));
            });
        }).getFiles());

        Provider<FileCollection> classDirectories = codeCoverageResultsConf.map(conf -> conf.getIncoming().artifactView(view -> {
            view.withVariantReselection();
            view.componentFilter(id -> id instanceof ProjectComponentIdentifier);
            view.attributes(attributes -> {
                attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.class, LibraryElements.CLASSES));
            });
        }).getFiles());

        ReportingExtension reporting = project.getExtensions().getByType(ReportingExtension.class);
        reporting.getReports().registerBinding(JacocoCoverageReport.class, DefaultJacocoCoverageReport.class);

        // Iterate and configure each user-specified report.
        reporting.getReports().withType(JacocoCoverageReport.class).all(report -> {
            report.getReportTask().configure(task -> {
                Provider<FileCollection> executionData = codeCoverageResultsConf.map(conf -> conf.getIncoming().artifactView(view -> {
                    view.withVariantReselection();
                    view.componentFilter(id -> id instanceof ProjectComponentIdentifier);
                    view.attributes(attributes -> {
                        attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.VERIFICATION));
                        attributes.attributeProvider(TestSuiteType.TEST_SUITE_TYPE_ATTRIBUTE, report.getTestType().map(tt -> objects.named(TestSuiteType.class, tt)));
                        attributes.attribute(VerificationType.VERIFICATION_TYPE_ATTRIBUTE, objects.named(VerificationType.class, VerificationType.JACOCO_RESULTS));
                        attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.BINARY_DATA_TYPE);
                    });
                }).getFiles());

                configureReportTaskInputs(task, classDirectories, sourceDirectories, executionData);
            });
        });

        // convention for synthesizing reports based on existing test suites in "this" project
        project.getPlugins().withId("jvm-test-suite", plugin -> {
            TestingExtension testing = project.getExtensions().getByType(TestingExtension.class);
            ExtensiblePolymorphicDomainObjectContainer<TestSuite> testSuites = testing.getSuites();

            testSuites.withType(JvmTestSuite.class).all(testSuite -> {
                reporting.getReports().create(testSuite.getName() + "CodeCoverageReport", JacocoCoverageReport.class, report -> {
                    report.getTestType().convention(testSuite.getTestType());
                });
            });
        });
    }

    private void configureReportTaskInputs(JacocoReport task, Provider<FileCollection> classDirectories, Provider<FileCollection> sourceDirectories, Provider<FileCollection> executionData) {
        task.getExecutionData().from(executionData);
        task.getClassDirectories().from(classDirectories);
        task.getSourceDirectories().from(sourceDirectories);
    }
}
