package com.luisguadagnin.soccermanager.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.luisguadagnin.soccermanager.configuration.AWSConfiguration;
import com.luisguadagnin.soccermanager.model.Offer;

public class OfferRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public OfferRepository() {
        dynamoDBMapper = AWSConfiguration.getDynamoDBMapper();
    }

    public Offer findById(String id) {
        return dynamoDBMapper.load(Offer.class, id);
    }

    public void delete(Offer offer) {
        dynamoDBMapper.delete(offer);
    }

}
