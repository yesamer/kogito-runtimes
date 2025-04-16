/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.kie.kogito.maven.plugin;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import static org.kie.kogito.codegen.manager.util.RunDebugUtil.debugProject;

@Mojo(name = "debug",
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
        threadSafe = true)
public class DebugMojo extends AbstractKogitoMojo {

    private static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.US);

    @Override
    public void execute() throws MojoExecutionException {
        Log log = getLog();
        log.info(mavenProject.toString());
        log.info(projectBuildOutputDirectory.toString());
        //log.info(mojoExecution.toString());
        // TODO FIX
        if (!projectBuildOutputDirectory.exists() || Objects.requireNonNull(projectBuildOutputDirectory.listFiles()).length == 0) {
            buildProject();
        }
        try {
            debugProject(mavenProject.getBasedir(), mavenProject.getBuild().getDirectory(), mavenProject.getBuild().getFinalName() + ".jar");
        } catch (IOException e) {
            throw new MojoExecutionException(e);
        }
    }

}
