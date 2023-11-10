package com.neo.crypto_bot.command;

import com.neo.crypto_bot.client.ExchangeApiClient;
import com.neo.crypto_bot.config.BotStateKeeper;
import com.neo.crypto_bot.constant.Actions;
import com.neo.crypto_bot.constant.BotState;
import com.neo.crypto_bot.constant.TextCommands;
import com.neo.crypto_bot.model.BotUser;
import com.neo.crypto_bot.repository.BotUserRepository;
import com.neo.crypto_bot.repository.TradingPairRepository;
import com.neo.crypto_bot.service.ListInitializer;
import com.neo.crypto_bot.service.ReplyKeyboardFactory;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.List;

//@Log4j2
@Component
public class StartCommandHandler extends BotCommand {

    private final ExchangeApiClient exchangeClient;

    private final TradingPairRepository tradingPairRepository;

    private final BotUserRepository botUserRepository;

    private final ReplyKeyboardFactory replyKeyboardFactory;

    private final BotStateKeeper botStateKeeper;

    private final ListInitializer listInitializer;

    public StartCommandHandler(@Value(TextCommands.START) String commandIdentifier,
                               @Value(TextCommands.START_DESCRIPTION) String description,
                               ExchangeApiClient exchangeClient,
                               TradingPairRepository tradingPairRepository,
                               BotUserRepository botUserRepository,
                               ReplyKeyboardFactory replyKeyboardFactory,
                               BotStateKeeper botStateKeeper,
                               ListInitializer listInitializer) {
        super(commandIdentifier, description);
        this.exchangeClient = exchangeClient;
        this.tradingPairRepository = tradingPairRepository;
        this.botUserRepository = botUserRepository;
        this.replyKeyboardFactory = replyKeyboardFactory;
        this.botStateKeeper = botStateKeeper;
        this.listInitializer = listInitializer;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        StringBuilder sb = new StringBuilder("Hi, " + chat.getFirstName() + " , nice to meet you!\n\n");
        sb.append("This bot is created to get quick info and some statistic about trading pairs on Binance.\n\n");
        sb.append("Just write pair symbols to get currency (Example: BTCUSDT or few pairs: BTCUSDT, LTCUSDT).\n\n");
        sb.append("--OR--\n\n");
        sb.append("You can also write only 1 asset and you will get possible quote assets to make pair (Example: BTC).\n\n");
        sb.append("--OR--\n\n");
        sb.append("You can choose from top 25 pairs from reply keyboard below\n\n");
        sb.append("--OR--\n\n");
        sb.append("You can get more information with /help command");
        if (botStateKeeper.getBotState().equals(BotState.INITIALIZATION) && tradingPairRepository.count() == 0)
            listInitializer.saveEntitiesInBatch(exchangeClient.getListing());
        botStateKeeper.changeState(BotState.INPUT_FOR_CURRENCY);
        InlineKeyboardMarkup langOptions = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder()
                                .text(EmojiParser.parseToUnicode("English \uD83C\uDDEC\uD83C\uDDE7/\uD83C\uDDFA\uD83C\uDDF8"))
                                .callbackData(Actions.ENG_LANGUAGE)
                                .build(),
                        InlineKeyboardButton.builder()
                                .text(EmojiParser.parseToUnicode("Ukrainian \uD83C\uDDFA\uD83C\uDDE6"))
                                .callbackData(Actions.UA_LANGUAGE)
                                .build()
                ))
                .build();
        SendMessage messageToSend = SendMessage.builder()
                .chatId(chat.getId())
                .text(sb.toString())
                .replyMarkup(langOptions)
                .build();
        registerUser(user, chat.getId());
        try {
            absSender.execute(messageToSend);
        } catch (TelegramApiException e) {
            System.out.println("Got some exception in start block: " + e.getMessage());
            //log.error("Got some exception in start block: " + e.getMessage());
        }
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
            //log.info(String.format("New User with chatId: %d registered", freshUser.getId()));
        }
    }
}