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
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

//@Log4j2
@Component
public class RemovePairCommandHandler extends BotCommand {

    private final ExchangeApiClient exchangeClient;

    private final TradingPairRepository tradingPairRepository;

    private final BotUserRepository botUserRepository;

    private final ReplyKeyboardFactory replyKeyboardFactory;

    private final CommandParser commandParser;

    private final BotStateKeeper botStateKeeper;

    public RemovePairCommandHandler(@Value(TextCommands.REMOVE_PAIR) String commandIdentifier,
                                    @Value(TextCommands.RP_DESCRIPTION) String description,
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
                .build();
        Optional<BotUser> maybeCurrUser = botUserRepository.findById(chat.getId());
        if (maybeCurrUser.isPresent()) {
            BotUser currUser = maybeCurrUser.get();
            if (strings.length >= 1) {
                List<String> pairsToRemove = new ArrayList<>();
                Arrays.stream(strings).forEach(s -> pairsToRemove.addAll(commandParser.getPairsFromCommand(s)));
                List<String> errors = new ArrayList<>();
                if (exchangeClient.checkPair(pairsToRemove)) {
                    pairsToRemove.forEach(p -> {
                        TradingPair pairToRemove = tradingPairRepository.findByName(p).orElse(new TradingPair());
                        if (currUser.getFavorites().contains(pairToRemove)) {
                            currUser.getFavorites().remove(pairToRemove);
                            botUserRepository.save(currUser);
                        } else {
                            errors.add(p);
                        }
                    });
                    StringBuilder sb = new StringBuilder();
                    if (!pairsToRemove.stream().filter(p -> !errors.contains(p)).toList().isEmpty()) {
                        sb.append(pairsToRemove.stream().filter(p -> !errors.contains(p)).toList()).append(" was removed from your favorites!\n");
                    } else {
                        sb.append("Nothing to remove!\n");
                    }
                    if (!errors.isEmpty()) sb.append("NOTE: pairs that is not in your favorites: ").append(errors);
                    messageToSend.setText(sb.toString());
                } else {
                    messageToSend.setText("Wrong pairs symbol input. Please check and try again");
                }
            } else {
                StringBuilder sb = new StringBuilder("You can use this command in \"/remove_pair BTCUSDT\" format\n");
                sb.append("or \"/remove_pair BTCUSDT, LTCUSDT\" to remove few pairs from favorites\n\n");
                sb.append("Also you can choose pair to remove in reply keyboard below if you have favorites\n");
                sb.append("or you can print it manually\n");
                if (!botUserRepository.findById(chat.getId()).get().getFavorites().isEmpty())
                    messageToSend.setReplyMarkup(replyKeyboardFactory.getKeyboardWithFavorites(chat.getId()));
                else messageToSend.setReplyMarkup(replyKeyboardFactory.getKeyboardWithTop25Pairs());
                messageToSend.setText(sb.toString());
                botStateKeeper.changeState(BotState.INPUT_FOR_REMOVE);
            }
        } else
            messageToSend.setText("You are not registered user and can`t add pairs to favorites. Click /start to register.");
        try {
            System.out.println(messageToSend.getReplyMarkup().toString());
            absSender.execute(messageToSend);
        } catch (TelegramApiException e) {
            System.out.println("Got some exception in remove pair block: " + e.getMessage());
            //log.error("Got some exception in remove pair block: " + e.getMessage());
        }
    }
}