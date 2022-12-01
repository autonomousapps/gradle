/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.api.internal.tasks.scala;

import org.gradle.internal.Factory;
import org.gradle.internal.jvm.Jvm;
import org.gradle.jvm.toolchain.JavaInstallationMetadata;

import java.io.File;
import java.util.Objects;

public class DefaultScalaJavaJointCompileSpecFactory implements Factory<DefaultScalaJavaJointCompileSpec> {

    private final JavaInstallationMetadata toolchain;

    public DefaultScalaJavaJointCompileSpecFactory(JavaInstallationMetadata toolchain) {
        Objects.requireNonNull(toolchain, "Toolchain is required for Scala compilation");
        this.toolchain = toolchain;
    }

    @Override
    public DefaultScalaJavaJointCompileSpec create() {
        File toolchainJavaHome = toolchain.getInstallationPath().getAsFile();
        return new DefaultScalaJavaJointCompileSpec(Jvm.forHome(toolchainJavaHome).getJavaExecutable());
    }
}
