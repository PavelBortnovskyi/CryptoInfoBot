package com.neo.crypto_bot.service;

import com.neo.crypto_bot.constant.Fields;
import com.neo.crypto_bot.model.BotUser;
import com.neo.crypto_bot.model.TradingPair;
import com.neo.crypto_bot.repository.BotUserRepository;
import com.neo.crypto_bot.repository.TradingPairRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.*;

@Component
@RequiredArgsConstructor
public class ReplyKeyboardFactory {

    private final TradingPairRepository tradingPairRepository;

    private final BotUserRepository botUserRepository;

    public ReplyKeyboardMarkup getKeyboardWithTop25Pairs() {
        List<TradingPair> topPairs = tradingPairRepository.getPopularPairs();
        System.out.println("Generating keyBoard with top 25 pairs");
        return fillKeyboard(topPairs, 4, Fields.NAME);
    }

    public ReplyKeyboardMarkup getKeyboardWithConvertibles(String baseAssetName) {
        List<TradingPair> convertiblePairs = tradingPairRepository.getConvertibleAssets(baseAssetName);
        System.out.println("Generating keyBoard with convertibles");
        return fillKeyboard(convertiblePairs, 4, Fields.QUOTE_ASSET);
    }

    public ReplyKeyboardMarkup getKeyboardWithFavorites(long chatId) {
        Optional<BotUser> maybeUser = botUserRepository.findById(chatId);
        Set<TradingPair> favorites = maybeUser.isEmpty() ? new HashSet<>() : maybeUser.get().getFavorites();
        System.out.println("Generating keyBoard with favorites");
        return fillKeyboard(favorites, 4, Fields.NAME);
    }

    private ReplyKeyboardMarkup fillKeyboard(Collection<TradingPair> collection, int columnsCount, Fields fields) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        int rowIndex = 0;
        if (!collection.isEmpty()) {
            keyboardRows.add(new KeyboardRow());
            for (TradingPair p : collection) {
                if (keyboardRows.get(rowIndex).size() == columnsCount) {
                    keyboardRows.add(new KeyboardRow());
                    rowIndex++;
                }
                String buttonLabel;
                switch (fields) {
                    case NAME -> buttonLabel = p.getName();
                    case BASE_ASSET -> buttonLabel = p.getBaseAsset();
                    case QUOTE_ASSET -> buttonLabel = p.getQuoteAsset();
                    default -> buttonLabel = "";
                }
                keyboardRows.get(rowIndex).add(buttonLabel);
            }
            keyboardMarkup.setKeyboard(keyboardRows);
            System.out.println("ReplyKeyboard set");
            return keyboardMarkup;
        } else {
            System.out.println("empty Keyboard"); return new ReplyKeyboardMarkup();}
    }
}
