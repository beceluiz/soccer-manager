package com.luisguadagnin.soccermanager;

import com.amazonaws.services.cognitoidp.model.UserNotFoundException;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.luisguadagnin.soccermanager.clients.CognitoClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LoginHandlerTest {

    private final LoginHandler loginHandler;
    private final CognitoClient cognitoClient;

    public LoginHandlerTest() {
        this.cognitoClient = mock(CognitoClient.class);
        this.loginHandler = new LoginHandler(cognitoClient);
    }

    @Test
    public void shouldLoginSuccessfully() {
        String email = "luis@guadagnin.com";
        String password = "123456";
        APIGatewayProxyRequestEvent input = createInput("{" +
                "  \"email\": \"" + email + "\"," +
                "  \"password\": \"" + password + "\"" +
                "}");
        String authToken = "eyLasoinfdaonaonsdaonsdagax";

        when(cognitoClient.auth(email, password))
                .thenReturn(authToken);

        APIGatewayProxyResponseEvent output = loginHandler.handleRequest(input, null);

        assertAll(
                () -> assertEquals(200, output.getStatusCode()),
                () -> assertEquals("Bearer " + authToken, output.getHeaders().get("Authorization"))
        );
    }

    @Test
    public void shouldFailToLoginWhenUserDoesNotExist() {
        String email = "luis@guadagnin.com";
        String password = "123456";
        APIGatewayProxyRequestEvent input = createInput("{" +
                "  \"email\": \"" + email + "\"," +
                "  \"password\": \"" + password + "\"" +
                "}");

        when(cognitoClient.auth(email, password))
                .thenThrow(new UserNotFoundException("error message"));

        APIGatewayProxyResponseEvent output = loginHandler.handleRequest(input, null);

        assertAll(
                () -> assertEquals(404, output.getStatusCode())
        );
    }

    private APIGatewayProxyRequestEvent createInput(String body) {
        return new APIGatewayProxyRequestEvent()
                .withBody(body);
    }
}
