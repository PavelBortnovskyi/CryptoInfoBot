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
import java.util.Set;
import java.util.stream.Collectors;

//@Log4j2
@Component
public class FavoriteCommandHandler extends BotCommand {

    private final ExchangeApiClient exchangeClient;

    private final TradingPairRepository tradingPairRepository;

    private final BotUserRepository botUserRepository;

    public FavoriteCommandHandler(@Value(TextCommands.GET_ALL_FAVORITE_PAIRS) String commandIdentifier,
                                  @Value(TextCommands.GAFP_DESCRIPTION) String description,
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
        long chatId = chat.getId();
        SendMessage messageToSend = SendMessage.builder().chatId(chatId).text("").build();
        if (botUserRepository.findById(chatId).isPresent()) {
            Set<TradingPair> userPairs = tradingPairRepository.getUsersFavoritePairs(chatId);
            if (!userPairs.isEmpty()) {
                messageToSend.setText(exchangeClient.getCurrency(userPairs.stream().map(TradingPair::getName).collect(Collectors.toList())));
                userPairs.forEach(p -> increasePairRate(p.getName()));
            } else messageToSend.setText("You no have favorite pair, please add them using /add_pair \"pair_name\" command");
        } else
            messageToSend.setText("You need to register by /start command to have possibility to get favorite pairs");
        try {
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
