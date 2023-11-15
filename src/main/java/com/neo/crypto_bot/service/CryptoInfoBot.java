package com.neo.crypto_bot.service;

import com.neo.crypto_bot.client.BinanceExchangeApiClient;
import com.neo.crypto_bot.client.ExchangeApiClient;
import com.neo.crypto_bot.config.BotConfig;
import com.neo.crypto_bot.config.BotStateKeeper;
import com.neo.crypto_bot.constant.Actions;
import com.neo.crypto_bot.constant.BotState;
import com.neo.crypto_bot.constant.Language;
import com.neo.crypto_bot.constant.TextCommands;
import com.neo.crypto_bot.model.BotUser;
import com.neo.crypto_bot.model.TradingPair;
import com.neo.crypto_bot.repository.BotUserRepository;
import com.neo.crypto_bot.repository.TradingPairRepository;
import com.vdurmont.emoji.EmojiParser;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethodMessage;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
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

    private final ImageFactory imageFactory;

    private String tempAssetName = "None";

    public CryptoInfoBot(BotConfig botConfig, BotUserRepository botUserRepository,
                         TradingPairRepository tradingPairRepository, BinanceExchangeApiClient exchangeClient,
                         ReplyKeyboardFactory replyKeyboardFactory, CommandParser commandParser, BotStateKeeper botStateKeeper,
                         ImageFactory imageFactory) {
        super(botConfig.getBotToken());
        this.botConfig = botConfig;
        this.botCommands = new ArrayList<>();
        this.botUserRepository = botUserRepository;
        this.tradingPairRepository = tradingPairRepository;
        this.exchangeClient = exchangeClient;
        this.replyKeyboardFactory = replyKeyboardFactory;
        this.commandParser = commandParser;
        this.botStateKeeper = botStateKeeper;
        this.imageFactory = imageFactory;
        Locale.setDefault(new Locale("en"));
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
                processUserTextInput(update.getMessage().getChat(), text);
            }
        }
        if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            BotUser currUser = botUserRepository.findById(callbackQuery.getMessage().getChatId()).get();
            String answer = "";
            switch (callbackQuery.getData()) {
                case Actions.ENG_LANGUAGE -> {
                    currUser.setLanguage(Language.EN.toString());
                    answer = "Great! Lets continue on english!";
                }
                case Actions.UA_LANGUAGE -> {
                    currUser.setLanguage(Language.UA.toString());
                    answer = "Чудово! Продовжимо на солов'їній!";
                }
            }
            botUserRepository.save(currUser);

            try {
                sendApiMethod(AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackQuery.getId())
                        .text(answer)
                        .build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }

            LocalizationManager.setLocale(new Locale(currUser.getLanguage().toLowerCase()));
            try {
                sendApiMethod(SendMessage.builder()
                        .chatId(callbackQuery.getMessage().getChatId())
                        .text(MessageFormat.format(LocalizationManager.getString("start_message"), currUser.getFirstName()))
                        .replyMarkup(replyKeyboardFactory.getKeyboardWithTop25Pairs())
                        .build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void processUserTextInput(Chat chat, String receivedMessage) {
        List<String> pairs = commandParser.getPairsFromCommand(receivedMessage);
        long chatId = chat.getId();
        botUserRepository.findById(chatId).ifPresent(botUser -> LocalizationManager.setLocale(new Locale(botUser.getLanguage().toLowerCase())));

        //Offer second asset to user if he entered only single asset name
        switch (botStateKeeper.getBotState()) {
            case INPUT_FOR_CURRENCY -> {
                if (pairs.size() == 1 && pairs.get(0).length() <= 4 && tempAssetName.equals("None")) {
                    tempAssetName = pairs.get(0);
                    ReplyKeyboardMarkup convertibles = replyKeyboardFactory.getKeyboardWithConvertibles(pairs.get(0));
                    if (!convertibles.getKeyboard().isEmpty())
                        sendTextAnswer(chatId, LocalizationManager.getString("choose_asset_message"), replyKeyboardFactory.getKeyboardWithConvertibles(pairs.get(0)));
                    else
                        sendTextAnswer(chatId, LocalizationManager.getString("convertibles_empty_error"), replyKeyboardFactory.getKeyboardWithTop25Pairs());
                } else if (pairs.size() == 1 && pairs.get(0).length() <= 4 && !tempAssetName.equals("None")) {
                    String answer = exchangeClient.getCurrency(List.of(tempAssetName + pairs.get(0)), chatId);
                    sendTextAnswer(chatId, answer, replyKeyboardFactory.getKeyboardWithTop25Pairs());
                    if (!answer.contains("error")) {
                        addPairIfNoExistToList(tempAssetName + pairs.get(0));
                        increasePairRate(tempAssetName + pairs.get(0));
                    }
                    tempAssetName = "None";
                } else {
                    String exchangeResponse = exchangeClient.getCurrency(pairs, chatId); //Try to get currencies of entered pairs from exchange
                    boolean invalidPairInput = exchangeResponse.contains("error");
                    if (!invalidPairInput) {
                        pairs.forEach(p -> {
                            addPairIfNoExistToList(p);
                            increasePairRate(p);
                        });

                        //log.info("Got currency of pairs: " + pairs);
                    }
                   this.sendAnswer(exchangeResponse, chatId);
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
                sendTextAnswer(chatId, "Please press /start command", null);
            }
        }
    }

    /**
     * Method sends currencies of favorite pairs for each bot user at 6:01 server time
     */
    @Scheduled(cron = "0 1 6 * * *")
    private void sendUserFavoritePairCurrencies() {
        List<BotUser> botUsers = botUserRepository.getUsersWithFavorites();
        HashMap<Long, Set<TradingPair>> favoritesCache = this.getFavoritesCache(botUsers);
        HashMap<String, Double> priceList = this.getPriceListForAllUsersFavorites(favoritesCache);
        HashMap<String, Double> priceDeviationList = this.exchangeClient.getPricesDayDeviation(priceList.keySet().stream().toList());

        StringBuilder sb = new StringBuilder();
        DecimalFormat df = new DecimalFormat("#.##############");
        int[] index = new int[1];
        index[0] = 1;

        botUsers.forEach(u -> {
            LocalizationManager.setLocale(new Locale(u.getLanguage().toLowerCase()));
            sb.append(MessageFormat.format(LocalizationManager.getString("daily_message"), u.getFirstName())).append("\n");
            u.getFavorites().forEach(p -> {
                String symbol = p.getName();
                String lastPrice = df.format(priceList.get(symbol));
                Double deviation = priceDeviationList.get(symbol);
                String direction = deviation > 20 ? ":rocket:" : (deviation > 0 ? ":chart_with_upwards_trend:" : ":chart_with_downwards_trend:");
                sb.append(index[0]++).append(String.format(") %s   :   %s (%.2f%%) ", symbol, lastPrice, deviation))
                        .append(direction)
                        .append("\n");
                increasePairRate(symbol);
            });
            this.sendAnswer(sb.toString(), u.getId());
            index[0] = 1;
            sb.replace(0, sb.toString().length(), "");
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

        StringBuilder sb = new StringBuilder();
        int[] index = new int[1];
        index[0] = 1;

        for (Map.Entry<Long, Set<TradingPair>> entry : favoritesCache.entrySet()) {
            LocalizationManager.setLocale(new Locale(botUserRepository.findById(entry.getKey()).get().getLanguage().toLowerCase()));
            String headMessage = LocalizationManager.getString("warning_message") + "\n";
            sb.append(headMessage);
            entry.getValue().forEach(p -> {
                double freshPrice = priceList.get(p.getName());
                double deviation = calculateDeviation(p.getLastCurrency(), freshPrice) * 100;
                String direction = deviation > 20 ? ":rocket:" : (deviation > 0 ? ":chart_with_upwards_trend:" : ":chart_with_downwards_trend:");
                if (Math.abs(deviation) > 5) {
                    DecimalFormat df = new DecimalFormat("#.##############");
                    sb.append(String.format("%02d) %-10s: %-9s -> %-9s (%.2f%%) %s\n", index[0]++, p.getName(), df.format(p.getLastCurrency()), df.format(freshPrice), deviation, direction));
                }
            });
            if (sb.toString().length() > headMessage.length()) {
                this.sendAnswer(sb.toString(), entry.getKey());
            }
            sb.replace(0, sb.toString().length(), "");
            index[0] = 1;
        }
        priceList.forEach(tradingPairRepository::updatePrice);
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

    private HashMap<String, Double> getPriceListForAllUsersFavorites
            (HashMap<Long, Set<TradingPair>> favoritesCache) {

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

    private void sendAnswer(String answer, long chatId) {
        try {
            String[] lines = answer.split("\n");
            answer = EmojiParser.parseToUnicode(answer);
            ReplyKeyboardMarkup keyboardTop25 = replyKeyboardFactory.getKeyboardWithTop25Pairs(); //Offer top 25 pairs for user to choose
            if (lines.length > 2 && lines.length <= 18) {
                sendPhotoAnswer(chatId, imageFactory.createImageWithText(answer, 86, 0), keyboardTop25);
            } else if (lines.length > 18) {
                sendPhotoAnswer(chatId, imageFactory.createImageWithText(answer, 72, 1), keyboardTop25);
            } else {
                sendTextAnswer(chatId, answer, keyboardTop25);
            }
            //log.error("Wrong user input or exchange no have such pair listing: " + pairs);
        } catch (IOException e) {
            System.out.println("Got exception while send answer: " + e.getMessage());
        }
    }

    private void sendTextAnswer(long chatId, String answer, ReplyKeyboardMarkup keyboardMarkup) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(EmojiParser.parseToUnicode(answer))
                .build();
        if (keyboardMarkup != null)
            message.setReplyMarkup(keyboardMarkup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            //log.error("Got some TelegramAPI exception: " + e.getMessage());
            System.out.println("Got some TelegramAPI exception: " + e.getMessage());
        }
    }

    private void sendPhotoAnswer(long chatId, InputFile photo, ReplyKeyboardMarkup keyboardMarkup) {
        SendPhoto sendPhoto = SendPhoto.builder()
                .chatId(chatId)
                .photo(photo)
                .build();
        if (keyboardMarkup != null)
            sendPhoto.setReplyMarkup(keyboardMarkup);
        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            //log.error("Got some TelegramAPI exception: " + e.getMessage());
            System.out.println("Got some TelegramAPI exception: " + e.getMessage());
        }
    }
}
