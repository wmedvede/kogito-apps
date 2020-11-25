package org.kie.kogito.taskassigning.process.service.client.graphql;

import javax.enterprise.context.ApplicationScoped;

import io.smallrye.graphql.client.typesafe.api.GraphQlClientBuilder;
import io.smallrye.graphql.client.typesafe.impl.GraphQlClientBuilderImpl;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.microprofile.client.impl.MpClientBuilderImpl;
import org.kie.kogito.taskassigning.process.service.client.BasicAuthenticationCredentials;
import org.kie.kogito.taskassigning.process.service.client.impl.mp.BasicAuthenticationFilter;

@ApplicationScoped
public class DataIndexQueryFactory {

    public DataIndexQueryFactory() {
        //CDI proxying
    }

    public UserTaskInstanceQuery newUserTaskInstanceQuery(String url, String user, String password) {
        GraphQlClientBuilderImpl builder = (GraphQlClientBuilderImpl) GraphQlClientBuilder.newBuilder();
        MpClientBuilderImpl mpClientBuilder = new MpClientBuilderImpl();

        ResteasyClient client = mpClientBuilder.build();
        client.register(new BasicAuthenticationFilter(BasicAuthenticationCredentials.newBuilder()
                                                              .user(user)
                                                              .password(password)
                                                              .build()));
        builder.client(client);

        return builder.endpoint(url)
                .build(UserTaskInstanceQuery.class);
    }
}
