package com.luisguadagnin.soccermanager;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luisguadagnin.soccermanager.dto.UpdatePlayerRequest;
import com.luisguadagnin.soccermanager.exception.BadRequestException;
import com.luisguadagnin.soccermanager.exception.ForbiddenException;
import com.luisguadagnin.soccermanager.exception.NotFoundException;
import com.luisguadagnin.soccermanager.model.Player;
import com.luisguadagnin.soccermanager.model.Team;
import com.luisguadagnin.soccermanager.repository.PlayerRepository;
import com.luisguadagnin.soccermanager.repository.TeamRepository;

import java.util.Map;

public class UpdatePlayerHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    public UpdatePlayerHandler() {
        objectMapper = new ObjectMapper();
        teamRepository = new TeamRepository();
        playerRepository = new PlayerRepository();
    }

    UpdatePlayerHandler(TeamRepository teamRepository, PlayerRepository playerRepository) {
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        try {
            updatePlayer(apiGatewayProxyRequestEvent);
        } catch (BadRequestException ex) {
            return new APIGatewayProxyResponseEvent()
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(buildErrorResponse(ex.getMessage()))
                    .withStatusCode(400);
        } catch(ForbiddenException ex) {
            return new APIGatewayProxyResponseEvent()
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(buildErrorResponse(ex.getMessage()))
                    .withStatusCode(403);
        } catch(NotFoundException ex) {
            return new APIGatewayProxyResponseEvent()
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(buildErrorResponse(ex.getMessage()))
                    .withStatusCode(404);
        } catch (Exception ex) {
            return new APIGatewayProxyResponseEvent()
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(buildErrorResponse("Internal server error"))
                    .withStatusCode(500);
        }

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200);
    }

    private void updatePlayer(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent) {
        String username = extractUsername(apiGatewayProxyRequestEvent);
        String playerId = apiGatewayProxyRequestEvent.getPathParameters().get("playerId");
        UpdatePlayerRequest updatePlayerRequest = convertInput(apiGatewayProxyRequestEvent.getBody());

        validateUpdatePlayerRequest(updatePlayerRequest);

        Player player = playerRepository.findById(playerId);
        if (player == null) throw new NotFoundException("Player doesn't exist");

        Team team = teamRepository.findById(username);
        if (!team.getPlayersId().contains(playerId)) throw new ForbiddenException("Player doesn't belong to logged user's team");

        player.setCountry(updatePlayerRequest.getCountry());
        player.setFirstName(updatePlayerRequest.getFirstName());
        player.setLastName(updatePlayerRequest.getLastName());
        playerRepository.save(player);
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

    private UpdatePlayerRequest convertInput(String body) {
        try {
            return objectMapper.readValue(body, UpdatePlayerRequest.class);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid request body");
        }
    }

    private void validateUpdatePlayerRequest(UpdatePlayerRequest updatePlayerRequest) {
        validate(!StringUtils.isNullOrEmpty(updatePlayerRequest.getCountry()), "\"country\" field is empty");
        validate(!StringUtils.isNullOrEmpty(updatePlayerRequest.getFirstName()), "\"firstName\" field is empty");
        validate(!StringUtils.isNullOrEmpty(updatePlayerRequest.getLastName()), "\"lastName\" field is empty");
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
