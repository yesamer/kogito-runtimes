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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import org.drools.scenariosimulation.api.model.ScenarioSimulationModel;
import org.drools.scenariosimulation.backend.runner.AbstractRunnerHelper;
import org.drools.scenariosimulation.backend.runner.RuleScenarioRunnerHelper;
import org.drools.scenariosimulation.backend.runner.TestScenarioEngine;
import org.kie.api.runtime.KieContainer;

public class KogitoTestScenarioEngine extends TestScenarioEngine {

    @Override
    public String getId() {
        return "KOGITO-TEST-ENGINE";
    }

    @Override
    protected KieContainer getKieContainer(ScenarioSimulationModel.Type type) {
        if (ScenarioSimulationModel.Type.RULE.equals(type)) {
            return super.getKieContainer(type);
        } else if (ScenarioSimulationModel.Type.DMN.equals(type)) {
            InvocationHandler nullHandler = (o, method, objects) -> null;
            return (KieContainer) Proxy.newProxyInstance(KieContainer.class.getClassLoader(),
                    new Class[] { KieContainer.class }, nullHandler);
        } else {
            throw new IllegalArgumentException("Impossible to run simulation of type " + type);
        }
    }

    @Override
    protected AbstractRunnerHelper getRunnerHelper(ScenarioSimulationModel.Type type) {
        if (ScenarioSimulationModel.Type.RULE.equals(type)) {
            return new RuleScenarioRunnerHelper();
        } else if (ScenarioSimulationModel.Type.DMN.equals(type)) {
            return new KogitoDMNScenarioRunnerHelper();
        } else {
            throw new IllegalArgumentException("Impossible to run simulation of type " + type);
        }
    }

}
