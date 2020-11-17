#!/bin/bash
#-Dkogito.auth.enabled=true
mvn clean compile quarkus:dev -Dquarkus.http.port=8480 -Dkogito.dataindex.http.url=http://localhost:8180/graphql
