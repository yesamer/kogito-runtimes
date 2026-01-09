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
 *
 */

package org.kie.kogito.maven.plugin;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.kie.kogito.codegen.manager.BuilderManager;
import org.kie.kogito.maven.plugin.util.MojoUtil;

@Mojo(name = "generateResources",
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        threadSafe = true)
public class GenerateResourcesMojo extends AbstractKieMojo {

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("execute");
        try {
            Set<URI> projectFilesUris = MojoUtil.getProjectFiles(mavenProject, null);
            BuilderManager.BuildInfo buildInfo = new BuilderManager.BuildInfo(
                    projectFilesUris,
                    project.getBasedir().toPath(),
                    projectBuildOutputDirectory,
                    mavenProject.getGroupId(),
                    mavenProject.getArtifactId(),
                    mavenProject.getVersion(),
                    jsonSchemaVersion,
                    generatePartial,
                    persistence,
                    onDemand,
                    keepSources,
                    mavenProject.getRuntimeClasspathElements(),
                    discoverFramework(),
                    properties);

            BuilderManager.processResources(buildInfo);

            //mavenProject.addCompileSourceRoot(project.getBasedir().getAbsolutePath() + "/target/generated-sources/kogito");

        } catch (DependencyResolutionRequiredException | IOException e) {
            throw new MojoExecutionException("Error building project", e);
        }
    }
}
