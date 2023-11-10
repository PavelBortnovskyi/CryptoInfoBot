package com.neo.crypto_bot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.neo.crypto_bot.model.BotUser;
import com.neo.crypto_bot.model.TradingPair;
import com.neo.crypto_bot.repository.BotUserRepository;
import com.neo.crypto_bot.service.LocalizationManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;


//@Log4j2
@Component
@RequiredArgsConstructor
public class BinanceExchangeApiClient implements ExchangeApiClient {

    private final OkHttpClient okHttpClient;

    private final BotUserRepository botUserRepository;

    @Qualifier("priceUrl")
    private final String priceUrl;

    @Qualifier("dayTickerPriceUrl")
    private final String dayTickerPriceUrl;

    @Qualifier("avgPriceUrl")
    private final String avgPriceUrl;

    @Qualifier("exchangeInfoUrl")
    private final String exchangeInfoUrl;

    @Qualifier("convertibleUrl")
    private final String convertibleUrl;
    private final ObjectMapper objectMapper;

    public String getCurrency(List<String> pairs, long userId) {
        StringBuilder sb = new StringBuilder(dayTickerPriceUrl);
        sb.append(this.defineSymbolParam(pairs));
        botUserRepository.findById(userId).ifPresent(botUser -> LocalizationManager.setLocale(new Locale(botUser.getLanguage().toLowerCase())));

        JsonNode jsonNode = makeRequest(sb.toString());
        sb.replace(0, sb.toString().length(), "");
        if (jsonNode.has("msg"))
            sb.append(MessageFormat.format(LocalizationManager.getString("binance_get_currency_error"), jsonNode.get("msg")));
        else {
            int[] index = new int[1];
            index[0] = 1;
            DecimalFormat df = new DecimalFormat("#.#########");
            ArrayNode arrayNode = objectMapper.createArrayNode();
            if (jsonNode.isArray()) {
                arrayNode = (ArrayNode) jsonNode;
                sb.append(LocalizationManager.getString("current_prices_head_message")).append("\n");
            } else if (jsonNode.isObject()) {
                arrayNode.add(jsonNode);
                sb.append("current_price_head_message").append("\n");
            }
            arrayNode.forEach(node -> {
                String symbol = node.get("symbol").asText().replace("\"", "");
                String price = df.format(node.get("lastPrice").asDouble());
                Double delta = node.get("priceChangePercent").asDouble();
                String direction = delta > 20 ? ":rocket:" : (delta > 0 ? ":chart_with_upwards_trend:" : ":chart_with_downwards_trend:");
                sb.append(String.format("%02d) %-10s:   %-10s  (%.2f%%) %s%n", index[0]++, symbol, price, delta, direction));
            });
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

    public Double getDayDeviation(String name) {
        StringBuilder sb = new StringBuilder(dayTickerPriceUrl);
        sb.append(this.defineSymbolParam(List.of(name)));
        JsonNode jsonNode = makeRequest(sb.toString());
        return jsonNode.get("priceChangePercent").asDouble();
    }

    public HashMap<String, Double> getPrices(List<String> pairs) {
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

    public HashMap<String, Double> getPricesDayDeviation(List<String> pairs) {
        HashMap<String, Double> priceList = new HashMap<>();
        StringBuilder sb = new StringBuilder(dayTickerPriceUrl);
        sb.append(this.defineSymbolParam(pairs));
        JsonNode jsonNode = makeRequest(sb.toString());
        if (jsonNode.isArray()) {
            jsonNode.forEach(n -> priceList.put(n.get("symbol").asText(), n.get("priceChangePercent").asDouble()));
        } else if (jsonNode.isObject()) {
            priceList.put(jsonNode.get("symbol").asText(), jsonNode.get("priceChangePercent").asDouble());
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
            //log.error("Currencies receive error: " + e.getMessage());
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


