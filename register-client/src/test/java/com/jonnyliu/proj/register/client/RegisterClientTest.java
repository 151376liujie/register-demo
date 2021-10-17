package com.jonnyliu.proj.register.client;

import org.junit.Before;
import org.junit.Test;

public class RegisterClientTest {

    private static final Long ONE_SECOND = 1000L;

    private RegisterClient registerClient;

    @Before
    public void setUp() {
        registerClient = new RegisterClient();
    }

    @Test
    public void start() throws Exception {
        registerClient.start();

        Thread.sleep(35 * ONE_SECOND);
        registerClient.shutdown();
    }
}