package com.luisguadagnin.soccermanager;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.luisguadagnin.soccermanager.clients.CognitoClient;
import com.luisguadagnin.soccermanager.exception.UserAlreadyExistsException;
import com.luisguadagnin.soccermanager.model.Player;
import com.luisguadagnin.soccermanager.model.Team;
import com.luisguadagnin.soccermanager.repository.PlayerRepository;
import com.luisguadagnin.soccermanager.repository.TeamRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CreateUserHandlerTest {

    private final CreateUserHandler createUserHandler;
    private final CognitoClient cognitoClient;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    public CreateUserHandlerTest() {
        this.cognitoClient = mock(CognitoClient.class);
        this.teamRepository = mock(TeamRepository.class);
        this.playerRepository = mock(PlayerRepository.class);
        this.createUserHandler = new CreateUserHandler(cognitoClient, teamRepository, playerRepository);
    }

    @Test
    public void shouldCreateUserSuccessfully() {
        String email = "luis@guadagnin.com";
        String password = "123456";
        APIGatewayProxyRequestEvent input = createInput("{" +
                "  \"email\": \"" + email + "\"," +
                "  \"password\": \"" + password + "\"" +
                "}");

        APIGatewayProxyResponseEvent output = createUserHandler.handleRequest(input, null);

        assertAll(
                () -> assertEquals(200, output.getStatusCode()),
                () -> verify(cognitoClient).createUser(email, password),
                () -> verify(teamRepository).save(any(Team.class)),
                () -> verify(playerRepository, times(20)).save(any(Player.class))
        );
    }

    @Test
    public void shouldNotCreateUserWhenRequestBodyIsInvalid() {
        APIGatewayProxyRequestEvent input = createInput("{" +
                "  \"anyAttribute\": \"anyValue\"" +
                "}");

        APIGatewayProxyResponseEvent output = createUserHandler.handleRequest(input, null);

        assertAll(
                () -> assertEquals(400, output.getStatusCode()),
                () -> verify(cognitoClient, never()).createUser(any(), any()),
                () -> verify(teamRepository, never()).save(any()),
                () -> verify(playerRepository, never()).save(any())
        );
    }

    @Test
    public void shouldNotCreateUserWhenPasswordIsInvalid() {
        String email = "luis@guadagnin.com";
        String password = "12";
        APIGatewayProxyRequestEvent input = createInput("{" +
                "  \"email\": \"" + email + "\"," +
                "  \"password\": \"" + password + "\"" +
                "}");

        APIGatewayProxyResponseEvent output = createUserHandler.handleRequest(input, null);

        assertAll(
                () -> assertEquals(400, output.getStatusCode()),
                () -> verify(cognitoClient, never()).createUser(any(), any()),
                () -> verify(teamRepository, never()).save(any()),
                () -> verify(playerRepository, never()).save(any())
        );
    }

    @Test
    public void shouldNotCreateUserWhenUserAlreadyExists() {
        String email = "luis@guadagnin.com";
        String password = "123456";
        APIGatewayProxyRequestEvent input = createInput("{" +
                "  \"email\": \"" + email + "\"," +
                "  \"password\": \"" + password + "\"" +
                "}");

        doThrow(new UserAlreadyExistsException())
                .when(cognitoClient).createUser(email, password);

        APIGatewayProxyResponseEvent output = createUserHandler.handleRequest(input, null);

        assertAll(
                () -> assertEquals(400, output.getStatusCode()),
                () -> verify(cognitoClient).createUser(email, password),
                () -> verify(teamRepository, never()).save(any()),
                () -> verify(playerRepository, never()).save(any())
        );
    }

    private APIGatewayProxyRequestEvent createInput(String body) {
        return new APIGatewayProxyRequestEvent()
                .withBody(body);
    }
}
