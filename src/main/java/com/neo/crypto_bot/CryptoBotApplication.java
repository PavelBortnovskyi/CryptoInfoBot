package com.neo.crypto_bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.telegram.telegrambots.starter.TelegramBotStarterConfiguration;

@SpringBootApplication
@Import(TelegramBotStarterConfiguration.class)
public class CryptoBotApplication {
	public static void main(String[] args) {
		SpringApplication.run(CryptoBotApplication.class, args);
	}
}
