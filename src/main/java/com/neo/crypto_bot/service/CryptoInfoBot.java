package com.neo.crypto_bot.service;

import com.neo.crypto_bot.client.BinanceExchangeApiClient;
import com.neo.crypto_bot.client.ExchangeApiClient;
import com.neo.crypto_bot.config.BotConfig;
import com.neo.crypto_bot.config.BotStateKeeper;
import com.neo.crypto_bot.constant.BotState;
import com.neo.crypto_bot.constant.TextCommands;
import com.neo.crypto_bot.model.BotUser;
import com.neo.crypto_bot.model.TradingPair;
import com.neo.crypto_bot.repository.BotUserRepository;
import com.neo.crypto_bot.repository.TradingPairRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j2;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLOutput;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

//@Log4j2
@Component
public class CryptoInfoBot extends TelegramLongPollingCommandBot {

    public List<BotCommand> botCommands;

    private final BotConfig botConfig;

    private final ReplyKeyboardFactory replyKeyboardFactory;

    private final BotUserRepository botUserRepository;

    private final TradingPairRepository tradingPairRepository;

    private final ExchangeApiClient exchangeClient;

    private final CommandParser commandParser;

    private final BotStateKeeper botStateKeeper;

    private String tempAssetName = "None";

    public CryptoInfoBot(BotConfig botConfig, BotUserRepository botUserRepository,
                         TradingPairRepository tradingPairRepository, BinanceExchangeApiClient exchangeClient,
                         ReplyKeyboardFactory replyKeyboardFactory, CommandParser commandParser, BotStateKeeper botStateKeeper) {
        super(botConfig.getBotToken());
        this.botConfig = botConfig;
        this.botCommands = new ArrayList<>();
        this.botUserRepository = botUserRepository;
        this.tradingPairRepository = tradingPairRepository;
        this.exchangeClient = exchangeClient;
        this.replyKeyboardFactory = replyKeyboardFactory;
        this.commandParser = commandParser;
        this.botStateKeeper = botStateKeeper;
        botCommands.add(new BotCommand("/start", "get started"));
        botCommands.add(new BotCommand("/my_data", "get info about user"));
        botCommands.add(new BotCommand("/delete_my_data", "remove all info about user"));
        botCommands.add(new BotCommand("/help", "get full commands list"));
        botCommands.add(new BotCommand("/add_pair", "add pair to favorites"));
        botCommands.add(new BotCommand("/remove_pair", "removes pair from favorites"));
        botCommands.add(new BotCommand("/get_all_favorite_pairs", "get currencies of pairs from favorites list"));
        botCommands.add(new BotCommand("/get_popular_pairs", "get top 25 frequently searched pairs with currencies"));
//        botCommands.add(new BotCommand("/settings", "set your preferences"));
        try {
            this.execute(new SetMyCommands(this.botCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            //log.error("Error while set bot`s command list: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public void processNonCommandUpdate(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String text = message.getText();
            IBotCommand command = getRegisteredCommand(text);
            if (Objects.nonNull(command)) {
                command.processMessage(this, message, new String[]{});
            } else {
                processUserInput(update.getMessage().getChat(), text);
            }
        }
    }

    private void processUserInput(Chat chat, String receivedMessage) {
        List<String> pairs = commandParser.getPairsFromCommand(receivedMessage);
        long chatId = chat.getId();
        //Offer second asset to user if he entered only single asset name
        switch (botStateKeeper.getBotState()) {
            case INPUT_FOR_CURRENCY -> {
                if (pairs.size() == 1 && pairs.get(0).length() <= 4 && tempAssetName.equals("None")) {
                    tempAssetName = pairs.get(0);
                    sendAnswer(chatId, "Please choose asset to get pair currency", replyKeyboardFactory.getKeyboardWithConvertibles(pairs.get(0)));
                } else if (pairs.size() == 1 && pairs.get(0).length() <= 4 && !tempAssetName.equals("None")) {
                    //String pair = tempAssetName.toUpperCase().equals("BTC") ? pairs.get(0) + tempAssetName : tempAssetName + pairs.get(0);
                    String answer = exchangeClient.getCurrency(List.of(tempAssetName + pairs.get(0)));
                    sendAnswer(chatId, answer, replyKeyboardFactory.getKeyboardWithTop25Pairs());
                    if (!answer.contains("error")) {
                        addPairIfNoExistToList(tempAssetName + pairs.get(0));
                        increasePairRate(tempAssetName + pairs.get(0));
                    }
                    tempAssetName = "None";
                } else {
                    String exchangeResponse = exchangeClient.getCurrency(pairs); //Try to get currencies of entered pairs from exchange
                    boolean invalidPairInput = exchangeResponse.contains("error");
                    if (!invalidPairInput) {
                        pairs.forEach(p -> {
                            addPairIfNoExistToList(p);
                            increasePairRate(p);
                        });
                        sendAnswer(chatId, exchangeClient.getCurrency(pairs), replyKeyboardFactory.getKeyboardWithTop25Pairs()); //Offer top 25 pairs for user to choose
                        //log.info("Got currency of pairs: " + pairs);
                    } else if (invalidPairInput) {
                        sendAnswer(chatId, exchangeResponse, replyKeyboardFactory.getKeyboardWithTop25Pairs());
                        //log.error("Wrong user input or exchange no have such pair listing: " + pairs);
                    }
                }
            }
            case INPUT_FOR_ADD -> {
                this.executeCommand(TextCommands.ADD_PAIR, receivedMessage, chat);
                botStateKeeper.changeState(BotState.INPUT_FOR_CURRENCY);
            }
            case INPUT_FOR_REMOVE -> {
                this.executeCommand(TextCommands.REMOVE_PAIR, receivedMessage, chat);
                botStateKeeper.changeState(BotState.INPUT_FOR_CURRENCY);
            }
            case INITIALIZATION -> {
                sendAnswer(chatId, "Please press /start command", null);
            }
        }
    }

    /**
     * Method sends currencies of favorite pairs for each bot user at 8:01 server time
     */
    @Scheduled(cron = "0 1 6 * * *")
    private void sendUserFavoritePairCurrencies() {
        List<BotUser> botUsers = botUserRepository.getUsersWithFavorites();
        HashMap<Long, Set<TradingPair>> favoritesCache = this.getFavoritesCache(botUsers);
        HashMap<String, Double> priceList = this.getPriceListForAllUsersFavorites(favoritesCache);
        HashMap<String, Double> priceDeviationList = this.exchangeClient.getPricesDayDeviation(priceList.keySet().stream().toList());

        StringBuilder sb = new StringBuilder("Hi! Lets see daily update for your favorites:\n");
        DecimalFormat df = new DecimalFormat("#.########");
        int[] index = new int[1];
        index[0] = 1;

        botUsers.forEach(u -> {
            u.getFavorites().forEach(p -> {
                String symbol = p.getName();
                String lastPrice = df.format(priceList.get(symbol));
                Double deviation = priceDeviationList.get(symbol);
                String direction = deviation > 0 ? ":chart_with_upwards_trend:" : ":chart_with_downwards_trend:";
                sb.append(index[0]++).append(String.format(") %s   :   %s (%.2f%%) ", symbol, lastPrice, deviation))
                        .append(direction)
                        .append("\n");
                increasePairRate(symbol);
            });
            sendAnswer(u.getId(), EmojiParser.parseToUnicode(sb.toString()), null);
            index[0] = 1;
            sb.replace(46, sb.toString().length(), "");
        });

        System.out.println("Currencies of users favorite pairs sent to subscribers at: " + LocalDateTime.now());
        //log.info("Currencies of users favorite pairs sent to subscribers at: " + LocalDateTime.now());
    }

    /**
     * Method sends notification to users if case price from favorites changes more than 5% (each 15 mins check)
     */
    @Scheduled(cron = "0 0/15 * * * *")
    private void sendUserFavoritesPriceUpdates() {
        List<BotUser> botUsers = botUserRepository.getUsersWithFavorites();
        HashMap<Long, Set<TradingPair>> favoritesCache = this.getFavoritesCache(botUsers);
        HashMap<String, Double> priceList = this.getPriceListForAllUsersFavorites(favoritesCache);

        StringBuilder sb = new StringBuilder("Assets from your favorites has change prices:\n");
        int[] index = new int[1];
        index[0] = 1;

        for (Map.Entry<Long, Set<TradingPair>> entry : favoritesCache.entrySet()) {
            entry.getValue().forEach(p -> {
                double freshPrice = priceList.get(p.getName());
                double deviation = calculateDeviation(p.getLastCurrency(), freshPrice);
                if (Math.abs(deviation) > 0.05) {
                    DecimalFormat df = new DecimalFormat("#.##############");
                    sb.append(index[0]++).append(String.format(") %s: %s -> %s (%.2f%%)\n", p.getName(), df.format(p.getLastCurrency()), df.format(freshPrice), deviation * 100));
                }
            });
            if (sb.toString().length() > 47) {
                sendAnswer(entry.getKey(), sb.toString(), null);
                sb.replace(46, sb.toString().length(), "");
            }
            index[0] = 1;
        }
        priceList.forEach(tradingPairRepository::updatePrice);
    }

    private void sendAnswer(long chatId, String answer, ReplyKeyboardMarkup keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(answer);
        if (keyboardMarkup != null)
            message.setReplyMarkup(keyboardMarkup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            //log.error("Got some TelegramAPI exception: " + e.getMessage());
        }
    }

    private void executeCommand(String commandName, String argument, Chat chat) {
        IBotCommand command = getRegisteredCommand(commandName);
        Message message = new Message();
        message.setText("/" + commandName + " " + argument);
        message.setChat(chat);
        String[] arg = new String[1];
        arg[0] = argument;
        command.processMessage(this, message, arg);
    }

    private void increasePairRate(String tradingPairName) {
        if (tradingPairRepository.findByName(tradingPairName).isPresent())
            tradingPairRepository.increaseRate(tradingPairName);
    }

    private TradingPair addPairIfNoExistToList(String tradingPairName) {
        if (tradingPairRepository.findByName(tradingPairName).isEmpty()) {
            TradingPair tradingPair = exchangeClient.getPair(tradingPairName);
            tradingPair.setRequests(0);
            //log.info("New pair added to list: " + tradingPairName);
            return tradingPairRepository.save(tradingPair);
        } else return tradingPairRepository.findByName(tradingPairName).get();
    }

    private HashMap<String, Double> getPriceListForAllUsersFavorites(HashMap<Long, Set<TradingPair>> favoritesCache) {

        Set<TradingPair> usersFavorites = favoritesCache.values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        List<String> favoritesNames = usersFavorites.stream().map(TradingPair::getName).collect(Collectors.toList());
        if (!favoritesNames.isEmpty()) {
            return exchangeClient.getPrices(favoritesNames);
        } else return new HashMap<>();
    }

    private HashMap<Long, Set<TradingPair>> getFavoritesCache(List<BotUser> botUsers) {
        HashMap<Long, Set<TradingPair>> usersIdsWithFavorites = new HashMap<>();
        botUsers.forEach(bu -> usersIdsWithFavorites.put(bu.getId(), bu.getFavorites()));
        return usersIdsWithFavorites;
    }

    private Double calculateDeviation(Double oldPrice, Double newPrice) {
        return (newPrice - oldPrice) / oldPrice;
    }
}
