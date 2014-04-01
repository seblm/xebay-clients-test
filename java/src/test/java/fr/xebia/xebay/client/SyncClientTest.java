package fr.xebia.xebay.client;

import fr.xebia.xebay.domain.BidOffer;
import fr.xebia.xebay.domain.PublicUser;
import fr.xebia.xebay.domain.User;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.ws.rs.NotAuthorizedException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class SyncClientTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public BidderTest bidderTest = new BidderTest();

    @Test
    public void should_register() {
        String apiKey = null;
        try {
            apiKey = bidderTest.register("email@provider.net");

            assertThat(apiKey).isNotNull();
        } finally {
            if (apiKey != null)
                bidderTest.unregister(apiKey);
        }
    }

    @Test
    public void should_unregister() {
        String apiKey = bidderTest.register("email@provider.net");

        bidderTest.unregister(apiKey);

        thrown.expect(NotAuthorizedException.class);
        bidderTest.syncClient.getUserInfo(apiKey);
    }

    @Test
    public void should_get_info() {
        String apiKey = null;
        try {
            apiKey = bidderTest.register("email@provider.net");

            User userInfo = bidderTest.syncClient.getUserInfo(apiKey);

            assertThat(userInfo.getName()).isEqualTo("email@provider.net");
            assertThat(userInfo.getBalance()).isEqualTo(1000);
            assertThat(userInfo.getItems()).isEmpty();
            assertThat(userInfo.getKey()).isEqualTo(apiKey);
        } finally {
            if (apiKey != null)
                bidderTest.unregister(apiKey);
        }
    }

    @Test
    public void should_get_users() {
        String apiKey = null;
        try {
            apiKey = bidderTest.register("email@provider.net");

            Set<PublicUser> users = bidderTest.syncClient.getPublicUsers();

            assertThat(users).hasSize(1).containsOnly(new PublicUser("email@provider.net", 1000, 0));
        } finally {
            if (apiKey != null) {
                bidderTest.unregister(apiKey);
            }
        }
    }

    @Test
    public void should_get_current_bid_offer() {
        BidOffer currentOffer = bidderTest.syncClient.getCurrentOffer();

        assertThat(currentOffer.getItem()).isNotNull();
        assertThat(currentOffer.getTimeToLive()).isGreaterThanOrEqualTo(0);
    }

    @Test
    public void should_bid() {
        String apiKey = null;
        try {
            apiKey = bidderTest.register("email@provider.net");
            BidOffer currentBidOffer = bidderTest.syncClient.getCurrentOffer();
            double firstValue = currentBidOffer.getItem().getValue();
            double newValue = firstValue * 2;

            BidOffer bidOffer = bidderTest.syncClient.bid(currentBidOffer.getItem().getName(), newValue, apiKey);

            assertThat(bidOffer.getBidder()).isEqualTo("email@provider.net");
            assertThat(bidOffer.getItem().getValue()).isEqualTo(newValue);
        } finally {
            if (apiKey != null) {
                bidderTest.unregister(apiKey);
            }
        }
    }

    @Test
    public void should_sell() throws InterruptedException {
        String apiKey = null;
        try {
            apiKey = bidderTest.register("email@provider.net");
            BidOffer currentBidOffer = bidderTest.syncClient.getCurrentOffer();
            double firstValue = currentBidOffer.getItem().getValue();
            double newValue = firstValue * 1.1;
            String itemName = currentBidOffer.getItem().getName();
            bidderTest.syncClient.bid(itemName, newValue, apiKey);
            Thread.sleep(10000);
            newValue = Math.round(newValue * 2);

            bidderTest.syncClient.sell(itemName, newValue, apiKey);

            Thread.sleep(10000);
            currentBidOffer = bidderTest.syncClient.getCurrentOffer();
            assertThat(currentBidOffer.getOwner()).isEqualTo("email@provider.net");
            assertThat(currentBidOffer.getItem().getValue()).isEqualTo(newValue);
        } finally {
            if (apiKey != null) {
                bidderTest.unregister(apiKey);
            }
        }
    }
}
