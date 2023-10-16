package com.neo.crypto_bot.service;

import com.neo.crypto_bot.constant.Fields;
import com.neo.crypto_bot.model.TradingPair;
import com.neo.crypto_bot.repository.BotUserRepository;
import com.neo.crypto_bot.repository.TradingPairRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ReplyKeyboardFactory {

    private final TradingPairRepository tradingPairRepository;

    private final BotUserRepository botUserRepository;

    public ReplyKeyboardMarkup getKeyboardWithTop25Pairs() {
        List<TradingPair> topPairs = tradingPairRepository.getPopularPairs();
        return fillKeyBoard(topPairs, 4, Fields.NAME);
    }

    public ReplyKeyboardMarkup getKeyboardWithConvertibles(String baseAssetName) {
        List<TradingPair> convertiblePairs = tradingPairRepository.getConvertibleAssets(baseAssetName);
        return fillKeyBoard(convertiblePairs, 5, Fields.QUOTE_ASSET);
    }

    public ReplyKeyboardMarkup getKeyboardWithFavorites(long chatId) {
        Set<TradingPair> favorites = botUserRepository.findById(chatId).get().getFavorites();
        return fillKeyBoard(favorites, 5, Fields.NAME);
    }

    private ReplyKeyboardMarkup fillKeyBoard(Collection<TradingPair> collection, int columnsCount, Fields fields) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        int rowIndex = 0;
        if (!collection.isEmpty()) {
            keyboardRows.add(new KeyboardRow());
            for (TradingPair p : collection) {
                if (keyboardRows.get(rowIndex).size() == columnsCount) {
                    keyboardRows.add(new KeyboardRow());
                    rowIndex++;
                } else {
                    String buttonLabel;
                    switch (fields) {
                        case NAME -> buttonLabel = p.getName();
                        case BASE_ASSET -> buttonLabel = p.getBaseAsset();
                        case QUOTE_ASSET -> buttonLabel = p.getQuoteAsset();
                        default -> buttonLabel = "";
                    }
                    keyboardRows.get(rowIndex).add(buttonLabel);
                }
            }
            keyboardMarkup.setKeyboard(keyboardRows);
            return keyboardMarkup;
        } else return new ReplyKeyboardMarkup();
    }
}
