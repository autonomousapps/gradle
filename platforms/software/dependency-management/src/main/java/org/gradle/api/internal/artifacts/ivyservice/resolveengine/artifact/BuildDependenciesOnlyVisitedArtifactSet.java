/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact;

import org.gradle.api.artifacts.Dependency;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.results.VisitedGraphResults;
import org.gradle.api.internal.artifacts.transform.ArtifactVariantSelector;
import org.gradle.api.internal.tasks.TaskDependencyResolveContext;
import org.gradle.api.specs.Spec;

public class BuildDependenciesOnlyVisitedArtifactSet implements VisitedArtifactSet {
    private final VisitedGraphResults graphResults;
    private final VisitedArtifactResults artifactsResults;
    private final ArtifactVariantSelector artifactVariantSelector;

    public BuildDependenciesOnlyVisitedArtifactSet(
        VisitedGraphResults graphResults,
        VisitedArtifactResults artifactsResults,
        ArtifactVariantSelector artifactVariantSelector
    ) {
        this.graphResults = graphResults;
        this.artifactsResults = artifactsResults;
        this.artifactVariantSelector = artifactVariantSelector;
    }

    @Override
    public SelectedArtifactSet select(Spec<? super Dependency> dependencySpec, ArtifactSelectionSpec spec) {

        // When resolving build dependencies, we ignore the dependencySpec, potentially capturing a greater
        // set of build dependencies than actually required. This is because it takes a lot of extra information
        // from the visited graph to properly filter artifacts by dependencySpec, and we don't want capture that when
        // calculating build dependencies. Instead, we should just stop allowing users to filer by dependencySpec.

        ResolvedArtifactSet selectedArtifacts = artifactsResults.select(artifactVariantSelector, spec, false).getArtifacts();
        return new BuildDependenciesOnlySelectedArtifactSet(graphResults, selectedArtifacts);
    }

    private static class BuildDependenciesOnlySelectedArtifactSet implements SelectedArtifactSet {
        private final VisitedGraphResults graphResults;
        private final ResolvedArtifactSet selectedArtifacts;

        BuildDependenciesOnlySelectedArtifactSet(VisitedGraphResults graphResults, ResolvedArtifactSet selectedArtifacts) {
            this.graphResults = graphResults;
            this.selectedArtifacts = selectedArtifacts;
        }

        @Override
        public void visitDependencies(TaskDependencyResolveContext context) {
            graphResults.visitFailures(context::visitFailure);
            context.add(selectedArtifacts);
        }

        @Override
        public void visitArtifacts(ArtifactVisitor visitor, boolean continueOnSelectionFailure) {
            throw new UnsupportedOperationException("Artifacts have not been resolved.");
        }
    }
}
