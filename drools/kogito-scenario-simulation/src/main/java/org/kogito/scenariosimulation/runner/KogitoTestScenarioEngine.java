/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kogito.scenariosimulation.runner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.drools.scenariosimulation.api.model.*;
import org.drools.scenariosimulation.api.utils.ConstantsHolder;
import org.drools.scenariosimulation.backend.expression.ExpressionEvaluatorFactory;
import org.drools.scenariosimulation.backend.runner.AbstractScenarioRunner;
import org.drools.scenariosimulation.backend.runner.ScenarioException;
import org.drools.scenariosimulation.backend.runner.model.ScenarioRunnerDTO;
import org.drools.scenariosimulation.backend.runner.model.ScenarioRunnerData;
import org.drools.scenariosimulation.backend.util.ScenarioSimulationXMLPersistence;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.drools.scenariosimulation.api.utils.ScenarioSimulationSharedUtils.FILE_EXTENSION;

public class KogitoTestScenarioEngine implements TestEngine {

    private static final ScenarioSimulationXMLPersistence xmlReader = ScenarioSimulationXMLPersistence.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(KogitoTestScenarioEngine.class);

    @Override
    public String getId() {
        return "kogito-test-scenario";
    }

    public static Stream<String> getResourcesByExtension(String extension) {
        String[] array = System.getProperty("java.class.path", ".").split(System.getProperty("path.separator"));
        List<String> asd = Arrays.asList(array);

        if (array.length == 1 && array[0].endsWith("-dev.jar")) {
            String classPath = array[0];
            //System.out.println(classPath);
            String base = classPath.substring(0, classPath.lastIndexOf("/"));
            //System.out.println(base);
            String first = base.concat("/test-classes");
            String second = base.concat("/classes");
            //System.out.println(first);
            //System.out.println(second);
            asd = new ArrayList<>();
            asd.add(first);
        }

        //asd.forEach(elem -> System.out.println("Discover in " + elem));
        //System.out.println(asd.size());

        return asd.stream().flatMap((elem) -> internalGetResources(elem, Pattern.compile(".*\\." + extension + "$")));
    }

    static Stream<String> internalGetResources(String path, Pattern pattern) {
        File file = new File(path);
        return !file.isDirectory() ? Stream.empty() : getResourcesFromDirectory(file, pattern);
    }

    public static Stream<String> getResourcesFromDirectory(File directory, Pattern pattern) {
        return directory != null && directory.listFiles() != null ? Arrays.stream(directory.listFiles()).flatMap((elem) -> {
            if (elem.isDirectory()) {
                return getResourcesFromDirectory(elem, pattern);
            } else {
                try {
                    String fileName = elem.getCanonicalPath();
                    if (pattern.matcher(fileName).matches()) {
                        return Stream.of(fileName);
                    }
                } catch (IOException var3) {
                    throw new ScenarioException("Impossible to access to resources", var3);
                }

                return Stream.empty();
            }
        }) : Stream.empty();
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
        EngineDescriptor parentDescriptor = new EngineDescriptor(uniqueId, "Kogito Test Scenario");

        Stream<String> resourcesByExtension = getResourcesByExtension(FILE_EXTENSION);

        resourcesByExtension.map(this::parseFile).forEach(scesim -> {
            String fileName = getScesimFileName(scesim.getFileName());
            //System.out.println("1-Filename:" + fileName);
            KogitoTestScenarioSuiteTestDescriptor suite = new KogitoTestScenarioSuiteTestDescriptor(uniqueId, fileName);
            parentDescriptor.addChild(suite);
            for (ScenarioWithIndex scenarioWithIndex : scesim.getScenarioWithIndices()) {
                int index = scenarioWithIndex.getIndex();
                //System.out.println("1=2-index:" + index);
                suite.addChild(new KogitoTestScenarioDescriptor(uniqueId, fileName, index, scesim, scenarioWithIndex));
            }
        });
        // System.out.println(parentDescriptor.getChildren().size());
        return parentDescriptor;
    }

    public static Description getDescriptionForSimulation(Optional<String> fullFileName, List<ScenarioWithIndex> scenarios) {
        String testSuiteName = fullFileName.isPresent() ? getScesimFileName(fullFileName.get()) : AbstractScenarioRunner.class.getSimpleName();
        Description suiteDescription = Description.createSuiteDescription(testSuiteName);
        scenarios.forEach(scenarioWithIndex -> suiteDescription.addChild(
                getDescriptionForScenario(fullFileName,
                        scenarioWithIndex.getIndex(),
                        scenarioWithIndex.getScesimData().getDescription())));
        return suiteDescription;
    }

