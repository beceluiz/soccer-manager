package com.luisguadagnin.soccermanager;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.luisguadagnin.soccermanager.model.Player;
import com.luisguadagnin.soccermanager.model.Team;
import com.luisguadagnin.soccermanager.repository.PlayerRepository;
import com.luisguadagnin.soccermanager.repository.TeamRepository;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdatePlayerHandlerTest {

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final UpdatePlayerHandler updatePlayerHandler;

    public UpdatePlayerHandlerTest() {
        this.playerRepository = mock(PlayerRepository.class);
        this.teamRepository = mock(TeamRepository.class);
        this.updatePlayerHandler = new UpdatePlayerHandler(teamRepository, playerRepository);
    }

    @Test
    public void shouldUpdatePlayerSuccessfully() {
        String username = "username-test";
        String playerId = "player-uuid";
        String requestBody = "{" +
                "  \"firstName\": \"Louis\"," +
                "  \"lastName\": \"Lane\"," +
                "  \"country\": \"Nigeria\"" +
                "}";

        Player player = Player.builder()
                .id(playerId)
                .firstName("Clark")
                .lastName("Kent")
                .country("Germany")
                .build();
        Team team = Team.builder()
                .id(username)
                .playersId(Set.of(playerId))
                .build();

        when(playerRepository.findById(playerId))
                .thenReturn(player);
        when(teamRepository.findById(username))
                .thenReturn(team);

        APIGatewayProxyRequestEvent input = createInput(username, requestBody, playerId);

        APIGatewayProxyResponseEvent output = updatePlayerHandler.handleRequest(input, null);

        assertAll(
                () -> assertEquals(200, output.getStatusCode()),
                () -> assertEquals("Louis", player.getFirstName()),
                () -> assertEquals("Lane", player.getLastName()),
                () -> assertEquals("Nigeria", player.getCountry()),
                () -> verify(playerRepository).save(player)
        );
    }

    @Test
    public void shouldNotUpdateWhenPlayerIsNotFromLoggedUserTeam() {
        String username = "username-test";
        String playerId = "player-uuid";
        String requestBody = "{" +
                "  \"firstName\": \"Louis\"," +
                "  \"lastName\": \"Lane\"," +
                "  \"country\": \"Nigeria\"" +
                "}";

        Player player = Player.builder()
                .id(playerId)
                .firstName("Clark")
                .lastName("Kent")
                .country("Germany")
                .build();
        Team team = Team.builder()
                .id(username)
                .playersId(Set.of("other-player-uuid"))
                .build();

        when(playerRepository.findById(playerId))
                .thenReturn(player);
        when(teamRepository.findById(username))
                .thenReturn(team);

        APIGatewayProxyRequestEvent input = createInput(username, requestBody, playerId);

        APIGatewayProxyResponseEvent output = updatePlayerHandler.handleRequest(input, null);

        assertAll(
                () -> assertEquals(403, output.getStatusCode()),
                () -> verify(playerRepository, never()).save(any())
        );
    }

    @Test
    public void shouldNotUpdateWhenPlayerDoesNotExist() {
        String username = "username-test";
        String playerId = "player-uuid";
        String requestBody = "{" +
                "  \"firstName\": \"Louis\"," +
                "  \"lastName\": \"Lane\"," +
                "  \"country\": \"Nigeria\"" +
                "}";

        when(playerRepository.findById(playerId))
                .thenReturn(null);

        APIGatewayProxyRequestEvent input = createInput(username, requestBody, playerId);

        APIGatewayProxyResponseEvent output = updatePlayerHandler.handleRequest(input, null);

        assertAll(
                () -> assertEquals(404, output.getStatusCode()),
                () -> verify(playerRepository, never()).save(any())
        );
    }

    @Test
    public void shouldNotUpdateWhenRequestIsInvalid() {
        String username = "username-test";
        String playerId = "player-uuid";
        String requestBody = "{" +
                "  \"anyAttribute1\": \"anyValue\"," +
                "  \"anyAttribute2\": \"anyValue\"" +
                "}";

        APIGatewayProxyRequestEvent input = createInput(username, requestBody, playerId);

        APIGatewayProxyResponseEvent output = updatePlayerHandler.handleRequest(input, null);

        assertAll(
                () -> assertEquals(400, output.getStatusCode()),
                () -> verify(playerRepository, never()).save(any())
        );
    }

    private APIGatewayProxyRequestEvent createInput(String username, String body, String playerId) {
        APIGatewayProxyRequestEvent.ProxyRequestContext context = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        context.setAuthorizer(Map.of("jwt", Map.of("claims", Map.of("username", username))));

        return new APIGatewayProxyRequestEvent()
                .withBody(body)
                .withPathParameters(Map.of("playerId", playerId))
                .withRequestContext(context);
    }

}
