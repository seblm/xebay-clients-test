package fr.xebia.xebay.client;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.junit.rules.ExternalResource;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;

public class BidderTest extends ExternalResource {
    private static final String ADMIN_KEY = "4dm1n";

    protected RestBidder restBidder;
    protected WebTarget target;

    @Override
    protected void before() {
        String target = "http://localhost:8080/rest";
        this.restBidder = new RestBidder(target);
        try {
            restBidder.getCurrentOffer();
        } catch (ProcessingException e) {
            System.out.format("Please start a bid server on %s%n", target);
            throw e;
        }
        this.target = ClientBuilder.newBuilder().register(JacksonFeature.class).build().target(target);
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

}
