package com.neo.crypto_bot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

@Log4j2
@Component
@RequiredArgsConstructor
public class BinanceClient {

    private final OkHttpClient okHttpClient;

    @Qualifier("priceUrl")
    private final String priceUrl;

    @Qualifier("avgPriceUrl")
    private final String avgPriceUrl;

    @Qualifier("exchangeInfoUrl")
    private final String exchangeInfoUrl;

    private final ObjectMapper objectMapper;

    public String getCurrency(Set<String> pairs) {
        StringBuilder sb = new StringBuilder(priceUrl);
        sb.append(this.defineSymbolParam(pairs));

        JsonNode jsonNode = makeRequest(sb.toString());
        sb.replace(0, sb.toString().length(), "");
        if (jsonNode.has("msg"))
            sb.append(String.format("Got some error from server response: %s\n Please check your input.", jsonNode.get("msg")));
        else {
            if (jsonNode.isArray()) {
                jsonNode.forEach(n -> sb.append(String.format("Current price of %s: %s", n.get("symbol"), n.get("price"))).append("\n"));
            } else if (jsonNode.isObject()) {
                sb.append(String.format("Current price of %s: %s", jsonNode.get("symbol"), jsonNode.get("price")));
            }
        }
        return sb.toString();
    }

    public boolean checkPairs(Set<String> pairs) {
        StringBuilder sb = new StringBuilder(priceUrl);
        sb.append(this.defineSymbolParam(pairs));

        JsonNode jsonNode = makeRequest(sb.toString());
        if (jsonNode.has("msg")) return false;
        else return true;
    }

    private JsonNode makeRequest(String url) {
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

    private String defineSymbolParam(Set<String> pairs) {
        StringBuilder sb = new StringBuilder();
        if (pairs.size() > 1) {
            sb.append("?symbols=[");
            pairs.forEach(s -> sb.append("\"" + s + "\","));
            sb.append("]");
            sb.replace(sb.lastIndexOf(","), sb.lastIndexOf(",") + 1, "");
        } else {
            sb.append("?symbol=").append(Arrays.stream(pairs.toArray()).findFirst().get());
        }
        return sb.toString();
    }
}
