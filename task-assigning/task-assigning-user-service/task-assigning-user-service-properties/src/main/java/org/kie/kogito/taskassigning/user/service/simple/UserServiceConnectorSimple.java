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

package org.kie.kogito.taskassigning.user.service.simple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.kie.kogito.taskassigning.user.service.User;
import org.kie.kogito.taskassigning.user.service.UserServiceConnector;

@ApplicationScoped
@Named("")
public class UserServiceConnectorSimple implements UserServiceConnector {

    private Map<String, User> users = new HashMap<>();

    public UserServiceConnectorSimple() {
        this.users = new HashMap<>(users);
    }

    @Override
    public void start() {
        if (System.getProperty("kieServerDataset") != null) {
            buildKieServerDataset();
        } else {
            buildKogitoDataset();
        }
        //TODO remove this line.
        System.out.println("UserServiceConnectorSimple was started!");
    }

    @Override
    public List<User> findAllUsers() {
        return new ArrayList<>(users.values());
    }

    @Override
    public User findUser(String id) {
        return users.get(id);
    }

    @Override
    public void destroy() {
        users.clear();
    }

    private void buildKogitoDataset() {
        users.put("john", new UserImpl("john",
                new HashSet<>(Arrays.asList(new GroupImpl("employees"))),
                new HashMap<>()));
        users.put("mary", new UserImpl("mary",
                new HashSet<>(Arrays.asList(new GroupImpl("managers"))),
                new HashMap<>()));
        users.put("poul", new UserImpl("poul",
                new HashSet<>(Arrays.asList(new GroupImpl("interns"),
                        new GroupImpl("managers"))),
                new HashMap<>()));

    }

    private void buildKieServerDataset() {
        users.put("krisv", new UserImpl("krisv",
                new HashSet<>(Arrays.asList(new GroupImpl("admin"),
                        new GroupImpl("analyst"),
                        new GroupImpl("user"))),
                new HashMap<>()));
        users.put("john", new UserImpl("john",
                new HashSet<>(Arrays.asList(new GroupImpl("analyst"),
                        new GroupImpl("Accounting"),
                        new GroupImpl("PM"))),
                new HashMap<>()));
        users.put("mary", new UserImpl("mary",
                new HashSet<>(Arrays.asList(new GroupImpl("analyst"),
                        new GroupImpl("HR"))),
                new HashMap<>()));
        users.put("sales-rep", new UserImpl("sales-rep",
                new HashSet<>(Arrays.asList(new GroupImpl("analyst"),
                        new GroupImpl("sales"))),
                new HashMap<>()));
        users.put("jack", new UserImpl("jack",
                new HashSet<>(Arrays.asList(new GroupImpl("analyst"),
                        new GroupImpl("IT"))),
                new HashMap<>()));

        users.put("katy", new UserImpl("katy",
                new HashSet<>(Arrays.asList(new GroupImpl("analyst"),
                        new GroupImpl("HR"))),
                new HashMap<>()));
        users.put("salaboy", new UserImpl("salaboy",
                new HashSet<>(Arrays.asList(new GroupImpl("admin"),
                        new GroupImpl("analyst"),
                        new GroupImpl("IT"),
                        new GroupImpl("HR"),
                        new GroupImpl("Accounting"))),
                new HashMap<>()));
        users.put("maciek", new UserImpl("maciek",
                new HashSet<>(Arrays.asList(new GroupImpl("admin"),
                        new GroupImpl("analyst"),
                        new GroupImpl("user"),
                        new GroupImpl("PM"),
                        new GroupImpl("HR"))),
                new HashMap<>()));

    }
}