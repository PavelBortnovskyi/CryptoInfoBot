package com.neo.crypto_bot.command;

import com.neo.crypto_bot.client.ExchangeApiClient;
import com.neo.crypto_bot.config.BotStateKeeper;
import com.neo.crypto_bot.constant.BotState;
import com.neo.crypto_bot.constant.TextCommands;
import com.neo.crypto_bot.model.BotUser;
import com.neo.crypto_bot.model.TradingPair;
import com.neo.crypto_bot.repository.BotUserRepository;
import com.neo.crypto_bot.repository.TradingPairRepository;
import com.neo.crypto_bot.service.CommandParser;
import com.neo.crypto_bot.service.ReplyKeyboardFactory;
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

import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

//@Log4j2
@Component
public class AddPairCommandHandler extends BotCommand {

    private final ExchangeApiClient exchangeClient;

    private final TradingPairRepository tradingPairRepository;

    private final BotUserRepository botUserRepository;

    private final ReplyKeyboardFactory replyKeyboardFactory;

    private final CommandParser commandParser;

    private final BotStateKeeper botStateKeeper;

    public AddPairCommandHandler(@Value(TextCommands.ADD_PAIR) String commandIdentifier,
                                 @Value(TextCommands.AP_DESCRIPTION) String description,
                                 ExchangeApiClient exchangeClient,
                                 TradingPairRepository tradingPairRepository,
                                 BotUserRepository botUserRepository,
                                 ReplyKeyboardFactory replyKeyboardFactory,
                                 CommandParser commandParser,
                                 BotStateKeeper botStateKeeper) {
        super(commandIdentifier, description);
        this.exchangeClient = exchangeClient;
        this.tradingPairRepository = tradingPairRepository;
        this.botUserRepository = botUserRepository;
        this.replyKeyboardFactory = replyKeyboardFactory;
        this.commandParser = commandParser;
        this.botStateKeeper = botStateKeeper;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        SendMessage messageToSend = SendMessage.builder()
                .chatId(chat.getId())
                .text("")
                .replyMarkup(replyKeyboardFactory.getKeyboardWithTop25Pairs()).build();
        Optional<BotUser> maybeCurrUser = botUserRepository.getUserWithFavoritePairs(chat.getId());
        if (maybeCurrUser.isPresent()) {
            BotUser currUser = maybeCurrUser.get();
            if (strings.length >= 1) {
                List<String> pairsToAdd = new ArrayList<>();
                Arrays.stream(strings).forEach(s -> pairsToAdd.addAll(commandParser.getPairsFromCommand(s)));
                if (exchangeClient.checkPair(pairsToAdd)) {
                    List<String> duplicates = new ArrayList<>();
                    pairsToAdd.forEach(p -> {
                        if (currUser.getFavorites().stream().filter(fp -> fp.getName().equals(p)).collect(Collectors.toList()).isEmpty()) {
                            if (tradingPairRepository.findByName(p).isPresent()) {
                                tradingPairRepository.updatePrice(p, exchangeClient.getPrice(p));
                                currUser.getFavorites().add(tradingPairRepository.findByName(p).get());
                            } else {
                                TradingPair pairToAdd = exchangeClient.getPair(p);
                                tradingPairRepository.save(pairToAdd);
                                currUser.getFavorites().add(pairToAdd);
                            }
                        } else duplicates.add(p);
                    });
                    botUserRepository.save(currUser);
                    if (duplicates.isEmpty()) messageToSend.setText(pairsToAdd + " was added to your favorites!");
                    else messageToSend.setText(duplicates + " is already in your favorites!");
                } else messageToSend.setText("Wrong pairs symbol input. Please check and try again");
            } else {
                StringBuilder sb = new StringBuilder("You should use this command in \"/add_pair BTCUSDT\" format\n");
                sb.append("or \"/add_pair BTCUSDT, LTCUSDT\" to add few pairs to favorites\n").append("\n");
                sb.append("Also you can choose pair to add in reply keyboard below from top 25 pairs\n\n");
                botStateKeeper.changeState(BotState.INPUT_FOR_ADD);
                messageToSend.setText(sb.toString());
            }
        } else
            messageToSend.setText("You are not registered user and can`t add pairs to favorites. Click /start to register.");
        try {
            absSender.execute(messageToSend);
        } catch (TelegramApiException e) {
            //log.error("Got some exception in start block: " + e.getMessage());
        }
    }
}