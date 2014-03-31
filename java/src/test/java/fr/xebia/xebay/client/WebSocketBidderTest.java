package fr.xebia.xebay.client;

import org.junit.Rule;
import org.junit.Test;

import javax.websocket.DeploymentException;
import java.io.IOException;
import java.net.URISyntaxException;

public class WebSocketBidderTest {
    @Rule
    public BidderTest bidderTest = new BidderTest();

    @Test
    public void should_idontknow() throws DeploymentException, IOException, URISyntaxException, InterruptedException {
        String apiKey = null;
        try {
            apiKey = bidderTest.register("email@provider.net");

            WebSocketBidder bidder = new WebSocketBidder("ws://localhost:8080/socket/bidEngine/" + apiKey, System.out::println);
        } finally {
            if (apiKey != null) {
                bidderTest.unregister(apiKey);
            }
        }
    }
}
