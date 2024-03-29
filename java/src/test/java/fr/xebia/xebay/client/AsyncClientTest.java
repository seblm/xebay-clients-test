package fr.xebia.xebay.client;

import fr.xebia.xebay.domain.BidOffer;
import fr.xebia.xebay.domain.PluginInfo;
import org.junit.Rule;
import org.junit.Test;

import javax.websocket.DeploymentException;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;

public class AsyncClientTest {
    @Rule
    public BidderTest bidderTest = new BidderTest();

    @Test
    public void should_notify_bid_offer() throws DeploymentException, IOException, URISyntaxException, InterruptedException {
        String apiKey = null;
        AsyncClient bidder = null;
        try {
            apiKey = bidderTest.register("email@provider.net");
            bidder = new AsyncClient("localhost:8080", apiKey);
            final WebSocketBidderStore bidOfferResult = new WebSocketBidderStore();
            bidder.onBidOfferChange(updatedBidOffer -> {
                bidOfferResult.set(updatedBidOffer);
                synchronized (this) {
                    this.notifyAll();
                }
            });

            bidderTest.bid(apiKey);

            synchronized (this) {
                this.wait(3000);
            }
            assertThat(bidOfferResult.updatedBidOffer).isNotNull();
            assertThat(bidOfferResult.updatedBidOffer.getOwner()).isNull();
            assertThat(bidOfferResult.updatedBidOffer.getItem().getName()).isNotEmpty();
        } finally {
            if (bidder != null) {
                bidder.session.close();
            }
            if (apiKey != null) {
                bidderTest.unregister(apiKey);
            }
        }
    }

    @Test
    public void should_notify_info() throws DeploymentException, IOException, URISyntaxException, InterruptedException {
        String apiKey = null;
        AsyncClient bidder = null;
        try {
            apiKey = bidderTest.register("email@provider.net");
            bidder = new AsyncClient("localhost:8080", apiKey);
            final WebSocketBidderStore result = new WebSocketBidderStore();
            bidder.onPluginInfo(info -> {
                result.setPluginInfo(info);
                synchronized (this) {
                    this.notifyAll();
                }
            });

            bidderTest.activate("BankBuyEverything");

            synchronized (this) {
                this.wait(3000);
            }
            assertThat(result.pluginInfo.getName()).isEqualTo("BankBuyEverything");
            assertThat(result.pluginInfo.isActivated()).isTrue();
        } finally {
            if (bidder != null) {
                bidder.session.close();
            }
            bidderTest.deactivate("BankBuyEverything");
            if (apiKey != null) {
                bidderTest.unregister(apiKey);
            }
        }
    }

    private final static class WebSocketBidderStore {
        private BidOffer updatedBidOffer;
        private PluginInfo pluginInfo;

        private void set(BidOffer updatedBidOffer) {
            this.updatedBidOffer = updatedBidOffer;
        }

        private void setPluginInfo(PluginInfo pluginInfo) {
            this.pluginInfo = pluginInfo;
        }
    }
}
