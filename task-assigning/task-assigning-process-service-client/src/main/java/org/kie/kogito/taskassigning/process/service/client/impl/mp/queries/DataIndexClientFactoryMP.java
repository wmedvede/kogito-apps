package org.kie.kogito.taskassigning.process.service.client.impl.mp.queries;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.kie.kogito.taskassigning.process.service.client.AuthenticationCredentials;
import org.kie.kogito.taskassigning.process.service.client.ProcessServiceClientConfig;
import org.kie.kogito.taskassigning.process.service.client.impl.mp.AuthenticationFilterFactory;

@ApplicationScoped
public class DataIndexClientFactoryMP implements DataIndexClientFactory {

    private AuthenticationFilterFactory filterFactory;

    public DataIndexClientFactoryMP() {
        //CDI proxying
    }

    @Inject
    public DataIndexClientFactoryMP(AuthenticationFilterFactory filterFactory) {
        this.filterFactory = filterFactory;
    }

    @Override
    public DataIndexClient newClient(ProcessServiceClientConfig config, AuthenticationCredentials credentials) {
        URL url;
        try {
            url = new URL(config.getServiceUrl());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid serviceUrl: " + config.getServiceUrl(), e);
        }
        GraphQLQueryServiceRest graphQLServiceRest = RestClientBuilder.newBuilder()
                .baseUrl(url)
                .register(filterFactory.newAuthenticationFilter(credentials))
                .connectTimeout(config.getConnectTimeoutMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(config.getReadTimeoutMillis(), TimeUnit.MILLISECONDS)
                .build(GraphQLQueryServiceRest.class);
        return new DataIndexClientMP(graphQLServiceRest);
    }
}
