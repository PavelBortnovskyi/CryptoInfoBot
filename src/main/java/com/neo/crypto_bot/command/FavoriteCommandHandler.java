package com.neo.crypto_bot.command;

import com.neo.crypto_bot.client.ExchangeApiClient;
import com.neo.crypto_bot.constant.TextCommands;
import com.neo.crypto_bot.model.BotUser;
import com.neo.crypto_bot.model.TradingPair;
import com.neo.crypto_bot.repository.BotUserRepository;
import com.neo.crypto_bot.repository.TradingPairRepository;
import com.neo.crypto_bot.service.ImageFactory;
import com.neo.crypto_bot.service.LocalizationManager;
import com.neo.crypto_bot.service.ReplyKeyboardFactory;
import com.vdurmont.emoji.EmojiParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

//@Log4j2
@Component
public class FavoriteCommandHandler extends BotCommand {

    private final ExchangeApiClient exchangeClient;

    private final TradingPairRepository tradingPairRepository;

    private final BotUserRepository botUserRepository;

    private final ReplyKeyboardFactory replyKeyboardFactory;

    private final ImageFactory imageFactory;

    public FavoriteCommandHandler(@Value(TextCommands.GET_ALL_FAVORITE_PAIRS) String commandIdentifier,
                                  @Value(TextCommands.GAFP_DESCRIPTION) String description,
                                  ExchangeApiClient exchangeClient,
                                  TradingPairRepository tradingPairRepository,
                                  BotUserRepository botUserRepository,
                                  ReplyKeyboardFactory replyKeyboardFactory,
                                  ImageFactory imageFactory) {
        super(commandIdentifier, description);
        this.exchangeClient = exchangeClient;
        this.tradingPairRepository = tradingPairRepository;
        this.botUserRepository = botUserRepository;
        this.replyKeyboardFactory = replyKeyboardFactory;
        this.imageFactory = imageFactory;
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
                try {
                    String answer = EmojiParser.parseToUnicode(exchangeClient.getCurrency(userPairs.stream().map(TradingPair::getName).collect(Collectors.toList()), chatId));
                    if (userPairs.size() > 1) {
                        SendPhoto photoToSend = SendPhoto.builder()
                                .chatId(chatId)
                                .replyMarkup(replyKeyboardFactory.getKeyboardWithTop25Pairs())
                                .photo(imageFactory.createImageWithText(answer, 86, userPairs.size() <= 17 ? 0 : 1))
                                .build();
                        absSender.execute(photoToSend);
                    } else messageToSend.setText(answer);
                } catch (IOException | TelegramApiException e) {
                    System.out.println("Got some exception in favorite pairs block: " + e.getMessage());
                }
                //messageToSend.setText(EmojiParser.parseToUnicode(exchangeClient.getCurrency(userPairs.stream().map(TradingPair::getName).collect(Collectors.toList()), chatId)));
                userPairs.forEach(p -> increasePairRate(p.getName()));
            } else messageToSend.setText(LocalizationManager.getString("empty_favorites_list_message"));
        } else
            messageToSend.setText(LocalizationManager.getString("not_registered_message_3"));
        try {
            messageToSend.setReplyMarkup(replyKeyboardFactory.getKeyboardWithTop25Pairs());
            if (!messageToSend.getText().isEmpty()) absSender.execute(messageToSend);
        } catch (
                TelegramApiException e) {
            System.out.println("Got some exception in favorite pairs block: " + e.getMessage());
            //log.error("Got some exception in favorite pairs block: " + e.getMessage());
        }
    }

    private void increasePairRate(String tradingPairName) {
        if (tradingPairRepository.findByName(tradingPairName).isPresent())
            tradingPairRepository.increaseRate(tradingPairName);
    }
}
