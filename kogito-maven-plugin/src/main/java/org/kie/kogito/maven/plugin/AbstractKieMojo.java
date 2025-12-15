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
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.kie.kogito.codegen.manager.BuilderManager;
import org.kie.kogito.codegen.manager.util.CodeGenManagerUtil;
import org.kie.kogito.maven.plugin.util.MojoUtil;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

public abstract class AbstractKieMojo extends AbstractMojo {

    /* This represents by default the target/classes directory. */
    @Parameter(required = true, defaultValue = "${project.build.outputDirectory}")
    protected Path projectBuildOutputDirectory;

    @Parameter
    protected Map<String, String> properties;

    @Parameter(property = "kogito.codegen.persistence")
    protected boolean persistence;

    @Parameter(property = "kogito.jsonSchema.version")
    protected String jsonSchemaVersion;

    @Parameter(property = "kogito.codegen.decisions")
    protected String generateDecisions;

    @Parameter(property = "kogito.codegen.predictions")
    protected String generatePredictions;

    @Parameter(property = "kogito.codegen.processes")
    protected String generateProcesses;

    @Parameter(property = "kogito.codegen.rules")
    protected String generateRules;
    /**
     * Partial generation can be used when reprocessing a pre-compiled project
     * for faster code-generation. It only generates code for rules and processes,
     * and does not generate extra meta-classes (etc. Application).
     * Use only when doing recompilation and for development purposes
     */
    @Parameter(property = "kogito.codegen.partial", defaultValue = "false")
    protected boolean generatePartial;

    @Parameter(property = "kogito.codegen.ondemand", defaultValue = "false")
    protected boolean onDemand;

    @Parameter(property = "kogito.sources.keep", defaultValue = "false")
    protected boolean keepSources;

    @Parameter(required = true, defaultValue = "${project}")
    protected MavenProject project;

    @Component
    private BuildPluginManager pluginManager;

    @Component
    private MavenProject mavenProject;

    @Component
    private MavenSession mavenSession;

    public void buildProject() throws MojoExecutionException {
        getLog().info("buildProject");
        executionLog();
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

            BuilderManager.build(buildInfo);

            mavenProject.addCompileSourceRoot(project.getBasedir().getAbsolutePath() + "/target/generated-sources/kogito");

            executeMojo(
                    plugin(
                            groupId("org.apache.maven.plugins"),
                            artifactId("maven-compiler-plugin"),
                            version("3.13.0")),
                    goal("compile"),
                    configuration(),
                    executionEnvironment(
                            mavenProject,
                            mavenSession,
                            pluginManager));

        } catch (DependencyResolutionRequiredException | IOException e) {
            throw new MojoExecutionException("Error building project", e);
        }
    }

    protected void executionLog() {
        getLog().info("=maven-kogito-plugin parameters ==========");
        getLog().info("Project base directory: " + project.getBasedir().toPath().toAbsolutePath());
        getLog().info("Build output directory: " + projectBuildOutputDirectory.toAbsolutePath());
        getLog().info("Persistence is enabled " + persistence);
        getLog().info("Json schema version: " + jsonSchemaVersion);
        getLog().info("===================================");
    }

    CodeGenManagerUtil.Framework discoverFramework() {
        if (MojoUtil.hasDependency(mavenProject, CodeGenManagerUtil.Framework.QUARKUS)) {
            return CodeGenManagerUtil.Framework.QUARKUS;
        }

        if (MojoUtil.hasDependency(mavenProject, CodeGenManagerUtil.Framework.SPRING)) {
            return CodeGenManagerUtil.Framework.SPRING;
        }

        return CodeGenManagerUtil.Framework.NONE;
    }

}
