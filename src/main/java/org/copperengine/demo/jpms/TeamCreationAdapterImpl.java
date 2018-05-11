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

import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.*;
import org.copperengine.core.Acknowledge;
import org.copperengine.core.ProcessingEngine;

import static org.asynchttpclient.Dsl.*;

public class TeamCreationAdapterImpl implements TeamCreationAdapter {
    private static final AtomicLong correlationIdCounter = new AtomicLong();

    private final AsyncHttpClient client = asyncHttpClient(config());
    private ProcessingEngine engine;

    private class PersonHandler extends AsyncCompletionHandler<Person> {
        private final String correlationId;

        private PersonHandler(String correlationId) {
            this.correlationId = correlationId;
        }

        @Override
        public Person onCompleted(Response response) {
            Person leader = null;
            Exception exc = null;
            try {
                leader = getPerson(response);
            } catch (Exception e) {
                exc = e;
            }
            var copperResp = new org.copperengine.core.Response<>(correlationId, leader, exc);
            var ack = new Acknowledge.DefaultAcknowledge();
            engine.notify(copperResp, ack);
            ack.waitForAcknowledge();
            return leader;
        }
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

    private String asyncCreatePerson(String url) {
        String correlationId = createCorrelationId();
        client.prepareGet(url).execute(new PersonHandler(correlationId));
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
