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

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PurchasePlayerHandlerTest {

    private final PurchasePlayerHandler purchasePlayerHandler;
    private final PlayerRepository playerRepository;
    private final OfferRepository offerRepository;
    private final TeamRepository teamRepository;

    public PurchasePlayerHandlerTest() {
        this.playerRepository = mock(PlayerRepository.class);
        this.offerRepository = mock(OfferRepository.class);
        this.teamRepository = mock(TeamRepository.class);
        this.purchasePlayerHandler = new PurchasePlayerHandler(playerRepository, offerRepository, teamRepository);
    }

    @Test
    public void shouldPurchasePlayerSuccessfully() {
        String username = "username-test";
        String playerId = "player-uuid";
        String requestBody = "{" +
                "  \"playerId\": \"" + playerId + "\"" +
                "}";
        String originalTeamId = "original-team-uuid";

        Player player = Player.builder()
                .id(playerId)
                .age(20)
                .firstName("Julian")
                .lastName("Assange")
                .value("1000000.00")
                .teamId(originalTeamId)
                .country("Colombia")
                .position(PlayerPosition.MIDFIELDER)
                .build();
        Team originalTeam = Team.builder()
                .id(originalTeamId)
                .name("Los Grandes")
                .country("Colombia")
                .budget("5000000.00")
                .playersId(new HashSet<>(Set.of(playerId)))
                .value("20000000.00")
                .build();
        Team newTeam = Team.builder()
                .id(username)
                .name("Los Mejores")
                .country("Chile")
                .budget("5000000.00")
                .playersId(new HashSet<>())
                .value("20000000.00")
                .build();
        Offer offer = Offer.builder()
                .id(playerId)
                .price(150000000)
                .build();

        when(playerRepository.findById(playerId))
                .thenReturn(player);
        when(teamRepository.findById(anyString()))
                .thenReturn(originalTeam)
                .thenReturn(newTeam);
        when(offerRepository.findById(playerId))
                .thenReturn(offer);

        APIGatewayProxyRequestEvent input = createInput(username, requestBody);

        APIGatewayProxyResponseEvent output = purchasePlayerHandler.handleRequest(input, null);

        assertAll(
                () -> assertEquals(200, output.getStatusCode()),
                () -> verify(teamRepository).findById(originalTeamId),
                () -> verify(teamRepository).findById(username),
                () -> assertEquals("6500000.00", originalTeam.getBudget()),
                () -> assertEquals("3500000.00", newTeam.getBudget()),
                () -> assertEquals("19000000.00", originalTeam.getValue()),
                () -> assertTrue(new BigDecimal("21000000.00").compareTo(new BigDecimal(newTeam.getValue())) < 0),
                () -> assertFalse(originalTeam.getPlayersId().contains(playerId)),
                () -> assertTrue(newTeam.getPlayersId().contains(playerId)),
                () -> assertEquals(username, player.getTeamId()),
                () -> assertTrue(new BigDecimal("1000000.00").compareTo(new BigDecimal(player.getValue())) < 0),
                () -> verify(playerRepository).save(player),
                () -> verify(teamRepository).save(originalTeam),
                () -> verify(teamRepository).save(newTeam),
                () -> verify(offerRepository).delete(offer)
        );
    }

    @Test
    public void shouldNotPurchasePlayerWhenRequestIsInvalid() {
        String username = "username-test";
        String requestBody = "{" +
                "  \"anyAttribute\": \"anyValue\"" +
                "}";

        APIGatewayProxyRequestEvent input = createInput(username, requestBody);

        APIGatewayProxyResponseEvent output = purchasePlayerHandler.handleRequest(input, null);

        assertAll(
                () -> assertEquals(400, output.getStatusCode()),
                () -> verify(playerRepository, never()).save(any())
        );
    }

    @Test
    public void shouldNotPurchasePlayerWhenPlayerIdIsEmpty() {
        String username = "username-test";
        String requestBody = "{" +
                "  \"playerId\": \"\"" +
                "}";

        APIGatewayProxyRequestEvent input = createInput(username, requestBody);

        APIGatewayProxyResponseEvent output = purchasePlayerHandler.handleRequest(input, null);

        assertAll(
                () -> assertEquals(400, output.getStatusCode()),
                () -> verify(playerRepository, never()).save(any())
        );
    }

    @Test
    public void shouldNotPurchasePlayerWhenPlayerDoesNotExist() {
        String username = "username-test";
        String playerId = "player-uuid";
        String requestBody = "{" +
                "  \"playerId\": \"" + playerId + "\"" +
                "}";

        when(playerRepository.findById(playerId))
                .thenReturn(null);

        APIGatewayProxyRequestEvent input = createInput(username, requestBody);

        APIGatewayProxyResponseEvent output = purchasePlayerHandler.handleRequest(input, null);

        assertAll(
                () -> assertEquals(404, output.getStatusCode()),
                () -> verify(playerRepository, never()).save(any())
        );
    }

    @Test
    public void shouldNotPurchasePlayerWhenPlayerIsInLoggedUserOwnTeam() {
        String username = "username-test";
        String playerId = "player-uuid";
        String requestBody = "{" +
                "  \"playerId\": \"" + playerId + "\"" +
                "}";

        Player player = Player.builder()
                .id(playerId)
                .age(20)
                .firstName("Julian")
                .lastName("Assange")
                .value("1000000.00")
                .teamId(username)
                .country("Colombia")
                .position(PlayerPosition.MIDFIELDER)
                .build();
        Team team = Team.builder()
                .id(username)
                .name("Los Grandes")
                .country("Colombia")
                .budget("5000000.00")
                .playersId(new HashSet<>(Set.of(playerId)))
                .value("20000000.00")
                .build();

        when(playerRepository.findById(playerId))
                .thenReturn(player);
        when(teamRepository.findById(anyString()))
                .thenReturn(team)
                .thenReturn(team);

        APIGatewayProxyRequestEvent input = createInput(username, requestBody);

        APIGatewayProxyResponseEvent output = purchasePlayerHandler.handleRequest(input, null);

        assertAll(
                () -> assertEquals(400, output.getStatusCode()),
                () -> verify(playerRepository, never()).save(any())
        );
    }

    @Test
    public void shouldNotPurchasePlayerWhenThereIsNoOffer() {
        String username = "username-test";
        String playerId = "player-uuid";
        String requestBody = "{" +
                "  \"playerId\": \"" + playerId + "\"" +
                "}";
        String originalTeamId = "original-team-uuid";

        Player player = Player.builder()
                .id(playerId)
                .age(20)
                .firstName("Julian")
                .lastName("Assange")
                .value("1000000.00")
                .teamId(originalTeamId)
                .country("Colombia")
                .position(PlayerPosition.MIDFIELDER)
                .build();
        Team originalTeam = Team.builder()
                .id(originalTeamId)
                .name("Los Grandes")
                .country("Colombia")
                .budget("5000000.00")
                .playersId(new HashSet<>(Set.of(playerId)))
                .value("20000000.00")
                .build();
        Team newTeam = Team.builder()
                .id(username)
                .name("Los Mejores")
                .country("Chile")
                .budget("5000000.00")
                .playersId(new HashSet<>())
                .value("20000000.00")
                .build();

        when(playerRepository.findById(playerId))
                .thenReturn(player);
        when(teamRepository.findById(anyString()))
                .thenReturn(originalTeam)
                .thenReturn(newTeam);
        when(offerRepository.findById(playerId))
                .thenReturn(null);

        APIGatewayProxyRequestEvent input = createInput(username, requestBody);

        APIGatewayProxyResponseEvent output = purchasePlayerHandler.handleRequest(input, null);

        assertAll(
                () -> assertEquals(400, output.getStatusCode()),
                () -> verify(playerRepository, never()).save(any())
        );
    }

    @Test
    public void shouldNotPurchasePlayerWhenBudgetIsLowerThanOfferPrice() {
        String username = "username-test";
        String playerId = "player-uuid";
        String requestBody = "{" +
                "  \"playerId\": \"" + playerId + "\"" +
                "}";
        String originalTeamId = "original-team-uuid";

        Player player = Player.builder()
                .id(playerId)
                .age(20)
                .firstName("Julian")
                .lastName("Assange")
                .value("1000000.00")
                .teamId(originalTeamId)
                .country("Colombia")
                .position(PlayerPosition.MIDFIELDER)
                .build();
        Team originalTeam = Team.builder()
                .id(originalTeamId)
                .name("Los Grandes")
                .country("Colombia")
                .budget("5000000.00")
                .playersId(new HashSet<>(Set.of(playerId)))
                .value("20000000.00")
                .build();
        Team newTeam = Team.builder()
                .id(username)
                .name("Los Mejores")
                .country("Chile")
                .budget("1000000.00")
                .playersId(new HashSet<>())
                .value("20000000.00")
                .build();
        Offer offer = Offer.builder()
                .id(playerId)
                .price(150000000)
                .build();

        when(playerRepository.findById(playerId))
                .thenReturn(player);
        when(teamRepository.findById(anyString()))
                .thenReturn(originalTeam)
                .thenReturn(newTeam);
        when(offerRepository.findById(playerId))
                .thenReturn(offer);

        APIGatewayProxyRequestEvent input = createInput(username, requestBody);

        APIGatewayProxyResponseEvent output = purchasePlayerHandler.handleRequest(input, null);

        assertAll(
                () -> assertEquals(400, output.getStatusCode()),
                () -> verify(playerRepository, never()).save(any())
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
