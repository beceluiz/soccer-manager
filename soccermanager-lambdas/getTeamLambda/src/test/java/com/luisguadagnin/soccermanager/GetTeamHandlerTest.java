package com.luisguadagnin.soccermanager;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.luisguadagnin.soccermanager.model.Player;
import com.luisguadagnin.soccermanager.model.Team;
import com.luisguadagnin.soccermanager.model.enums.PlayerPosition;
import com.luisguadagnin.soccermanager.repository.PlayerRepository;
import com.luisguadagnin.soccermanager.repository.TeamRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GetTeamHandlerTest {

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final GetTeamHandler getTeamHandler;

    public GetTeamHandlerTest() {
        this.teamRepository = mock(TeamRepository.class);
        this.playerRepository = mock(PlayerRepository.class);
        this.getTeamHandler = new GetTeamHandler(teamRepository, playerRepository);
    }

    @Test
    public void shouldGetTeamSuccessfully() {
        String username = "username-test";
        APIGatewayProxyRequestEvent input = createInput(username);

        Set<String> playersIds = Set.of("player-uuid-1", "player-uuid-2", "player-uuid-3");
        Team team = Team.builder()
                .id(username)
                .playersId(playersIds)
                .value("3000000.00")
                .budget("5000000.00")
                .country("Brazil")
                .name("The Avengers")
                .build();
        List<Player> players = List.of(
                Player.builder()
                        .id("player-uuid-1")
                        .position(PlayerPosition.ATTACKER)
                        .country("Brazil")
                        .teamId(username)
                        .value("1000000.00")
                        .firstName("John")
                        .lastName("Cena")
                        .age(23)
                        .build(),
                Player.builder()
                        .id("player-uuid-2")
                        .position(PlayerPosition.ATTACKER)
                        .country("Brazil")
                        .teamId(username)
                        .value("1000000.00")
                        .firstName("Johnny")
                        .lastName("Rotten")
                        .age(24)
                        .build(),
                Player.builder()
                        .id("player-uuid-3")
                        .position(PlayerPosition.ATTACKER)
                        .country("Brazil")
                        .teamId(username)
                        .value("1000000.00")
                        .firstName("James")
                        .lastName("Hetfield")
                        .age(25)
                        .build()
        );

        when(teamRepository.findById(username))
                .thenReturn(team);
        when(playerRepository.findById(playersIds))
                .thenReturn(players);

        APIGatewayProxyResponseEvent output = getTeamHandler.handleRequest(input, null);

        assertAll(
                () -> assertEquals(200, output.getStatusCode()),
                () -> assertEquals("{\"id\":\"username-test\",\"name\":\"The Avengers\",\"country\":\"Brazil\",\"value\":\"3000000.00\",\"budget\":\"5000000.00\",\"players\":[" +
                                "{\"id\":\"player-uuid-1\",\"firstName\":\"John\",\"lastName\":\"Cena\",\"country\":\"Brazil\",\"age\":23,\"value\":\"1000000.00\",\"position\":\"ATTACKER\"}," +
                                "{\"id\":\"player-uuid-2\",\"firstName\":\"Johnny\",\"lastName\":\"Rotten\",\"country\":\"Brazil\",\"age\":24,\"value\":\"1000000.00\",\"position\":\"ATTACKER\"}," +
                                "{\"id\":\"player-uuid-3\",\"firstName\":\"James\",\"lastName\":\"Hetfield\",\"country\":\"Brazil\",\"age\":25,\"value\":\"1000000.00\",\"position\":\"ATTACKER\"}]}",
                        output.getBody()),
                () -> verify(teamRepository).findById(username),
                () -> verify(playerRepository).findById(playersIds)
        );

    }

    private APIGatewayProxyRequestEvent createInput(String username) {
        APIGatewayProxyRequestEvent.ProxyRequestContext context = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        context.setAuthorizer(Map.of("jwt", Map.of("claims", Map.of("username", username))));

        return new APIGatewayProxyRequestEvent()
                .withRequestContext(context);
    }
}
