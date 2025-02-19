/*
 * Copyright 2023 the original author or authors.
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

package org.gradle.tooling.events.problems;


import org.gradle.api.Incubating;

import java.util.List;

/**
 * Represents a list of aggregated problems. These are sent at the end of the build.
 * All Problems that occurred more than once during the build are aggregated and sent as a {@link ProblemAggregation}.
 * They won't be sent in between the build only the first one.
 * The aggregation happens based on the {@link ProblemCategory} and {@link Label}.
 * Therefore, the {@link ProblemCategory} and {@link Label} are the same for all aggregated problems and are also present in the this aggregation interface.
 *
 * @since 8.6
 */
@Incubating
public interface ProblemAggregation {

    /**
     * Returns the problem category that is common for all aggregated problems.
     *
     * @return the problem category
     * @since 8.6
     */
    ProblemCategory getCategory();

    /**
     * Returns the problem label that is common for all aggregated problems.
     *
     * @return the problem label
     * @since 8.6
     */
    Label getLabel();

    /**
     * The list of aggregated problems.
     *
     * @return The list of aggregated problems.
     * @since 8.6
     */
    List<ProblemDescriptor> getProblemDescriptors();

}
