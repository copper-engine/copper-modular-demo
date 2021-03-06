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
module org.copperengine.demo.jpms {
    requires org.copperengine.core;

    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires async.http.client;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires owner;
    requires java.management;

    exports org.copperengine.demo.jpms;
}
