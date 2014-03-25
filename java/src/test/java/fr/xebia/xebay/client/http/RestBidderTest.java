package fr.xebia.xebay.client.http;

import fr.xebia.xebay.domain.BidOffer;
import fr.xebia.xebay.domain.PublicUser;
import fr.xebia.xebay.domain.User;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.util.Set;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;

public class RestBidderTest {

    private static final String ADMIN_KEY = "4dm1n";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private RestBidder restBidder;
    private WebTarget target;

    @Before
    public void createRestBidder() {
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

    @Test
    public void should_register() {
        String apiKey = null;
        try {
            apiKey = register("email@provider.net");

            assertThat(apiKey).isNotNull();
        } finally {
            if (apiKey != null)
                unregister(apiKey);
        }
    }

    @Test
    public void should_unregister() {
        String apiKey = register("email@provider.net");

        unregister(apiKey);

        thrown.expect(NotAuthorizedException.class);
        restBidder.getUserInfo(apiKey);
    }

    @Test
    public void should_get_info() {
        String apiKey = null;
        try {
            apiKey = register("email@provider.net");

            User userInfo = restBidder.getUserInfo(apiKey);

            assertThat(userInfo.getName()).isEqualTo("email@provider.net");
            assertThat(userInfo.getBalance()).isEqualTo(1000);
            assertThat(userInfo.getItems()).isEmpty();
            assertThat(userInfo.getKey()).isEqualTo(apiKey);
        } finally {
            if (apiKey != null)
                unregister(apiKey);
        }
    }

    @Test
    public void should_get_users() {
        String apiKey = null;
        try {
            apiKey = register("email@provider.net");

            Set<PublicUser> users = restBidder.getPublicUsers();

            assertThat(users).hasSize(1).containsOnly(new PublicUser("email@provider.net", 1000, 0));
        } finally {
            if (apiKey != null) {
                unregister(apiKey);
            }
        }
    }

    @Test
    public void should_get_current_bid_offer() {
        BidOffer currentOffer = restBidder.getCurrentOffer();

        assertThat(currentOffer.getItem()).isNotNull();
        assertThat(currentOffer.getTimeToLive()).isGreaterThanOrEqualTo(0);
    }

    @Test
    public void should_bid() {
        String apiKey = null;
        try {
            apiKey = register("email@provider.net");
            BidOffer currentBidOffer = restBidder.getCurrentOffer();
            double firstValue = currentBidOffer.getItem().getValue();
            double newValue = firstValue * 1.1;

            BidOffer bidOffer = restBidder.bidForm(currentBidOffer.getItem().getName(), newValue, apiKey);

            assertThat(bidOffer.getBidder()).isEqualTo("email@provider.net");
            assertThat(bidOffer.getItem().getValue()).isEqualTo(newValue);
        } finally {
            if (apiKey != null) {
                unregister(apiKey);
            }
        }
    }

    @Test
    public void should_sell() throws InterruptedException {
        String apiKey = null;
        try {
            apiKey = register("email@provider.net");
            BidOffer currentBidOffer = restBidder.getCurrentOffer();
            double firstValue = currentBidOffer.getItem().getValue();
            double newValue = firstValue * 1.1;
            String itemName = currentBidOffer.getItem().getName();
            restBidder.bidForm(itemName, newValue, apiKey);
            Thread.sleep(10000);
            newValue = Math.round(newValue * 2);

            restBidder.sell(itemName, newValue, apiKey);

            Thread.sleep(10000);
            currentBidOffer = restBidder.getCurrentOffer();
            assertThat(currentBidOffer.getOwner()).isEqualTo("email@provider.net");
            assertThat(currentBidOffer.getItem().getValue()).isEqualTo(newValue);
        } finally {
            if (apiKey != null) {
                unregister(apiKey);
            }
        }
    }

    private String register(String name) {
        return target.path("/users/register")
                .queryParam("name", name)
                .request()
                .header(AUTHORIZATION, ADMIN_KEY)
                .get(String.class);
    }

    private void unregister(String apiKey) {
        target.path("/users/unregister")
                .queryParam("key", apiKey)
                .request()
                .header(AUTHORIZATION, ADMIN_KEY)
                .delete();
    }
}
