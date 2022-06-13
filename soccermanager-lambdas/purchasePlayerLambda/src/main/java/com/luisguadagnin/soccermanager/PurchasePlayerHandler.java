package com.luisguadagnin.soccermanager;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luisguadagnin.soccermanager.dto.PurchasePlayerRequest;
import com.luisguadagnin.soccermanager.exception.BadRequestException;
import com.luisguadagnin.soccermanager.exception.NotFoundException;
import com.luisguadagnin.soccermanager.model.Offer;
import com.luisguadagnin.soccermanager.model.Player;
import com.luisguadagnin.soccermanager.model.Team;
import com.luisguadagnin.soccermanager.repository.OfferRepository;
import com.luisguadagnin.soccermanager.repository.PlayerRepository;
import com.luisguadagnin.soccermanager.repository.TeamRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

public class PurchasePlayerHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper;
    private final PlayerRepository playerRepository;
    private final OfferRepository offerRepository;
    private final TeamRepository teamRepository;

    public PurchasePlayerHandler() {
        this.objectMapper = new ObjectMapper();
        this.playerRepository = new PlayerRepository();
        this.offerRepository = new OfferRepository();
        this.teamRepository = new TeamRepository();
    }

    PurchasePlayerHandler(PlayerRepository playerRepository, OfferRepository offerRepository, TeamRepository teamRepository) {
        this.objectMapper = new ObjectMapper();
        this.playerRepository = playerRepository;
        this.offerRepository = offerRepository;
        this.teamRepository = teamRepository;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        try {
            purchasePlayer(apiGatewayProxyRequestEvent);
        } catch (BadRequestException ex) {
            return new APIGatewayProxyResponseEvent()
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(buildErrorResponse(ex.getMessage()))
                    .withStatusCode(400);
        } catch (NotFoundException ex) {
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

    private void purchasePlayer(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent) {
        String loggedUser = extractUsername(apiGatewayProxyRequestEvent);
        PurchasePlayerRequest purchasePlayerRequest = buildRequestFromBody(apiGatewayProxyRequestEvent);
        validateRequest(purchasePlayerRequest);

        Player purchasedPlayer = findPlayerById(purchasePlayerRequest.getPlayerId());
        Team originalTeam = findTeamById(purchasedPlayer.getTeamId());
        Team newTeam = findTeamById(loggedUser);
        validateDistinctTeams(originalTeam, newTeam);
        Offer offer = findOfferById(purchasedPlayer.getId());

        updateTeamBudgets(originalTeam, newTeam, offer);
        updatePlayerTeam(purchasedPlayer, originalTeam, newTeam);
        updateOriginalTeamValue(originalTeam, purchasedPlayer);
        updatePlayerValue(purchasedPlayer);
        updateNewTeamValue(newTeam, purchasedPlayer);

        offerRepository.delete(offer);
        teamRepository.save(originalTeam);
        teamRepository.save(newTeam);
        playerRepository.save(purchasedPlayer);
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

    private PurchasePlayerRequest buildRequestFromBody(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent) {
        String bodyStr = apiGatewayProxyRequestEvent.getBody();
        try {
            return objectMapper.readValue(bodyStr, PurchasePlayerRequest.class);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Invalid request");
        }
    }

    private void validateRequest(PurchasePlayerRequest purchasePlayerRequest) {
        if (StringUtils.isNullOrEmpty(purchasePlayerRequest.getPlayerId())) {
            throw new BadRequestException("\"playerId\" field is empty");
        }
    }

    private Player findPlayerById(String playerId) {
        return Optional
                .ofNullable(playerRepository.findById(playerId))
                .orElseThrow(() -> new NotFoundException("Player not found")); // converts into 404
    }

    private Team findTeamById(String teamId) {
        return Optional
                .ofNullable(teamRepository.findById(teamId))
                .orElseThrow(() -> new RuntimeException("Team not found")); // converts into 500
    }

    private Offer findOfferById(String offerId) {
        return Optional
                .ofNullable(offerRepository.findById(offerId))
                .orElseThrow(() -> new BadRequestException("No offer found for this player")); // converts into 400
    }

    private void validateDistinctTeams(Team originalTeam, Team newTeam) {
        if (Objects.equals(originalTeam.getId(), newTeam.getId())) {
            throw new BadRequestException("Cannot buy a player from your own team");
        }
    }

    private void updateTeamBudgets(Team originalTeam, Team newTeam, Offer offer) {
        BigDecimal purchaseCost = new BigDecimal(offer.getPrice()).divide(new BigDecimal("100"), 2, RoundingMode.UNNECESSARY);

        /* Updates new team budget */
        BigDecimal newTeamBudget = new BigDecimal(newTeam.getBudget()).setScale(2, RoundingMode.UNNECESSARY);
        if (newTeamBudget.compareTo(purchaseCost) < 0) {
            throw new BadRequestException("Not enough budget to purchase player");
        }
        BigDecimal newNewTeamBudget = newTeamBudget.subtract(purchaseCost);
        newTeam.setBudget(newNewTeamBudget.toString());

        /* Updates original team budget */
        BigDecimal originalTeamBudget = new BigDecimal(originalTeam.getBudget()).setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal newOriginalTeamBudget = originalTeamBudget.add(purchaseCost);
        originalTeam.setBudget(newOriginalTeamBudget.toString());
    }

    private void updatePlayerTeam(Player purchasedPlayer, Team originalTeam, Team newTeam) {
        purchasedPlayer.setTeamId(newTeam.getId());
        originalTeam.getPlayersId().remove(purchasedPlayer.getId());
        newTeam.getPlayersId().add(purchasedPlayer.getId());
    }

    private void updateOriginalTeamValue(Team originalTeam, Player purchasedPlayer) {
        BigDecimal playerValue = new BigDecimal(purchasedPlayer.getValue());
        BigDecimal teamValue = new BigDecimal(originalTeam.getValue());
        BigDecimal newTeamValue = teamValue.subtract(playerValue);
        originalTeam.setValue(newTeamValue.toString());
    }

    private void updatePlayerValue(Player purchasedPlayer) {
        BigDecimal oldValue = new BigDecimal(purchasedPlayer.getValue());
        int percentageIncrease = generateRandomNumberBetween(10, 100);
        BigDecimal multiplier = new BigDecimal(percentageIncrease)
                .divide(new BigDecimal(100), 2, RoundingMode.UNNECESSARY)
                .add(BigDecimal.ONE);
        BigDecimal newValue = oldValue.multiply(multiplier).setScale(2, RoundingMode.HALF_DOWN);
        purchasedPlayer.setValue(newValue.toString());
    }

    private void updateNewTeamValue(Team newTeam, Player purchasedPlayer) {
        BigDecimal playerValue = new BigDecimal(purchasedPlayer.getValue());
        BigDecimal teamValue = new BigDecimal(newTeam.getValue());
        BigDecimal newTeamValue = teamValue.add(playerValue);
        newTeam.setValue(newTeamValue.toString());
    }

    private int generateRandomNumberBetween(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
    }

    private String buildErrorResponse(String message) {
        return "{ \"message\": \"" + message + "\" }";
    }

}