    public static Description getDescriptionForScenario(Optional<String> fullFileName, int index, String description) {
        String testName = fullFileName.isPresent() ? getScesimFileName(fullFileName.get()) : AbstractScenarioRunner.class.getSimpleName();
        return Description.createTestDescription(testName,
                String.format("#%d: %s", index, description));
    }

    public static String getScesimFileName(String fileFullPath) {
        if (fileFullPath == null) {
            return null;
        }
        int idx = fileFullPath.replace("\\", "/").lastIndexOf('/');
        String fileName = idx >= 0 ? fileFullPath.substring(idx + 1) : fileFullPath;
        return fileName.endsWith(ConstantsHolder.SCESIM_EXTENSION) ? fileName.substring(0, fileName.lastIndexOf(ConstantsHolder.SCESIM_EXTENSION)) : fileName;
    }

    @Override
    public void execute(ExecutionRequest executionRequest) {
        //System.out.println("execute");
        TestDescriptor testDescriptorEngine = executionRequest.getRootTestDescriptor();
        EngineExecutionListener listener = executionRequest.getEngineExecutionListener();
        KogitoDMNScenarioRunnerHelper scenarioRunnerHelper = new KogitoDMNScenarioRunnerHelper();
        for (TestDescriptor testSuiteDescriptor : executionRequest.getRootTestDescriptor().getChildren()) {
            listener.executionStarted(testSuiteDescriptor);
            TestExecutionResult state = TestExecutionResult.successful();
            for (TestDescriptor testDescriptor : testSuiteDescriptor.getChildren()) {
                listener.executionStarted(testDescriptor);

                ScenarioRunnerDTO scenarioRunnerDTO = ((KogitoTestScenarioDescriptor) testDescriptor).getScenarioRunnerDTO();
                ExpressionEvaluatorFactory expressionEvaluatorFactory = ExpressionEvaluatorFactory.create(
                        null,
                        scenarioRunnerDTO.getSettings().getType());
                ScesimModelDescriptor simulationModelDescriptor = scenarioRunnerDTO.getSimulationModelDescriptor();
                Settings settings = scenarioRunnerDTO.getSettings();
                Background background = scenarioRunnerDTO.getBackground();
                //System.out.println("test descriptor " + testDescriptor.getDisplayName());
                ScenarioWithIndex scenarioWithIndex = ((KogitoTestScenarioDescriptor) testDescriptor).getScenarioWithIndex();
                ScenarioRunnerData scenarioRunnerData = new ScenarioRunnerData();

                try {
                    scenarioRunnerHelper.run(null,
                            simulationModelDescriptor,
                            scenarioWithIndex,
                            expressionEvaluatorFactory,
                            null,
                            scenarioRunnerData,
                            settings,
                            background);
                } catch (ScenarioException e) {
                    /*
                     * IndexedScenarioException indexedScenarioException = new IndexedScenarioException(index, e);
                     * indexedScenarioException.setFileName(scenarioRunnerDTO.getFileName());
                     */
                    //                    runNotifier.fireTestFailure(new Failure(descriptionForScenario, indexedScenarioException));
                    listener.executionFinished(testDescriptor, TestExecutionResult.failed(e));
                    //listener.executionFinished(testDescriptorEngine, TestExecutionResult.failed(e));

                    logger.error(e.getMessage(), e);
                } catch (Throwable e) {
                    /*
                     * IndexedScenarioException indexedScenarioException = new IndexedScenarioException(index, "Unexpected test error in scenario '" +
                     * scenarioWithIndex.getScesimData().getDescription() + "'", e);
                     * indexedScenarioException.setFileName(scenarioRunnerDTO.getFileName());
                     */
                    listener.executionFinished(testDescriptor, TestExecutionResult.failed(e));
                    //listener.executionFinished(testDescriptorEngine, TestExecutionResult.failed(e));
                    logger.error(e.getMessage(), e);
                    //                    runNotifier.fireTestFailure(new Failure(descriptionForScenario, indexedScenarioException));
                }

                listener.executionFinished(testDescriptor, TestExecutionResult.successful());

                //System.out.println(testDescriptor.getDisplayName() + " Succcess");
            }

            //System.out.println("Suite finished with " + state.toString());
            listener.executionFinished(testSuiteDescriptor, state);
        }

    }

    protected ScenarioRunnerDTO parseFile(String path) {
        try (final Scanner scanner = new Scanner(new File(path))) {
            String rawFile = scanner.useDelimiter("\\Z").next();
            ScenarioSimulationModel scenarioSimulationModel = xmlReader.unmarshal(rawFile);
            return new ScenarioRunnerDTO(scenarioSimulationModel, path);
        } catch (FileNotFoundException e) {
            throw new ScenarioException("File not found, this should not happen: " + path, e);
        } catch (Exception e) {
            throw new ScenarioException("Issue on parsing file: " + path, e);
        }
    }

}
