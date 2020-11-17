#!/bin/bash
mvn clean compile quarkus:dev -Dquarkus.infinispan-client.use-auth=true -Dquarkus.infinispan-client.auth-username=myuser -Dquarkus.infinispan-client.auth-password=qwer1234! -Dkogito.protobuf.folder=`pwd`/PROTOS -Ddebug=5006
