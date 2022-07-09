package com.luisguadagnin.soccermanager.clients;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClient;
import com.amazonaws.services.cognitoidp.model.AdminInitiateAuthRequest;
import com.amazonaws.services.cognitoidp.model.AdminInitiateAuthResult;
import com.amazonaws.services.cognitoidp.model.AuthFlowType;
import com.luisguadagnin.soccermanager.configuration.AWSConfiguration;

import java.util.Map;

public class CognitoClient {

    private final AWSCognitoIdentityProvider awsCognitoIdentityProvider;

    public CognitoClient() {
        awsCognitoIdentityProvider = AWSCognitoIdentityProviderClient.builder()
                .withCredentials(AWSConfiguration.getAwsCredentialsProvider())
                .withRegion(AWSConfiguration.getAwsRegion())
                .build();
    }

    public String auth(String email, String password) {
        AdminInitiateAuthRequest adminInitiateAuthRequest = new AdminInitiateAuthRequest()
                .withUserPoolId(AWSConfiguration.getCognitoUserPoolId())
                .withClientId(AWSConfiguration.getCognitoUserPoolClientId())
                .withAuthFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
                .withAuthParameters(Map.of(
                        "USERNAME", email,
                        "PASSWORD", password
                ));

        AdminInitiateAuthResult adminInitiateAuthResult = awsCognitoIdentityProvider.adminInitiateAuth(adminInitiateAuthRequest);

        return adminInitiateAuthResult.getAuthenticationResult().getAccessToken();
    }

}
