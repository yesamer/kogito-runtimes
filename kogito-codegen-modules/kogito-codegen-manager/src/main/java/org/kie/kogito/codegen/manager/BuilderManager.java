package org.kie.kogito.codegen.manager;

import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.drools.codegen.common.GeneratedFile;
import org.kie.kogito.KogitoGAV;
import org.kie.kogito.codegen.api.context.KogitoBuildContext;
import org.kie.kogito.codegen.manager.processes.PersistenceGenerationHelper;
import org.kie.kogito.codegen.manager.util.CodeGenManagerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.kogito.codegen.manager.CompilerHelper.RESOURCES;

public class BuilderManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuilderManager.class);

    public record BuildInfo(Set<URL> projectFilesUrls,
            Path projectBaseAbsolutePath, //MUST BE ABSOLUTE
            Path outputDirectory,
            String projectGroupId,
            String projectArtifactId,
            String projectVersion,
            String javaSourceEncoding,
            String javaSourceVersion,
            String javaTargetVersion,
            String jsonSchemaVersion,
            boolean generatePartial,
            boolean enablePersistence,
            List<String> runtimeClassPathElements,
            CodeGenManagerUtil.Framework framework) {
    }

    public static void build(BuildInfo buildInfo) {
        LOGGER.info("Building project: {}:{}:{}", buildInfo.projectGroupId(), buildInfo.projectArtifactId(), buildInfo.projectVersion());
        ClassLoader projectClassLoader = CodeGenManagerUtil.projectClassLoader(buildInfo.projectFilesUrls());
        KogitoGAV kogitoGAV = new KogitoGAV(buildInfo.projectGroupId(), buildInfo.projectArtifactId(), buildInfo.projectVersion());
        KogitoBuildContext kogitoBuildContext = CodeGenManagerUtil.discoverKogitoRuntimeContext(projectClassLoader, buildInfo.projectBaseAbsolutePath(), kogitoGAV,
                new CodeGenManagerUtil.ProjectParameters(buildInfo.framework(), "", "", "", "", buildInfo.enablePersistence()),
                className -> CodeGenManagerUtil.isClassNameInUrlClassLoader(buildInfo.projectFilesUrls().toArray(URL[]::new), className));
        /* Set System Property ? */
        Map<String, Collection<GeneratedFile>> generatedModelFiles = GenerateModelHelper.generateModelFiles(kogitoBuildContext, buildInfo.generatePartial());
        CompilerHelper.compileAndDumpGeneratedSources(generatedModelFiles.get(CompilerHelper.SOURCES), projectClassLoader, buildInfo.runtimeClassPathElements(), buildInfo.projectBaseAbsolutePath(),
                buildInfo.javaSourceEncoding(), buildInfo.javaSourceVersion(), buildInfo.javaTargetVersion());
        CompilerHelper.dumpResources(generatedModelFiles.get(RESOURCES), buildInfo.projectBaseAbsolutePath());
        Map<String, Collection<GeneratedFile>> persistenceFiles = PersistenceGenerationHelper.generatePersistenceFiles(kogitoBuildContext, projectClassLoader, buildInfo.jsonSchemaVersion());
        CompilerHelper.compileAndDumpGeneratedSources(persistenceFiles.get(CompilerHelper.SOURCES), projectClassLoader, buildInfo.runtimeClassPathElements(), buildInfo.projectBaseAbsolutePath(),
                buildInfo.javaSourceEncoding(), buildInfo.javaSourceVersion(), buildInfo.javaTargetVersion());
        CompilerHelper.dumpResources(persistenceFiles.get(RESOURCES), buildInfo.projectBaseAbsolutePath());
        // QUESTION Should we delete source Files?
        CodeGenManagerUtil.deleteDrlFiles(buildInfo.outputDirectory());
        LOGGER.info("Project build done");
    }

}
