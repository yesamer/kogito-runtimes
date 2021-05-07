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
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.drools.scenariosimulation.api.model.*;
import org.drools.scenariosimulation.backend.expression.ExpressionEvaluatorFactory;
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
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
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
            System.out.println(classPath);
            String base = classPath.substring(0, classPath.lastIndexOf("/"));
            System.out.println(base);
            String first = base.concat("/test-classes");
            String second = base.concat("/classes");
            System.out.println(first);
            System.out.println(second);
            asd = new ArrayList<>();
            asd.add(first);
        }

        asd.forEach(elem -> System.out.println("Discover in " + elem));
        System.out.println(asd.size());

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

        discoveryRequest.getSelectorsByType(ClassSelector.class).forEach(selector -> {
            if (selector.getClassName().equals("KogitoScenarioJunitActivatorTest")) {
                System.out.println("FOUND!!!!");
                //discoverTests(new File(System.getenv("test_dir")), engineDescriptor);
            }
        });

        String[] array = System.getProperty("java.class.path", ".").split(System.getProperty("path.separator"));
        List<String> asd = Arrays.asList(array);

        resourcesByExtension.map(this::parseFile)
                .forEach(scesim -> parentDescriptor.addChild(new KogitoTestScenarioDescriptor(uniqueId, scesim)));
        System.out.println(parentDescriptor.getChildren().size());
        return parentDescriptor;
    }

    @Override
    public void execute(ExecutionRequest executionRequest) {
        System.out.println("execute");
        TestDescriptor testDescriptorEngine = executionRequest.getRootTestDescriptor();
        EngineExecutionListener listener = executionRequest.getEngineExecutionListener();
        KogitoDMNScenarioRunnerHelper scenarioRunnerHelper = new KogitoDMNScenarioRunnerHelper();
        listener.executionStarted(testDescriptorEngine);
        TestExecutionResult state = TestExecutionResult.successful();
        for (TestDescriptor testDescriptor : executionRequest.getRootTestDescriptor().getChildren()) {
            listener.executionStarted(testDescriptor);

            ScenarioRunnerDTO scenarioRunnerDTO = ((KogitoTestScenarioDescriptor) testDescriptor).getScenarioRunnerDTO();
            ExpressionEvaluatorFactory expressionEvaluatorFactory = ExpressionEvaluatorFactory.create(
                    null,
                    scenarioRunnerDTO.getSettings().getType());
            ScesimModelDescriptor simulationModelDescriptor = scenarioRunnerDTO.getSimulationModelDescriptor();
            Settings settings = scenarioRunnerDTO.getSettings();
            Background background = scenarioRunnerDTO.getBackground();
            System.out.println("test descriptor " + testDescriptor.getDisplayName());
            for (ScenarioWithIndex scenarioWithIndex : scenarioRunnerDTO.getScenarioWithIndices()) {
                ScenarioRunnerData scenarioRunnerData = new ScenarioRunnerData();
                int index = scenarioWithIndex.getIndex();

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

            }
            listener.executionFinished(testDescriptor, TestExecutionResult.successful());

            System.out.println(testDescriptor.getDisplayName() + " Succcess");
        }
        listener.executionFinished(testDescriptorEngine, state);

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
