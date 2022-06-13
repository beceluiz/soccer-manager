package com.luisguadagnin.soccermanager;


import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class SoccermanagerCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        new SoccermanagerCdkStack(app, "SoccermanagerCdkStack", StackProps.builder()
                .env(Environment.builder()
                        .account("241230872468")
                        .region("us-east-1")
                        .build())
                .build());

        app.synth();
    }
}

