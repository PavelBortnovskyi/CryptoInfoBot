package com.neo.crypto_bot.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;


public interface ExchangeApiClient {

    boolean checkPair(List<String> pairSet);

    JsonNode makeRequest(String url);

    String getCurrency(List<String> pairs);
}
