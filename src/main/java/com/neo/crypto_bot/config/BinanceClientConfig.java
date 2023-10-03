package com.neo.crypto_bot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BinanceClientConfig {

    @Value("${binance.api.tickerPrice.url}")
    private String priceUrl;

    @Value("${binance.api.exchangeInfo.url}")
    private String exchangeInfoUrl;

    @Value("${binance.api.avgPrice.url}")
    private String avgPriceUrl;

    @Bean(name = "priceUrl")
    public String getPriceUrl() {
        return this.priceUrl;
    }

    @Bean(name = "exchangeInfoUrl")
    public String getExchangeInfoUrl() {
        return this.exchangeInfoUrl;
    }

    @Bean(name = "avgPriceUrl")
    public String getAvgPriceUrl() {
        return this.avgPriceUrl;
    }
}
