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
package org.kie.kogito.codegen.manager;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.drools.codegen.common.GeneratedFile;
import org.kie.kogito.codegen.api.context.KogitoBuildContext;
import org.kie.kogito.codegen.core.ApplicationGenerator;
import org.kie.kogito.codegen.core.utils.ApplicationGeneratorDiscovery;
import org.kie.kogito.codegen.manager.processes.PersistenceGenerationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.drools.codegen.common.GeneratedFileType.COMPILED_CLASS;
import static org.kie.efesto.common.api.constants.Constants.INDEXFILE_DIRECTORY_PROPERTY;

public class GenerateModelHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateModelHelper.class);

    public static final String SOURCES = "SOURCES";
    public static final String RESOURCES = "RESOURCES";

    private GenerateModelHelper() {
    }

    public record GenerateModelInfo(
            ClassLoader projectClassLoader,
            KogitoBuildContext kogitoBuildContext,
            boolean onDemand,
            boolean generatePartial,
            Map<String, String> properties,
            File outputDirectory,
            List<String> runtimeClassPathElements,
            File baseDir,
            String schemaVersion,
            boolean keepSources) {

        public GenerateModelInfo(ClassLoader projectClassLoader, KogitoBuildContext kogitoBuildContext, BuilderManager.BuildInfo buildInfo) {
            this(projectClassLoader,
                    kogitoBuildContext,
                    buildInfo.onDemand(),
                    buildInfo.generatePartial(),
                    buildInfo.properties(),
                    buildInfo.outputDirectory().toFile(),
                    buildInfo.runtimeClassPathElements(),
                    buildInfo.projectBasePath().toFile(),
                    buildInfo.jsonSchemaVersion(),
                    buildInfo.keepSources());
        }
    }

    public record GenerateModelFilesInfo(KogitoBuildContext kogitoBuildContext,
            boolean generatePartial) {

        public GenerateModelFilesInfo(GenerateModelInfo generateModelInfo) {
            this(generateModelInfo.kogitoBuildContext,
                    generateModelInfo.generatePartial);
        }
    }

    public static void generateModel(GenerateModelInfo generateModelInfo) {
        Map<String, Collection<GeneratedFile>> generatedModelFiles;
        if (generateModelInfo.onDemand) {
            LOGGER.info("On-Demand Mode is On. Use mvn compile kogito:scaffold");
            generatedModelFiles = new HashMap<>();
        } else {
            generatedModelFiles = generateModelFiles(new GenerateModelFilesInfo(generateModelInfo));
        }
        if (generateModelInfo.outputDirectory == null) {
            throw new IllegalStateException("outputDirectory is null");
        }
        boolean indexFileDirectorySet = isIndexFileDirectorySet(generateModelInfo.outputDirectory);
        if (indexFileDirectorySet) {
            System.clearProperty(INDEXFILE_DIRECTORY_PROPERTY);
        }

        GeneratedFileManager.dumpGeneratedFiles(generatedModelFiles.get(SOURCES), generateModelInfo.baseDir().toPath());
        GeneratedFileManager.dumpGeneratedFiles(generatedModelFiles.get(RESOURCES), generateModelInfo.baseDir().toPath());

        Map<String, Collection<GeneratedFile>> generatedPersistenceFiles =
                PersistenceGenerationHelper.generatePersistenceFiles(
                        generateModelInfo.kogitoBuildContext, generateModelInfo.projectClassLoader,
                        generateModelInfo.schemaVersion);

        GeneratedFileManager.dumpGeneratedFiles(generatedPersistenceFiles.get(SOURCES), generateModelInfo.baseDir().toPath());
        GeneratedFileManager.dumpGeneratedFiles(generatedPersistenceFiles.get(RESOURCES), generateModelInfo.baseDir().toPath());

        if (!generateModelInfo.keepSources()) {
            GeneratedFileManager.deleteFilesByExtension(generateModelInfo.outputDirectory().toPath(), "drl");
        }
    }

    public static Map<String, Collection<GeneratedFile>> generateModelFiles(GenerateModelFilesInfo generateModelFilesInfo) {
        ApplicationGenerator appGen = ApplicationGeneratorDiscovery.discover(generateModelFilesInfo.kogitoBuildContext());

        Collection<GeneratedFile> generatedFiles;
        if (generateModelFilesInfo.generatePartial()) {
            generatedFiles = appGen.generateComponents();
        } else {
            generatedFiles = appGen.generate();
        }
        Collection<GeneratedFile> generatedClasses = new HashSet<>();
        Collection<GeneratedFile> generatedResources = new HashSet<>();
        generatedFiles.forEach(generatedFile -> {
            switch (generatedFile.category()) {
                case SOURCE -> generatedClasses.add(generatedFile);
                case INTERNAL_RESOURCE, STATIC_HTTP_RESOURCE -> generatedResources.add(generatedFile);
                case COMPILED_CLASS -> generatedResources.add(new GeneratedFile(COMPILED_CLASS, convertPath(generatedFile.path().toString()), generatedFile.contents()));
                default -> throw new IllegalStateException("Unexpected file with category: " + generatedFile.category());
            }
        });
        return Map.of(SOURCES, generatedClasses, RESOURCES, generatedResources);
    }

    static boolean isIndexFileDirectorySet(File outputDirectory) {
        boolean toReturn = false;
        if (System.getProperty(INDEXFILE_DIRECTORY_PROPERTY) == null) {
            System.setProperty(INDEXFILE_DIRECTORY_PROPERTY, outputDirectory.toString());
            toReturn = true;
        }
        return toReturn;
    }

    private static String convertPath(String toConvert) {
        return toConvert.replace('.', File.separatorChar) + ".class";
    }
}
