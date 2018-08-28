[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/copper-engine/copper-modular-demo/blob/master/LICENSE)
[![Build Status](https://img.shields.io/travis/copper-engine/copper-modular-demo/master.svg?label=Build)](https://travis-ci.org/copper-engine/copper-modular-demo)

## copper-modular-demo

A demo project that shows how to build a modular application with COPPER 5.0.
The Gradle script allows creating a custom runtime image of this application.  

The code also illustrates how COPPER 5.0 workflows support Java 10 features such as local-variable type inference
(look for the `var` keyword).

We tried to keep the application as simple as possible: no Spring, no persistent workflows, no SOAP services.
Have a look at [copper-starter](https://github.com/copper-engine/copper-starter) for an application using these features.

So, what is our application good for?
It creates project teams consisting of a leader and a few members.
To this end, it uses a recruiting service, which takes as input a list of constraints and
provides a person that satisfies them.

A person is characterized by name, gender, and location.
To keep things simple, we consider that each person offered by the recruiting service is qualified to be part of the team,
so we only need to impose constraints on gender and location.
For example, we can ask the service to find us a woman from Australia.
It's not mandatory to specify all constraints.
We could also ask for any person from Australia, any woman, or simply any person.  

We use [uinames.com](http://uinames.com) to emulate the recruiting service.

For each team to be created we specify its size and the gender of its leader.
Therefore, the workflow data contains two fields: `int teamSize`and `boolean femaleLeader`.

The application implements the following workflow:
- appoint a team leader with the gender implied by `femaleLeader`
- recruit (in parallel) `teamSize` members from the same region as the leader
- display the team

A COPPER workflow will be created and executed for each team to be created.
The number of workflows (teams) to be created can be configured in [application.properties](src/main/resources/application.properties).

The [uinames.com](http://uinames.com) server cannot handle a large number of requests simultaneously.
Under heavy load, it sends an error response or sometimes even no response at all.
For our demo application this is actually a good thing, because it allows us to test error and timeout scenarios.

The parameter `delayMillis` in [application.properties](src/main/resources/application.properties)
allows throttling the recruiting requests.
In this way, you can control the error frequency.
But please do not abuse the [uinames.com](http://uinames.com) server by running the application with low values of `delayMillis` for long periods!

To create the custom runtime image execute:

```
./gradlew jlink
```

After that, you will find the runtime image in the `build/copper-modular-demo-image` directory.

