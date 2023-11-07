package com.neo.crypto_bot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.neo.crypto_bot.model.TradingPair;

import java.util.HashMap;
import java.util.List;


public interface ExchangeApiClient {

    boolean checkPair(List<String> pairSet);

    JsonNode makeRequest(String url);

    String getCurrency(List<String> pairs);

    public List<TradingPair> getListing();

    public List<TradingPair> getConvertiblePairs(String name);

    public TradingPair getPair(String name);

    public Double getPrice(String name);

    public Double getDayDeviation(String name);

    public HashMap<String, Double> getPrices(List<String> pairSet);

    public HashMap<String, Double> getPricesDayDeviation(List<String> pairs);
}
