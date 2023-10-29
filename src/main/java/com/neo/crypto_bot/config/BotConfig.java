package com.neo.crypto_bot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.CommandRegistry;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.ICommandRegistry;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.net.InetSocketAddress;
import java.net.Proxy;


@Getter
@Configuration
@EnableScheduling
public class BotConfig {


    @Value("${bot.token}")
    private String botToken;

    @Value("${bot.name}")
    private String botName;

    /**
     Initialization of bot
     */
//    @Bean
//    public TelegramBotsApi telegramBotsApi(CryptoInfoBot cryptoInfoBot) throws TelegramApiException {
//            TelegramBotsApi tBotApi = new TelegramBotsApi(DefaultBotSession.class);
//            tBotApi.registerBot(cryptoInfoBot);
//            return tBotApi;
//    }

//    @Bean
//    @Primary
//    public ICommandRegistry getICommandRegistry() {return new CommandRegistry(true, () -> botName);
//    }

    @Bean
    public OkHttpClient okHttpClient() {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("193.150.21.138", 8088));
        return new OkHttpClient().newBuilder().proxy(proxy).build();
    }


    @Bean
    public ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }
}
