package com.neo.crypto_bot.command;

import com.neo.crypto_bot.client.ExchangeApiClient;
import com.neo.crypto_bot.constant.TextCommands;
import com.neo.crypto_bot.model.TradingPair;
import com.neo.crypto_bot.repository.BotUserRepository;
import com.neo.crypto_bot.repository.TradingPairRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.stream.Collectors;

//@Log4j2
@Component
public class PopularPairsCommandHandler extends BotCommand {

    private final ExchangeApiClient exchangeClient;

    private final TradingPairRepository tradingPairRepository;

    private final BotUserRepository botUserRepository;

    public PopularPairsCommandHandler(@Value(TextCommands.GET_POPULAR_PAIRS) String commandIdentifier,
                                      @Value(TextCommands.GPP_DESCRIPTION) String description,
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
        List<TradingPair> popularPairs = tradingPairRepository.getPopularPairs();
        if (!popularPairs.isEmpty()) {
            String prices = exchangeClient.getCurrency(popularPairs.stream().map(TradingPair::getName).collect(Collectors.toList()));
            String[] pricesRows = prices.split("\n");
            StringBuilder sb = new StringBuilder(pricesRows[0] + "\n");
            int index = 1;
            for (TradingPair p : popularPairs) {
                for (int i = 1; i < pricesRows.length; i++) {
                    if (pricesRows[i].contains(p.getName())) {
                        pricesRows[i] = pricesRows[i].replaceAll("\\b\\d+\\)", index++ + ") ");
                        sb.append(pricesRows[i] += ", asked " + p.getRequests() + " times\n");
                    }
                }
            }
            messageToSend.setText(EmojiParser.parseToUnicode(sb.toString()));
        } else messageToSend.setText("Sorry, our pair rank list is empty at the moment");
        try {
            absSender.execute(messageToSend);
        } catch (
                TelegramApiException e) {
            //log.error("Got some exception in favorite pairs block: " + e.getMessage());
        }
    }
}
