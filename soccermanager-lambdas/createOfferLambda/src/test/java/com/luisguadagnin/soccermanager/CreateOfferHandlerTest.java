package com.luisguadagnin.soccermanager;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.luisguadagnin.soccermanager.model.Offer;
import com.luisguadagnin.soccermanager.model.Player;
import com.luisguadagnin.soccermanager.model.Team;
import com.luisguadagnin.soccermanager.model.enums.PlayerPosition;
import com.luisguadagnin.soccermanager.repository.OfferRepository;
import com.luisguadagnin.soccermanager.repository.PlayerRepository;
import com.luisguadagnin.soccermanager.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateOfferHandlerTest {

    private final CreateOfferHandler createOfferHandler;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final OfferRepository offerRepository;

    public CreateOfferHandlerTest() {
        this.teamRepository = Mockito.mock(TeamRepository.class);
        this.playerRepository = Mockito.mock(PlayerRepository.class);
        this.offerRepository = Mockito.mock(OfferRepository.class);
        this.createOfferHandler = new CreateOfferHandler(teamRepository, playerRepository, offerRepository);
    }

    @Test
    public void shouldCreateOfferSuccessfully() {
        String username = "username-test";
        String playerId = "uuid-test";
        String price = "50000.00";
        APIGatewayProxyRequestEvent input = createInput(username, "{" +
                "  \"playerId\": \"" + playerId + "\"," +
                "  \"price\": \"" + price + "\"" +
                "}");
        Player player = Player.builder()
                .id(playerId)
                .teamId(username)
                .value("100000.00")
                .country("Brazil")
                .position(PlayerPosition.ATTACKER)
                .build();
        Team team = Team.builder()
                .id(username)
                .playersId(Set.of(player.getId()))
                .build();

        when(playerRepository.findById(playerId))
                .thenReturn(player);
        when(teamRepository.findById(username))
                .thenReturn(team);
        when(offerRepository.findById(playerId))
                .thenReturn(null);

        APIGatewayProxyResponseEvent output = createOfferHandler.handleRequest(input, null);

        Offer expectedOffer = Offer.builder()
                .id(playerId)
                .position(player.getPosition())
                .country(player.getCountry())
                .price(5000000L)
                .discount(5000)
                .build();

        assertAll(
                () -> assertEquals(200, output.getStatusCode()),
                () -> verify(offerRepository).save(expectedOffer)
        );
    }

    @Test
    public void shouldNotCreateOfferWhenRequestBodyIsInvalid() {
        String username = "username-test";
        APIGatewayProxyRequestEvent input = createInput(username, "{" +
                "  \"anyAttribute\": \"anyValue\"" +
                "}");

        APIGatewayProxyResponseEvent output = createOfferHandler.handleRequest(input, null);

        assertAll(
                () -> assertEquals(400, output.getStatusCode()),
                () -> verify(offerRepository, never()).save(any())
        );
    }

    @Test
    public void shouldNotCreateOfferWhenPriceIsEmpty() {
        String username = "username-test";
        String playerId = "uuid-test";
        String price = "50000.00";
        APIGatewayProxyRequestEvent input = createInput(username, "{" +
                "  \"playerId\": \"" + playerId + "\"," +
                "  \"price\": \"" + price + "\"" +
                "}");

        when(playerRepository.findById(playerId))
                .thenReturn(null);

        APIGatewayProxyResponseEvent output = createOfferHandler.handleRequest(input, null);

        assertAll(
                () -> assertEquals(404, output.getStatusCode()),
                () -> verify(offerRepository, never()).save(any())
        );
    }

    @Test
    public void shouldNotCreateOfferWhenPlayerDoesNotExist() {
        String username = "username-test";
        APIGatewayProxyRequestEvent input = createInput(username, "{" +
                "  \"playerId\": \"any-player-uuid\"" +
                "}");

        APIGatewayProxyResponseEvent output = createOfferHandler.handleRequest(input, null);

        assertAll(
                () -> assertEquals(400, output.getStatusCode()),
                () -> verify(offerRepository, never()).save(any())
        );
    }

    @Test
    public void shouldNotCreateOfferWhenPlayerDoesNotBelongToLoggerUserTeam() {
        String username = "username-test";
        String playerId = "uuid-test";
        String price = "50000.00";
        APIGatewayProxyRequestEvent input = createInput(username, "{" +
                "  \"playerId\": \"" + playerId + "\"," +
                "  \"price\": \"" + price + "\"" +
                "}");
        Player player = Player.builder()
                .id(playerId)
                .teamId(username)
                .value("100000.00")
                .country("Brazil")
                .position(PlayerPosition.ATTACKER)
                .build();
        Team team = Team.builder()
                .id(username)
                .playersId(Set.of("other-players-uuid"))
                .build();

        when(playerRepository.findById(playerId))
                .thenReturn(player);
        when(teamRepository.findById(username))
                .thenReturn(team);

        APIGatewayProxyResponseEvent output = createOfferHandler.handleRequest(input, null);

        assertAll(
                () -> assertEquals(403, output.getStatusCode()),
                () -> verify(offerRepository, never()).save(any())
        );
    }

    @Test
    public void shouldNotCreateOfferWhenAnOfferAlreadyExistsForThatPlayer() {
        String username = "username-test";
        String playerId = "uuid-test";
        String price = "50000.00";
        APIGatewayProxyRequestEvent input = createInput(username, "{" +
                "  \"playerId\": \"" + playerId + "\"," +
                "  \"price\": \"" + price + "\"" +
                "}");
        Player player = Player.builder()
                .id(playerId)
                .teamId(username)
                .value("100000.00")
                .country("Brazil")
                .position(PlayerPosition.ATTACKER)
                .build();
        Team team = Team.builder()
                .id(username)
                .playersId(Set.of(player.getId()))
                .build();
        Offer offer = Offer.builder()
                .id(playerId)
                .build();

        when(playerRepository.findById(playerId))
                .thenReturn(player);
        when(teamRepository.findById(username))
                .thenReturn(team);
        when(offerRepository.findById(playerId))
                .thenReturn(offer);

        APIGatewayProxyResponseEvent output = createOfferHandler.handleRequest(input, null);

        assertAll(
                () -> assertEquals(400, output.getStatusCode()),
                () -> verify(offerRepository, never()).save(any())
        );
    }

    private APIGatewayProxyRequestEvent createInput(String username, String body) {
        APIGatewayProxyRequestEvent.ProxyRequestContext context = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        context.setAuthorizer(Map.of("jwt", Map.of("claims", Map.of("username", username))));

        return new APIGatewayProxyRequestEvent()
                .withBody(body)
                .withRequestContext(context);
    }
}
