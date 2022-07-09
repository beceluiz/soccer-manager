package com.luisguadagnin.soccermanager;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.luisguadagnin.soccermanager.dto.OffersQueryResponse;
import com.luisguadagnin.soccermanager.dto.SearchOffersRequest;
import com.luisguadagnin.soccermanager.model.Offer;
import com.luisguadagnin.soccermanager.model.Player;
import com.luisguadagnin.soccermanager.model.enums.PlayerPosition;
import com.luisguadagnin.soccermanager.repository.OfferRepository;
import com.luisguadagnin.soccermanager.repository.PlayerRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchOffersHandlerTest {

    private final OfferRepository offerRepository;
    private final PlayerRepository playerRepository;
    private final SearchOffersHandler searchOffersHandler;

    public SearchOffersHandlerTest() {
        this.offerRepository = mock(OfferRepository.class);
        this.playerRepository = mock(PlayerRepository.class);
        this.searchOffersHandler = new SearchOffersHandler(playerRepository, offerRepository);
    }

    @Test
    public void shouldSearchOffersSuccessfully() {
        APIGatewayProxyRequestEvent input = new APIGatewayProxyRequestEvent();
        String offerId = "offer-id";

        Player player = Player.builder()
                .id(offerId)
                .position(PlayerPosition.MIDFIELDER)
                .country("Brazil")
                .teamId("team-id")
                .value("2000000.00")
                .firstName("Jay")
                .lastName("Cutler")
                .age(35)
                .build();

        Offer offer = Offer.builder()
                .id(offerId)
                .price(150000000)
                .discount(2500)
                .country("Brazil")
                .position(PlayerPosition.MIDFIELDER)
                .build();

        when(offerRepository.findByQuery(any(SearchOffersRequest.class)))
                .thenReturn(OffersQueryResponse.builder()
                        .offers(List.of(offer))
                        .build());
        when(playerRepository.findById(List.of(offerId)))
                .thenReturn(List.of(player));

        APIGatewayProxyResponseEvent output = searchOffersHandler.handleRequest(input, null);

        assertAll(
                () -> assertEquals("{\"offers\":[{\"id\":\"offer-id\",\"price\":\"1500000.00\",\"discount\":\"25.00\",\"player\":" +
                                "{\"id\":\"offer-id\",\"firstName\":\"Jay\",\"lastName\":\"Cutler\",\"country\":\"Brazil\",\"age\":35,\"value\":\"2000000.00\",\"position\":\"MIDFIELDER\"}}]}",
                        output.getBody()),
                () -> assertEquals(200, output.getStatusCode())
        );
        System.out.println(output.getBody());
    }
}
