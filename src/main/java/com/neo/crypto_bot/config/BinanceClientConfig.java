package com.neo.crypto_bot.config;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BinanceClientConfig {

    @Value("${binance_api_tickerPrice_url}")
    //@Value("${BINANCE_TICKER_PRICE_URL}")
    private String priceUrl;

    @Value("${binance_api_exchangeInfo_url}")
    //@Value("${BINANCE_EXCHANGE_INFO_URL}")
    private String exchangeInfoUrl;

    @Value("${binance_api_avgPrice_url}")
    //@Value("${BINANCE_AVG_PRICE_URL}")
    private String avgPriceUrl;

    @Value("${binance_api_convertible_url}")
    //@Value("${BINANCE_CONVERTIBLE_URL}")
    private String convertibleUrl;

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

    @Bean(name = "convertibleUrl")
    public String getConvertibleUrl() {
        return this.convertibleUrl;
    }
}
