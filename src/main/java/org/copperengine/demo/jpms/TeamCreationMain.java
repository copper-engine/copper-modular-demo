/*
 * Copyright 2018 SCOOP Software GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.copperengine.demo.jpms;

import org.copperengine.core.CopperException;
import org.copperengine.core.DependencyInjector;
import org.copperengine.core.common.DefaultTicketPoolManager;
import org.copperengine.core.common.SimpleJmxExporter;
import org.copperengine.core.common.TicketPool;
import org.copperengine.core.common.TicketPoolManager;
import org.copperengine.core.monitoring.LoggingStatisticCollector;
import org.copperengine.core.tranzient.TransientEngineFactory;
import org.copperengine.core.tranzient.TransientScottyEngine;
import org.copperengine.core.wfrepo.FileBasedWorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.Random;

public class TeamCreationMain {
    private static final Logger logger = LoggerFactory.getLogger(TeamCreationMain.class);
    private static int NUMBER_OF_WORKFLOWS = 200;

    private TransientScottyEngine engine;
    private final Random rnd = new Random();

	public static void main(String[] args) throws Exception {
	    long delayMillis = 50;
	    if(args.length > 0) {
	        delayMillis = Long.parseLong(args[0]);
        }
        new TeamCreationMain().runWithDependencyInjector(new DefaultDependencyInjector(delayMillis));
	    System.exit(0);
	}

	private void runWithDependencyInjector(DependencyInjector dependencyInjector) throws Exception {
		// create the processing engine; configure the directory containing workflow source files and the dependency injector
		var factory = new TransientEngineFactory() {
			@Override
			protected File getWorkflowSourceDirectory() {
				return new File("./src/workflow/java");
			}
            @Override
            protected DependencyInjector createDependencyInjector() {
                return dependencyInjector;
            }

            @Override
            protected TicketPoolManager createTicketPoolManager() {
                DefaultTicketPoolManager tpManager = new DefaultTicketPoolManager();
                tpManager.setTicketPools(Collections.singletonList(new TicketPool(DefaultTicketPoolManager.DEFAULT_POOL_ID, NUMBER_OF_WORKFLOWS)));
                return tpManager;
            }
        };

		//Startup the engine
		engine = factory.create();
        SimpleJmxExporter exporter = startJmxExporter();

        //start NUMBER_OF_WORKFLOWS workflows
        for (int i = 0; i < NUMBER_OF_WORKFLOWS; i++) {
            try {
                boolean femaleLeader = rnd.nextBoolean();
                int teamSize = 2 + rnd.nextInt(3);
                var request = new TeamCreationRequest(femaleLeader, teamSize);
                engine.run("TeamCreationWorkFlow", request);
            } catch (CopperException e) {
                logger.error("copper error: ", e);
            }
        }
        logger.info("Running {} workflows...", NUMBER_OF_WORKFLOWS);

        // wait for all workflow instances to finish
        int oldRemaining = NUMBER_OF_WORKFLOWS;
        int remaining = engine.getNumberOfWorkflowInstances();
        while(remaining > 0) {
            if(remaining != oldRemaining) {
                logger.info("{} workflows remaining...", remaining);
                oldRemaining = remaining;
            }
            Thread.sleep(1000);
            remaining = engine.getNumberOfWorkflowInstances();
        }

        exporter.shutdown();
        engine.shutdown();
    }

    private SimpleJmxExporter startJmxExporter() throws Exception {
        SimpleJmxExporter exporter = new SimpleJmxExporter();
        exporter.addProcessingEngineMXBean("team-creation-engine", engine);
        exporter.addWorkflowRepositoryMXBean("team-creation-workflow", (FileBasedWorkflowRepository)engine.getWfRepository());
        engine.getProcessorPools().forEach(pool -> exporter.addProcessorPoolMXBean(pool.getId(), pool));

        LoggingStatisticCollector statisticsCollector = new LoggingStatisticCollector();
        statisticsCollector.start();
        engine.setStatisticsCollector(statisticsCollector);
        exporter.addStatisticsCollectorMXBean("team-creation-statistics", statisticsCollector);

        exporter.startup();
        return exporter;
    }
}
