package com.luisguadagnin.soccermanager;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.luisguadagnin.soccermanager.model.Team;
import com.luisguadagnin.soccermanager.repository.TeamRepository;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateTeamHandlerTest {

    private final TeamRepository teamRepository;
    private final UpdateTeamHandler updateTeamHandler;

    public UpdateTeamHandlerTest() {
        this.teamRepository = mock(TeamRepository.class);
        this.updateTeamHandler = new UpdateTeamHandler(teamRepository);
    }

    @Test
    public void shouldUpdateTeamSuccessfully() {
        String username = "username-test";
        String requestBody = "{" +
                "  \"name\": \"Nova Era\"," +
                "  \"country\": \"Madagascar\"" +
                "}";

        Team team = Team.builder()
                .id(username)
                .name("New Kids On The Block")
                .country("Argentina")
                .build();

        when(teamRepository.findById(username))
                .thenReturn(team);

        APIGatewayProxyRequestEvent input = createInput(username, requestBody);

        APIGatewayProxyResponseEvent output = updateTeamHandler.handleRequest(input, null);

        assertAll(
                () -> assertEquals(200, output.getStatusCode()),
                () -> verify(teamRepository).save(team),
                () -> assertEquals("Nova Era", team.getName()),
                () -> assertEquals("Madagascar", team.getCountry())
        );
    }

    @Test
    public void shouldNotUpdateTeamWhenRequestIsInvalid() {
        String username = "username-test";
        String requestBody = "{" +
                "  \"anyAttribute\": \"anyValue\"" +
                "}";

        APIGatewayProxyRequestEvent input = createInput(username, requestBody);

        APIGatewayProxyResponseEvent output = updateTeamHandler.handleRequest(input, null);

        assertAll(
                () -> assertEquals(400, output.getStatusCode()),
                () -> verify(teamRepository, never()).save(any())
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
