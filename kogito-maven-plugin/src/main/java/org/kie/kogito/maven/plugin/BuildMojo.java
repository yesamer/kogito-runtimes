package org.kie.kogito.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "generateModel",
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        defaultPhase = LifecyclePhase.COMPILE,
        threadSafe = true)
public class BuildMojo extends AbstractKogitoMojo {

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Compiler Source Java Version: " + mavenCompilerSourceJavaVersion);
        getLog().info("Compiler Target Java Version: " + mavenCompilerTargetJavaVersion);
        getLog().info("Compiler Source Encoding: " + projectSourceEncoding);
        getLog().info("Project base directory: " + projectBaseDir.getAbsolutePath());
        getLog().info("Build output directory: " + projectBuildOutputDirectory);
        getLog().info("Partial generation is enabled: " + generatePartial);
        getLog().info("Json schema version: " + jsonSchemaVersion);
        getLog().info("===================================");

        buildProject();
    }
}
