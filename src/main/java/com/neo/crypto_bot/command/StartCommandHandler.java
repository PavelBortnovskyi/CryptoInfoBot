package com.neo.crypto_bot.command;

import com.neo.crypto_bot.client.ExchangeApiClient;
import com.neo.crypto_bot.constant.TextCommands;
import com.neo.crypto_bot.model.BotUser;
import com.neo.crypto_bot.model.TradingPair;
import com.neo.crypto_bot.repository.BotUserRepository;
import com.neo.crypto_bot.repository.TradingPairRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@Component
public class StartCommandHandler extends BotCommand {

    private final ExchangeApiClient exchangeClient;

    private final TradingPairRepository tradingPairRepository;

    private final BotUserRepository botUserRepository;

    public StartCommandHandler(@Value(TextCommands.START) String commandIdentifier,
                               @Value(TextCommands.START_DESCRIPTION) String description,
                               ExchangeApiClient exchangeClient,
                               TradingPairRepository tradingPairRepository,
                               BotUserRepository botUserRepository) {
        super(commandIdentifier, description);
        this.exchangeClient = exchangeClient;
        this.tradingPairRepository = tradingPairRepository;
        this.botUserRepository = botUserRepository;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        StringBuilder sb = new StringBuilder("Hi, " + chat.getFirstName() + " , nice to meet you!\n");
        sb.append("This bot is created to get quick info and some statistic about trading pairs on Binance.\n");
        sb.append("Just write pair symbols to get currency (Example: BTCUSDT or few pairs: BTCUSDT, LTCUSDT).\n");
        sb.append("You can get more information with /help command");
        List<TradingPair> list = exchangeClient.getListing(); //TODO: remove from here
        tradingPairRepository.saveAll(list);
        SendMessage messageToSend = SendMessage.builder()
                .chatId(chat.getId())
                .text(sb.toString())
                .replyMarkup(getKeyboardWithTop25Pairs()).build();
        registerUser(user, chat.getId());
        try {
            absSender.execute(messageToSend);
        } catch (TelegramApiException e) {
            log.error("Got some exception in start block: " + e.getMessage());
        }
    }

    private ReplyKeyboardMarkup getKeyboardWithTop25Pairs() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        List<TradingPair> topPairs = tradingPairRepository.getPopularPairs();
        if (topPairs.size() == 25) {
            int rowIndex = 0;
            keyboardRows.add(new KeyboardRow());
            for (int i = 0; i < 25; i++) {
                if (keyboardRows.get(rowIndex).size() % 5 == 0) {
                    keyboardRows.add(new KeyboardRow());
                    rowIndex++;
                }
                keyboardRows.get(rowIndex).add(topPairs.get(i).getName());
            }
            keyboardMarkup.setKeyboard(keyboardRows);
            return keyboardMarkup;
        } else return null;
    }

    private void registerUser(User user, long chatId) {
        if (this.botUserRepository.findById(chatId).isEmpty()) {
            BotUser freshUser = new BotUser();
            freshUser.setId(chatId);
            freshUser.setNickName(user.getUserName());
            freshUser.setFirstName(user.getFirstName());
            freshUser.setLastName(user.getLastName());
            freshUser.setRegisteredAt(LocalDateTime.now());
            this.botUserRepository.save(freshUser);
            log.info(String.format("New User with chatId: %d registered", freshUser.getId()));
        }
    }
}