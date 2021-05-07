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

import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;

public class KogitoTestScenarioSuiteTestDescriptor extends AbstractTestDescriptor {

    public KogitoTestScenarioSuiteTestDescriptor(UniqueId uniqueId, String fileName) {
        super(uniqueId.append("testscenario", fileName),
                fileName,
                ClassSource.from(KogitoTestScenarioEngine.class));
    }

    @Override
    public Type getType() {
        return Type.CONTAINER;
    }
}
