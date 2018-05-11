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
package org.copperengine.demo.jpms.workflow;

import org.copperengine.core.*;
import org.copperengine.demo.jpms.Person;
import org.copperengine.demo.jpms.TeamCreationAdapter;
import org.copperengine.demo.jpms.TeamCreationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@WorkflowDescription(alias = "TeamCreationWorkFlow", majorVersion = 1, minorVersion = 0, patchLevelVersion = 0)
public class TeamCreationWorkflow extends Workflow<TeamCreationRequest> {
    private static final Logger logger = LoggerFactory.getLogger(TeamCreationWorkflow.class);

    private transient TeamCreationAdapter adapter;

    @AutoWire
    public void setAdapter(TeamCreationAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void main() throws Interrupt {
        // trigger the creation of the team leader
        var leaderCorrelationId = adapter.asyncCreateLeader(getData().isFemaleLeader());

        // wait asynchronously for the team leader to be created
        wait(WaitMode.ALL, 60, TimeUnit.SECONDS, leaderCorrelationId);

        // retrieve the team leader
        Response<Person> leaderResponse = getAndRemoveResponse(leaderCorrelationId);
        var leader = fromResponse(leaderResponse, "leader", leaderCorrelationId);
        if(leader == null) return;

        // best practice: set variables that are no longer needed to null in order to reduce the footprint
        leaderCorrelationId = null;
        leaderResponse = null;

        // trigger the creation of all team members
        int teamSize = getData().getTeamSize();
        var memberCorrelationIds = new String[teamSize];
        for(int i=0; i < teamSize; i++) {
            memberCorrelationIds[i] = adapter.asyncCreateTeamMember(leader);
        }

        // wait asynchronously for all team members to be created
        wait(WaitMode.ALL, 60, TimeUnit.SECONDS, memberCorrelationIds);

        // retrieve all team members
        var members = new ArrayList<Person>();
        for(int i=0; i < teamSize; i++) {
            Response<Person> memberResponse = getAndRemoveResponse(memberCorrelationIds[i]);
            var member = fromResponse(memberResponse, "member", memberCorrelationIds[i]);
            if(member != null) {
                members.add(member);
            }
        }

        // display the created team
        if(!members.isEmpty()) {
            logger.info("Team of {} from {}: {}", leader.getFullName(), leader.getLocation(),
                    members.stream().map(Person::getFullName).collect(Collectors.joining(", ")));
        }
    }

    private Person fromResponse(Response<Person> response, String role, String correlationId) {
        if(response.isTimeout()) {
            logger.debug("Timeout for team {} with correlationId: {}", role, correlationId);
            return null;
        }
        if(response.getException() != null) {
            logger.debug("Failed to create team {} with correlationId {}: {}", role, correlationId, response.getException().getMessage());
            return null;
        }
        return response.getResponse();
    }
}
