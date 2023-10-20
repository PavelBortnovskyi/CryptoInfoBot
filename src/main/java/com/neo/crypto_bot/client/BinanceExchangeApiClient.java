package com.neo.crypto_bot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neo.crypto_bot.model.TradingPair;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;


@Log4j2
@Component
@RequiredArgsConstructor
public class BinanceExchangeApiClient implements ExchangeApiClient {

    private final OkHttpClient okHttpClient;

    @Qualifier("priceUrl")
    private final String priceUrl;

    @Qualifier("avgPriceUrl")
    private final String avgPriceUrl;

    @Qualifier("exchangeInfoUrl")
    private final String exchangeInfoUrl;

    @Qualifier("convertibleUrl")
    private final String convertibleUrl;
    private final ObjectMapper objectMapper;

    public String getCurrency(List<String> pairs) {
        StringBuilder sb = new StringBuilder(priceUrl);
        sb.append(this.defineSymbolParam(pairs));

        JsonNode jsonNode = makeRequest(sb.toString());
        sb.replace(0, sb.toString().length(), "");
        if (jsonNode.has("msg"))
            sb.append(String.format("Got some error from server response: %s\n Please check your input.", jsonNode.get("msg")));
        else {
            int[] index = new int[1];
            index[0] = 1;
            if (jsonNode.isArray()) {
                sb.append("Current price(s) for pair(s):\n");
                jsonNode.forEach(n -> sb.append(String.format("%d) %s: %s", index[0]++, n.get("symbol"), n.get("price"))).append("\n"));
            } else if (jsonNode.isObject()) {
                sb.append("Current price(s) for pair(s):\n");
                sb.append(String.format("%d) %s: %s", index[0]++, jsonNode.get("symbol"), jsonNode.get("price")));
            }
        }
        return sb.toString();
    }

    public List<TradingPair> getListing() {
        List<TradingPair> assetsList = new ArrayList<>();
        JsonNode jsonNode = makeRequest(exchangeInfoUrl + "?permissions=SPOT");
        JsonNode rawAssetsList = jsonNode.get("symbols");
        rawAssetsList.forEach(a -> {
            TradingPair asset = new TradingPair();
            asset.setName(a.get("symbol").asText());
            asset.setBaseAsset(a.get("baseAsset").asText());
            asset.setQuoteAsset(a.get("quoteAsset").asText());
            asset.setRequests(0);
            assetsList.add(asset);
        });
        return assetsList;
    }

    public List<TradingPair> getConvertiblePairs(String name) {
        List<TradingPair> assetsList = new ArrayList<>();
        JsonNode jsonNode = makeRequest(convertibleUrl + "?fromAsset=" + name);
        jsonNode.forEach(a -> {
            TradingPair asset = new TradingPair();
            asset.setName(name + a.get("toAsset").asText());
            asset.setBaseAsset(a.get("fromAsset").asText());
            asset.setQuoteAsset(a.get("toAsset").asText());
            asset.setRequests(0);
            assetsList.add(asset);
        });
        return assetsList;
    }

    public boolean checkPair(List<String> pairs) {
        StringBuilder sb = new StringBuilder(priceUrl);
        sb.append(this.defineSymbolParam(pairs));

        JsonNode jsonNode = makeRequest(sb.toString());
        if (jsonNode.has("msg")) return false;
        else return true;
    }

    public TradingPair getPair(String name) {
        StringBuilder sb = new StringBuilder(exchangeInfoUrl);
        sb.append(this.defineSymbolParam(List.of(name)));

        JsonNode jsonNode = makeRequest(sb.toString());
        TradingPair pair = new TradingPair();
        pair.setName(name);
        pair.setBaseAsset(jsonNode.get("symbols").get("baseAsset").asText());
        pair.setQuoteAsset(jsonNode.get("symbols").get("quoteAsset").asText());
        pair.setLastCurrency(getPrice(name));
        return pair;
    }

    public Double getPrice(String name) {
        StringBuilder sb = new StringBuilder(priceUrl);
        sb.append(this.defineSymbolParam(List.of(name)));
        JsonNode jsonNode = makeRequest(sb.toString());
        return jsonNode.get("price").asDouble();
    }

    public HashMap<String, Double> getPrices(List<String> pairs){
        HashMap<String, Double> priceList = new HashMap<>();
        StringBuilder sb = new StringBuilder(priceUrl);
        sb.append(this.defineSymbolParam(pairs));
        JsonNode jsonNode = makeRequest(sb.toString());
        if (jsonNode.isArray()) {
            jsonNode.forEach(n -> priceList.put(n.get("symbol").asText(), n.get("price").asDouble()));
        } else if (jsonNode.isObject()) {
            priceList.put(jsonNode.get("symbol").asText(), jsonNode.get("price").asDouble());
        }
        return priceList;
    }

    public JsonNode makeRequest(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            String rawResponse = response.body() == null ? null : response.body().string();
            return objectMapper.readTree(rawResponse);
        } catch (IOException e) {
            log.error("Currencies receive error: " + e.getMessage());
            //throw new ServiceException("Currencies receive error: ", e);
            return null;
        }
    }

    private String defineSymbolParam(List<String> pairs) {
        StringBuilder sb = new StringBuilder();
        if (pairs.size() > 1) {
            sb.append("?symbols=[");
            pairs.forEach(s -> sb.append("\"" + s + "\","));
            sb.append("]");
            sb.replace(sb.lastIndexOf(","), sb.lastIndexOf(",") + 1, "");
        } else if (pairs.size() == 1) {
            sb.append("?symbol=").append(Arrays.stream(pairs.toArray()).findFirst().get());
        }
        return sb.toString();
    }
}
