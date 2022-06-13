package com.luisguadagnin.soccermanager.clients;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClient;
import com.amazonaws.services.cognitoidp.model.AdminCreateUserRequest;
import com.amazonaws.services.cognitoidp.model.AdminGetUserRequest;
import com.amazonaws.services.cognitoidp.model.AdminSetUserPasswordRequest;
import com.amazonaws.services.cognitoidp.model.UserNotFoundException;
import com.luisguadagnin.soccermanager.configuration.AWSConfiguration;
import com.luisguadagnin.soccermanager.exception.UserAlreadyExistsException;

public class CognitoClient {

    private final AWSCognitoIdentityProvider awsCognitoIdentityProvider;

    public CognitoClient() {
        awsCognitoIdentityProvider = AWSCognitoIdentityProviderClient.builder()
                .withCredentials(AWSConfiguration.getAwsCredentialsProvider())
                .withRegion(AWSConfiguration.getAwsRegion())
                .build();
    }

    private boolean existsUserByEmail(String email) {
        AdminGetUserRequest adminGetUserRequest = new AdminGetUserRequest()
                .withUserPoolId(AWSConfiguration.getCognitoUserPoolId())
                .withUsername(email);
        try {
            awsCognitoIdentityProvider.adminGetUser(adminGetUserRequest);
            return true;
        } catch (UserNotFoundException ex) {
            return false;
        }
    }

    private void createUserWithTemporaryPassword(String email) {
        AdminCreateUserRequest adminCreateUserRequest = new AdminCreateUserRequest()
                .withUserPoolId(AWSConfiguration.getCognitoUserPoolId())
                .withUsername(email);
        awsCognitoIdentityProvider.adminCreateUser(adminCreateUserRequest);
    }

    private void setUserPassword(String email, String password) {
        AdminSetUserPasswordRequest adminSetUserPasswordRequest = new AdminSetUserPasswordRequest()
                .withUserPoolId(AWSConfiguration.getCognitoUserPoolId())
                .withUsername(email)
                .withPassword(password)
                .withPermanent(true);
        awsCognitoIdentityProvider.adminSetUserPassword(adminSetUserPasswordRequest);
    }

    public void createUser(String email, String password) {
        if (existsUserByEmail(email)) {
            throw new UserAlreadyExistsException();
        }

        createUserWithTemporaryPassword(email);

        setUserPassword(email, password);
    }

}
