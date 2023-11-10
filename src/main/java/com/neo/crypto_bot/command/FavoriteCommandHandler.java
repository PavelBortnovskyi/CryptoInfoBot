package com.neo.crypto_bot.command;

import com.neo.crypto_bot.client.ExchangeApiClient;
import com.neo.crypto_bot.constant.TextCommands;
import com.neo.crypto_bot.model.BotUser;
import com.neo.crypto_bot.model.TradingPair;
import com.neo.crypto_bot.repository.BotUserRepository;
import com.neo.crypto_bot.repository.TradingPairRepository;
import com.neo.crypto_bot.service.LocalizationManager;
import com.neo.crypto_bot.service.ReplyKeyboardFactory;
import com.vdurmont.emoji.EmojiParser;
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
import java.util.*;
import java.util.stream.Collectors;

//@Log4j2
@Component
public class FavoriteCommandHandler extends BotCommand {

    private final ExchangeApiClient exchangeClient;

    private final TradingPairRepository tradingPairRepository;

    private final BotUserRepository botUserRepository;

    private final ReplyKeyboardFactory replyKeyboardFactory;

    public FavoriteCommandHandler(@Value(TextCommands.GET_ALL_FAVORITE_PAIRS) String commandIdentifier,
                                  @Value(TextCommands.GAFP_DESCRIPTION) String description,
                                  ExchangeApiClient exchangeClient,
                                  TradingPairRepository tradingPairRepository,
                                  BotUserRepository botUserRepository,
                                  ReplyKeyboardFactory replyKeyboardFactory) {
        super(commandIdentifier, description);
        this.exchangeClient = exchangeClient;
        this.tradingPairRepository = tradingPairRepository;
        this.botUserRepository = botUserRepository;
        this.replyKeyboardFactory = replyKeyboardFactory;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        long chatId = chat.getId();
        SendMessage messageToSend = SendMessage.builder().chatId(chatId).text("").build();
        Optional<BotUser> currUser = botUserRepository.getUserWithFavoritePairs(chatId);
        if (currUser.isPresent()) {
            LocalizationManager.setLocale(new Locale(currUser.get().getLanguage().toLowerCase()));
            Set<TradingPair> userPairs = currUser.get().getFavorites();
            if (!userPairs.isEmpty()) {
                messageToSend.setText(EmojiParser.parseToUnicode(exchangeClient.getCurrency(userPairs.stream().map(TradingPair::getName).collect(Collectors.toList()), chatId)));
                userPairs.forEach(p -> increasePairRate(p.getName()));
            } else messageToSend.setText(LocalizationManager.getString("empty_favorites_list_message"));
        } else
            messageToSend.setText(LocalizationManager.getString("not_registered_message_3"));
        try {
            messageToSend.setReplyMarkup(replyKeyboardFactory.getKeyboardWithTop25Pairs());
            absSender.execute(messageToSend);
        } catch (
                TelegramApiException e) {
            //log.error("Got some exception in favorite pairs block: " + e.getMessage());
        }
    }

    private void increasePairRate(String tradingPairName) {
        if (tradingPairRepository.findByName(tradingPairName).isPresent())
            tradingPairRepository.increaseRate(tradingPairName);
    }
}
