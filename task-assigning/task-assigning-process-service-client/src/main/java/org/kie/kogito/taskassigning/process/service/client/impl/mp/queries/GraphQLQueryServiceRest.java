package org.kie.kogito.taskassigning.process.service.client.impl.mp.queries;

import java.io.Closeable;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public interface GraphQLQueryServiceRest extends Closeable {

    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    ObjectNode executeQuery(String query);
}
