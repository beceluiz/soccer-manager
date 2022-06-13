package com.luisguadagnin.soccermanager;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luisguadagnin.soccermanager.dto.PlayerResponse;
import com.luisguadagnin.soccermanager.dto.TeamResponse;
import com.luisguadagnin.soccermanager.model.Player;
import com.luisguadagnin.soccermanager.model.Team;
import com.luisguadagnin.soccermanager.repository.PlayerRepository;
import com.luisguadagnin.soccermanager.repository.TeamRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GetTeamHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    public GetTeamHandler() {
        objectMapper = new ObjectMapper();
        teamRepository = new TeamRepository();
        playerRepository = new PlayerRepository();
    }

    GetTeamHandler(TeamRepository teamRepository, PlayerRepository playerRepository) {
        objectMapper = new ObjectMapper();
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        String response;
        try {
            response = createTeam(apiGatewayProxyRequestEvent);
        }  catch (Exception ex) {
            return new APIGatewayProxyResponseEvent()
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(buildErrorResponse("Internal server error"))
                    .withStatusCode(500);
        }

        return new APIGatewayProxyResponseEvent()
                .withBody(response)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withStatusCode(200);
    }

    private String createTeam(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent) throws JsonProcessingException {
        String username = extractUsername(apiGatewayProxyRequestEvent);

        Team team = teamRepository.findById(username);
        List<Player> players = playerRepository.findById(team.getPlayersId());

        TeamResponse teamResponse = buildTeamResponse(team, players);
        return objectMapper.writeValueAsString(teamResponse);
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

    private TeamResponse buildTeamResponse(Team team, List<Player> players) {
        return TeamResponse.builder()
                .id(team.getId())
                .budget(team.getBudget())
                .country(team.getCountry())
                .name(team.getName())
                .value(team.getValue())
                .players(players.stream().map(this::buildPlayerResponse).collect(Collectors.toList()))
                .build();
    }

    private PlayerResponse buildPlayerResponse(Player player) {
        return PlayerResponse.builder()
                .id(player.getId())
                .position(player.getPosition().toString())
                .age(player.getAge())
                .country(player.getCountry())
                .firstName(player.getFirstName())
                .lastName(player.getLastName())
                .value(player.getValue())
                .build();
    }

    private String buildErrorResponse(String message) {
        return "{ \"message\": \"" + message + "\" }";
    }

}
