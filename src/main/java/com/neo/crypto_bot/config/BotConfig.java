package com.neo.crypto_bot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neo.crypto_bot.service.CryptoInfoBot;
import lombok.Getter;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Getter
@Configuration
public class BotConfig {


    @Value("${bot.token}")
    private String botToken;

    @Value("${bot.name}")
    private String botName;

    /**
     Initialization of bot
     */
    @Bean
    public TelegramBotsApi telegramBotsApi(CryptoInfoBot cryptoInfoBot) throws TelegramApiException {
            TelegramBotsApi tBotApi = new TelegramBotsApi(DefaultBotSession.class);
            tBotApi.registerBot(cryptoInfoBot);
            return tBotApi;
    }

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient();
    }


    @Bean
    public ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }
}
