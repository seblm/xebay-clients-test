package fr.xebia.xebay.client;

import fr.xebia.xebay.domain.BidOffer;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.junit.rules.ExternalResource;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;

public class BidderTest extends ExternalResource {
    private static final String ADMIN_KEY = "4dm1n";

    protected SyncClient syncClient;
    protected WebTarget target;
    BidOffer currentOffer;

    @Override
    protected void before() {
        String hostAndPort = "localhost:8080";
        this.syncClient = new SyncClient(hostAndPort);
        try {
            currentOffer = syncClient.getCurrentOffer();
        } catch (ProcessingException e) {
            System.out.format("Please start a bid server on %s%n", hostAndPort);
            throw e;
        }
        this.target = ClientBuilder.newBuilder().register(JacksonFeature.class).property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true).build().target("http://" + hostAndPort + "/rest");
    }

    protected String register(String name) {
        return target.path("/users/register")
                .queryParam("name", name)
                .request()
                .header(AUTHORIZATION, ADMIN_KEY)
                .get(String.class);
    }

    protected void unregister(String apiKey) {
        target.path("/users/unregister")
                .queryParam("key", apiKey)
                .request()
                .header(AUTHORIZATION, ADMIN_KEY)
                .delete();
    }

    protected void bid(String apiKey) {
        syncClient.bid(currentOffer.getItem().getName(), currentOffer.getItem().getValue() * 1.2, apiKey);
    }

    protected void activate(String pluginName) {
        target.path("bidEngine/plugin").path(pluginName).path("activate").request()
                .header(AUTHORIZATION, ADMIN_KEY)
                .method("PATCH", Void.TYPE);
    }

    protected void deactivate(String pluginName) {
        target.path("bidEngine/plugin").path(pluginName).path("deactivate").request()
                .header(AUTHORIZATION, ADMIN_KEY)
                .method("PATCH", Void.TYPE);
    }
}
