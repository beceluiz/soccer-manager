package com.luisguadagnin.soccermanager;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luisguadagnin.soccermanager.dto.UpdateTeamRequest;
import com.luisguadagnin.soccermanager.exception.BadRequestException;
import com.luisguadagnin.soccermanager.model.Team;
import com.luisguadagnin.soccermanager.repository.TeamRepository;

import java.util.Map;

public class UpdateTeamHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper;
    private final TeamRepository teamRepository;

    public UpdateTeamHandler() {
        objectMapper = new ObjectMapper();
        teamRepository = new TeamRepository();
    }

    UpdateTeamHandler(TeamRepository teamRepository) {
        this.objectMapper = new ObjectMapper();
        this.teamRepository = teamRepository;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        try {
            updateTeam(apiGatewayProxyRequestEvent);
        } catch (BadRequestException ex) {
            return new APIGatewayProxyResponseEvent()
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(buildErrorResponse(ex.getMessage()))
                    .withStatusCode(400);
        } catch (Exception ex) {
            return new APIGatewayProxyResponseEvent()
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(buildErrorResponse("Internal server error"))
                    .withStatusCode(500);
        }

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200);
    }

    private void updateTeam(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent) {
        String username = extractUsername(apiGatewayProxyRequestEvent);
        UpdateTeamRequest updateTeamRequest = convertInput(apiGatewayProxyRequestEvent.getBody());

        validateUpdateTeamRequest(updateTeamRequest);

        Team team = teamRepository.findById(username);
        team.setCountry(updateTeamRequest.getCountry());
        team.setName(updateTeamRequest.getName());
        teamRepository.save(team);
    }

    @SuppressWarnings("unchecked")
    private String extractUsername(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent) {
        try {
            Map<String, Object> authorizer = apiGatewayProxyRequestEvent.getRequestContext().getAuthorizer();
            Map<String, Object> jwt = (Map<String, Object>) authorizer.get("jwt");
            Map<String, Object> claims = (Map<String, Object>) jwt.get("claims");
            return (String) claims.get("username");
        } catch (ClassCastException ex) {
            throw new RuntimeException("Unable to process authorization token", ex);
        }
    }

    private UpdateTeamRequest convertInput(String body) {
        try {
            return objectMapper.readValue(body, UpdateTeamRequest.class);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid request body");
        }
    }

    private void validateUpdateTeamRequest(UpdateTeamRequest updateTeamRequest) {
        validate(!StringUtils.isNullOrEmpty(updateTeamRequest.getCountry()), "\"country\" field is empty");
        validate(!StringUtils.isNullOrEmpty(updateTeamRequest.getName()), "\"name\" field is empty");
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
