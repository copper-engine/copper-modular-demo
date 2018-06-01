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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Response;
import org.copperengine.core.Acknowledge;
import org.copperengine.core.ProcessingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.asynchttpclient.Dsl.asyncHttpClient;
import static org.asynchttpclient.Dsl.config;

public class TeamCreationAdapterImpl implements TeamCreationAdapter {
    private static final Logger logger = LoggerFactory.getLogger(TeamCreationAdapterImpl.class);
    private static final AtomicLong correlationIdCounter = new AtomicLong();

    private final long delayMillis;
    private long lastScheduledStart;
    private ScheduledExecutorService svc = Executors.newScheduledThreadPool(1000);

    private final AsyncHttpClient client = asyncHttpClient(config());
    private ProcessingEngine engine;

    private class PersonHandler extends AsyncCompletionHandler<Person> {
        private final String correlationId;

        private PersonHandler(String correlationId) {
            this.correlationId = correlationId;
        }

        @Override
        public Person onCompleted(Response response) {
            Person person = null;
            Exception exc = null;
            try {
                person = getPerson(response);
            } catch (Exception e) {
                exc = e;
            }
            var copperResp = new org.copperengine.core.Response<>(correlationId, person, exc);
            var ack = new Acknowledge.DefaultAcknowledge();
            logger.debug("notifying: {}", person);
            engine.notify(copperResp, ack);
            ack.waitForAcknowledge();
            return person;
        }
    }

    public TeamCreationAdapterImpl(long delayMillis) {
        this.delayMillis = delayMillis;
    }

    public void setEngine(ProcessingEngine engine) {
        this.engine = engine;
    }

    @Override
    public String asyncCreateLeader(boolean female) {
        String url = "http://uinames.com/api/?gender=" + (female ? "female" : "male");
        return asyncCreatePerson(url);
    }

    @Override
    public String asyncCreateTeamMember(Person leader) {
        String url = "http://uinames.com/api/?region=" + leader.getLocation();
        return asyncCreatePerson(url);
    }

    private void schedule(Runnable action) {
        long delay;
        synchronized (this) {
            long currTime = System.currentTimeMillis();
            lastScheduledStart = (lastScheduledStart == 0) ? currTime : lastScheduledStart + delayMillis;
            delay = Math.max(0, lastScheduledStart - currTime);
        }
        logger.trace("Scheduling action with delay: {} ms.", delay);
        svc.schedule(action, delay, TimeUnit.MILLISECONDS);
    }

    private String asyncCreatePerson(String url) {
        String correlationId = createCorrelationId();
        schedule(() -> {
                logger.trace("Getting url: {}", url);
                client.prepareGet(url).execute(new PersonHandler(correlationId));
            }
        );
        return correlationId;
    }

    private static String createCorrelationId() {
        return "CORRELATION-ID-" + correlationIdCounter.incrementAndGet();
    }

    private static Person getPerson(Response response) throws Exception {
        if(response.getStatusCode() / 100 != 2) {
            throw new Exception("HTTP-" + response.getStatusCode() + ": " + response.getStatusText());
        }
        var node = new ObjectMapper().readTree(response.getResponseBody());
        String firstName = node.get("name").asText();
        String lastName = node.get("surname").asText();
        String gender = node.get("gender").asText();
        String location = node.get("region").asText();
        return new Person(firstName, lastName, location, "female".equals(gender));
    }
}
