package com.luisguadagnin.soccermanager;

import com.amazonaws.services.cognitoidp.model.AWSCognitoIdentityProviderException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luisguadagnin.soccermanager.clients.CognitoClient;
import com.luisguadagnin.soccermanager.dto.UserData;
import com.luisguadagnin.soccermanager.exception.BadRequestException;

import java.util.Map;

public class LoginHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper;

    private final CognitoClient cognitoClient;

    public LoginHandler() {
        this.objectMapper = new ObjectMapper();
        this.cognitoClient = new CognitoClient();
    }

    LoginHandler(CognitoClient cognitoClient) {
        this.objectMapper = new ObjectMapper();
        this.cognitoClient = cognitoClient;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        String responseToken;
        try {
            responseToken = auth(apiGatewayProxyRequestEvent);
        } catch (BadRequestException ex) {
            return new APIGatewayProxyResponseEvent()
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(buildErrorResponse(ex.getMessage()))
                    .withStatusCode(400);
        } catch (AWSCognitoIdentityProviderException ex) {
            return new APIGatewayProxyResponseEvent()
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(buildErrorResponse("Invalid credentials"))
                    .withStatusCode(404);
        } catch (Exception ex) {
            return new APIGatewayProxyResponseEvent()
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(buildErrorResponse("Internal server error"))
                    .withStatusCode(500);
        }

        return new APIGatewayProxyResponseEvent()
                .withHeaders(Map.of("Authorization", "Bearer " + responseToken))
                .withStatusCode(200);
    }

    private String auth(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent) {
        UserData userData = convertInput(apiGatewayProxyRequestEvent.getBody());

        validateUserData(userData);

        return cognitoClient.auth(userData.getEmail(), userData.getPassword());
    }

    private UserData convertInput(String body) {
        try {
            return objectMapper.readValue(body, UserData.class);
        } catch(JsonProcessingException e) {
            System.out.println(body);
            e.printStackTrace();
            throw new BadRequestException("Invalid request body");
        }
    }

    private void validateUserData(UserData userData) {
        validate(!StringUtils.isNullOrEmpty(userData.getEmail()), "\"email\" field is empty");
        validate(!StringUtils.isNullOrEmpty(userData.getPassword()), "\"password\" field is empty");
    }

    private void validate(boolean isValid, String failMessage) {
        if(!isValid) {
            throw new BadRequestException(failMessage);
        }
    }

    private String buildErrorResponse(String message) {
        return "{ \"message\": \"" + message + "\" }";
    }

}
