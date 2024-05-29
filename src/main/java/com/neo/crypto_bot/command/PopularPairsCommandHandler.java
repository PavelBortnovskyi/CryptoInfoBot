package com.neo.crypto_bot.command;

import com.neo.crypto_bot.client.ExchangeApiClient;
import com.neo.crypto_bot.constant.TextCommands;
import com.neo.crypto_bot.model.TradingPair;
import com.neo.crypto_bot.repository.BotUserRepository;
import com.neo.crypto_bot.repository.TradingPairRepository;
import com.neo.crypto_bot.service.ImageFactory;
import com.neo.crypto_bot.service.LocalizationManager;
import com.vdurmont.emoji.EmojiParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

//@Log4j2
@Component
public class PopularPairsCommandHandler extends BotCommand {

    private final ExchangeApiClient exchangeClient;

    private final TradingPairRepository tradingPairRepository;

    private final BotUserRepository botUserRepository;

    private final ImageFactory imageFactory;

    public PopularPairsCommandHandler(@Value(TextCommands.GET_POPULAR_PAIRS) String commandIdentifier,
                                      @Value(TextCommands.GPP_DESCRIPTION) String description,
                                      ExchangeApiClient exchangeClient,
                                      TradingPairRepository tradingPairRepository,
                                      BotUserRepository botUserRepository,
                                      ImageFactory imageFactory) {
        super(commandIdentifier, description);
        this.exchangeClient = exchangeClient;
        this.tradingPairRepository = tradingPairRepository;
        this.botUserRepository = botUserRepository;
        this.imageFactory = imageFactory;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        long chatId = chat.getId();

//        SendChatAction chatAction = SendChatAction.builder()
//                .chatId(chatId)
//                .action("typing")
//                .build();
//        try {
//            absSender.execute(chatAction);
//        } catch (TelegramApiException e) {
//            throw new RuntimeException(e);
//        }

        SendMessage messageToSend = SendMessage.builder().chatId(chatId).text("").build();
        List<TradingPair> popularPairs = tradingPairRepository.getPopularPairs();
        botUserRepository.findById(chatId).ifPresent(botUser -> LocalizationManager.setLocale(new Locale(botUser.getLanguage())));
        if (!popularPairs.isEmpty()) {
            String prices = exchangeClient.getCurrency(popularPairs.stream().map(TradingPair::getName).collect(Collectors.toList()), chatId);
            String[] pricesRows = prices.split("\n");
            StringBuilder sb = new StringBuilder(LocalizationManager.getString("top_pair_message")).append("\n");
            int index = 1;
            for (TradingPair p : popularPairs) {
                for (int i = 1; i < pricesRows.length; i++) {
                    if (pricesRows[i].contains(p.getName())) {
                        pricesRows[i] = pricesRows[i].replaceAll("\\b\\d+\\)", String.format("%02d)", index++));
                        sb.append(MessageFormat.format(LocalizationManager.getString("asked_message"), pricesRows[i], p.getRequests())).append("\n");
                    }
                }
            }

            String answer = EmojiParser.parseToUnicode(sb.toString());
            try {
                SendPhoto photoToSend = SendPhoto.builder()
                        .chatId(chatId)
                        .photo(imageFactory.createImageWithText(answer, 72, 1))
                        .build();
                absSender.execute(photoToSend);
            } catch (IOException | TelegramApiException e) {
                System.out.println("Got some exception in favorite pairs block: " + e.getMessage());
            }
            //messageToSend.setText(EmojiParser.parseToUnicode(sb.toString()));
        } else messageToSend.setText("Sorry, our pair rank list is empty at the moment");
        try {
            absSender.execute(messageToSend);
        } catch (
                TelegramApiException e) {
            //log.error("Got some exception in favorite pairs block: " + e.getMessage());
        }
    }
}
