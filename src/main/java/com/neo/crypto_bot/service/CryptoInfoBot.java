package com.neo.crypto_bot.service;

import com.neo.crypto_bot.client.BinanceExchangeApiClient;
import com.neo.crypto_bot.client.ExchangeApiClient;
import com.neo.crypto_bot.config.BotConfig;
import com.neo.crypto_bot.model.BotUser;
import com.neo.crypto_bot.model.TradingPair;
import com.neo.crypto_bot.repository.BotUserRepository;
import com.neo.crypto_bot.repository.TradingPairRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Component
public class CryptoInfoBot extends TelegramLongPollingBot {

    public List<BotCommand> botCommands;

    private final BotConfig botConfig;

    private final String username;

    private final BotUserRepository botUserRepository;

    private final TradingPairRepository tradingPairRepository;

    private final ExchangeApiClient exchangeClient;

    private String tempAssetName = "None";

    public CryptoInfoBot(BotConfig botConfig, BotUserRepository botUserRepository,
                         TradingPairRepository tradingPairRepository, BinanceExchangeApiClient exchangeClient) {
        super(botConfig.getBotToken());
        this.botConfig = botConfig;
        this.username = getBotUsername();
        this.botCommands = new ArrayList<>();
        this.botUserRepository = botUserRepository;
        this.tradingPairRepository = tradingPairRepository;
        this.exchangeClient = exchangeClient;
        botCommands.add(new BotCommand("/start", "get started"));
        botCommands.add(new BotCommand("/my_data", "get info about user"));
        botCommands.add(new BotCommand("/delete_my_data", "remove all info about user"));
        botCommands.add(new BotCommand("/help", "get full commands list"));
        botCommands.add(new BotCommand("/add_pair", "add pair to favorites"));
        botCommands.add(new BotCommand("/remove_pair", "removes pair form favorites"));
        botCommands.add(new BotCommand("/get_all_favorite_pairs", "get currencies of pairs from favorites list"));
        botCommands.add(new BotCommand("/get_popular_pairs", "get top 25 frequently searched pairs with currencies"));
        botCommands.add(new BotCommand("/settings", "set your preferences"));
        try {
            this.execute(new SetMyCommands(this.botCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error while set bot`s command list: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return "${bot.name}";
    }


    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String receivedMessage = update.getMessage().getText();
            String clientName = update.getMessage().getChat().getFirstName();
            long chatId = update.getMessage().getChatId();

            switch (receivedMessage) {
                case ("/start"): {
                    registerUser(update.getMessage());
                    onStartCommandReceived(chatId, clientName);
                    break;
                }
                case ("/my_data"): {
                    onMyDataCommandReceived(chatId);
                    break;
                }
                case ("/delete_my_data"): {
                    onDeleteCommandReceiver(chatId);
                    break;
                }
                case ("/add_pair"): {
                    sendAnswer(chatId, "You should use this command in /add_pair BTCUSDT format \n " +
                            "or  /add_pair BTCUSDT, LTCUSDT to add few pairs to favorites", null);
                    break;
                }
                case ("/get_all_favorite_pairs"): {
                    onGetAllFavoritePairs(chatId);
                    break;
                }
                case ("/get_popular_pairs"): {
                    onGetPopularPairs(chatId);
                    break;
                }
                default:
                    if (receivedMessage.startsWith("/add_pair")) {
                        List<String> pairs = getPairsFromCommand(receivedMessage, 9);
                        if (exchangeClient.checkPair(pairs)) {
                            if (botUserRepository.findById(chatId).isPresent()) {
                                BotUser currUser = botUserRepository.findById(chatId).get();
                                pairs.forEach(p -> {
                                    addPairIfNoExistToList(p);
                                    currUser.getFavorites().add(exchangeClient.getPair(p));
                                });
                                botUserRepository.save(currUser);
                                sendAnswer(chatId, pairs + " added to your favorite list", null);
                                log.info(String.format("User with chatId: %s added %s pair(s) to follow", chatId, pairs));
                            } else
                                sendAnswer(chatId, "You need to register by /start command to have possibility to add favorite pairs", null);
                        } else
                            sendAnswer(chatId, "Looks like Binance does not have mentioned pairs, try different", null);
                    }
                    processUserInput(chatId, receivedMessage);
            }
        }
    }


    private void processUserInput(long chatId, String receivedMessage) {
        List<String> pairs = getPairsFromCommand(receivedMessage, 0);
        //Offer second asset to user if he entered only single asset name
        if (pairs.size() == 1 && pairs.get(0).length() <= 4 && tempAssetName.equals("None")) {
            tempAssetName = pairs.get(0);
            sendAnswer(chatId, "Please choose asset to get pair currency", getKeyboardWithConvertibles2(pairs.get(0)));
        } else if (pairs.size() == 1 && pairs.get(0).length() <= 4 && !tempAssetName.equals("None")) {
            //String pair = tempAssetName.toUpperCase().equals("BTC") ? pairs.get(0) + tempAssetName : tempAssetName + pairs.get(0);
            String answer = exchangeClient.getCurrency(List.of(tempAssetName + pairs.get(0)));
            sendAnswer(chatId, answer, getKeyboardWithTop25Pairs());
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
                    //addPairIfNoExistToList(p);
                    increasePairRate(p);
                });
                sendAnswer(chatId, exchangeClient.getCurrency(pairs), getKeyboardWithTop25Pairs()); //Offer top 25 pairs for user to choose
                log.info("Got currency of pairs: " + pairs);
            } else if (invalidPairInput) {
                sendAnswer(chatId, exchangeResponse, getKeyboardWithTop25Pairs());
                log.error("Wrong user input or exchange no have such pair listing: " + pairs);
            }
        }
    }

    private void onStartCommandReceived(long chatId, String name) {
        StringBuilder sb = new StringBuilder("Hi, " + name + " , nice to meet you!\n");
        sb.append("This bot is created to get quick info and some statistic about trading pairs on Binance.\n");
        sb.append("Just write pair symbols to get currency (Example: BTCUSDT or few pairs: BTCUSDT, LTCUSDT).\n");
        sb.append("You can get more information with /help command");
        List<TradingPair> list = exchangeClient.getListing(); //TODO: remove from here
        tradingPairRepository.saveAll(list);
        sendAnswer(chatId, sb.toString(), getKeyboardWithTop25Pairs());
    }

    private void onMyDataCommandReceived(long chatId) {
        BotUser currUser = botUserRepository.findById(chatId).orElse(new BotUser());
        if (currUser.getId() != null) {
            StringBuilder sb = new StringBuilder("This bot have some data about you: \n\n");
            sb.append("Nickname: ").append(currUser.getNickName()).append("\n")
                    .append("FirstName: ").append(currUser.getFirstName()).append("\n")
                    .append("LastName: ").append(currUser.getLastName()).append("\n")
                    .append("Registered at: ").append(currUser.getRegisteredAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            sendAnswer(chatId, sb.toString(), null);
        } else sendAnswer(chatId, "We no have any data about you, press /start to register", null);
    }

    private void onDeleteCommandReceiver(long chatId) {
        if (botUserRepository.findById(chatId).isPresent()) {
            botUserRepository.deleteById(chatId);
            log.info("Deleted data about user with chatID: " + chatId);
        } else sendAnswer(chatId, "We no have any data about you, press /start to register", null);
    }

    private void onGetAllFavoritePairs(long chatId) {
        if (botUserRepository.findById(chatId).isPresent()) {
            Set<TradingPair> userPairs = botUserRepository.getUsersFavoritePairs(chatId);
            if (!userPairs.isEmpty()) {
                sendAnswer(chatId, exchangeClient.getCurrency(userPairs.stream().map(TradingPair::getName).collect(Collectors.toList())), null);
                userPairs.forEach(p -> increasePairRate(p.getName()));
            } else sendAnswer(chatId, "You no have favorite pair, please add them using /add command", null);
        } else
            sendAnswer(chatId, "You need to register by /start command to have possibility to get favorite pairs", null);
    }

    /**
     * Method sends currencies of favorite pairs for each bot user at 8:00 server time
     */
    @Scheduled(cron = "0 0 8 * * *")
    private void sendUserFavoritePairCurrencies() {
        List<BotUser> botUsers = botUserRepository.findAll().stream().filter(u -> !u.getFavorites().isEmpty()).toList();
        botUsers.forEach(u -> onGetAllFavoritePairs(u.getId()));
        log.info("Currencies of users favorite pairs sent to subscribers at: " + LocalDateTime.now());
    }

    private void onGetPopularPairs(long chatId) {
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
            sendAnswer(chatId, sb.toString(), null);
        } else sendAnswer(chatId, "Sorry, our pair rank list is empty at the moment", null);
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
            log.error("Got some TelegramAPI exception: " + e.getMessage());
        }
    }

    private void registerUser(Message message) {
        if (this.botUserRepository.findById(message.getChatId()).isEmpty()) {
            BotUser freshUser = new BotUser();
            freshUser.setId(message.getChatId());
            freshUser.setNickName(message.getChat().getUserName());
            freshUser.setFirstName(message.getChat().getFirstName());
            freshUser.setLastName(message.getChat().getLastName());
            freshUser.setRegisteredAt(LocalDateTime.now());
            this.botUserRepository.save(freshUser);
            log.info(String.format("New User with chatId: %d registered", freshUser.getId()));
        }
    }

    private List<String> getPairsFromCommand(String command, int commandShift) {
        List<String> pairs = new ArrayList<>();
        String userInput = command.substring(commandShift).replaceAll(" ", "").toUpperCase();
        if (userInput.contains(",")) {
            pairs.addAll(Arrays.asList(userInput.split(",")));
        } else pairs.add(userInput);
        return pairs;
    }

    private void increasePairRate(String tradingPairName) {
        if (tradingPairRepository.findByName(tradingPairName).isPresent())
            tradingPairRepository.increaseRate(tradingPairName);
    }

    private TradingPair addPairIfNoExistToList(String tradingPairName) {
        if (tradingPairRepository.findByName(tradingPairName).isEmpty()) {
            TradingPair tradingPair = exchangeClient.getPair(tradingPairName);
            tradingPair.setRequests(0);
            log.info("New pair added to list: " + tradingPairName);
            return tradingPairRepository.save(tradingPair);
        } else return tradingPairRepository.findByName(tradingPairName).get();
    }

    private ReplyKeyboardMarkup getKeyboardWithTop25Pairs() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        List<TradingPair> topPairs = tradingPairRepository.getPopularPairs();
        if (topPairs.size() == 25) {
            int rowIndex = 0;
            keyboardRows.add(new KeyboardRow());
            for (int i = 0; i < 25; i++) {
                if (keyboardRows.get(rowIndex).size() % 5 == 0) {
                    keyboardRows.add(new KeyboardRow());
                    rowIndex++;
                }
                keyboardRows.get(rowIndex).add(topPairs.get(i).getName());
            }
            keyboardMarkup.setKeyboard(keyboardRows);
            return keyboardMarkup;
        } else return null;
    }

    private ReplyKeyboardMarkup getKeyboardWithConvertibles(String baseAssetName) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        List<TradingPair> convertiblePairs = exchangeClient.getConvertiblePairs(baseAssetName);
        int rowIndex = 0;
        keyboardRows.add(new KeyboardRow());
        for (TradingPair p : convertiblePairs) {
            if (keyboardRows.get(rowIndex).size() == 5) {
                keyboardRows.add(new KeyboardRow());
                rowIndex++;
            } else
                keyboardRows.get(rowIndex).add(p.getQuoteAsset());
        }
        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    private ReplyKeyboardMarkup getKeyboardWithConvertibles2(String baseAssetName) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        List<TradingPair> convertiblePairs = tradingPairRepository.getConvertibleAssets(baseAssetName);
        int rowIndex = 0;
        keyboardRows.add(new KeyboardRow());
        for (TradingPair p : convertiblePairs) {
            if (keyboardRows.get(rowIndex).size() == 5) {
                keyboardRows.add(new KeyboardRow());
                rowIndex++;
            } else
                keyboardRows.get(rowIndex).add(p.getQuoteAsset());
        }
        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }
}
