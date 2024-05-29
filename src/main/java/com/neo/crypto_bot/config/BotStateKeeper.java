package com.neo.crypto_bot.config;

import com.neo.crypto_bot.constant.BotState;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@NoArgsConstructor
@Getter
public class BotStateKeeper {

    private Map<Long, BotState> userStates = new ConcurrentHashMap<>(){{put(0L, BotState.INITIALIZATION);}};

    public BotState getStateForUser(Long userId) {
        return userStates.getOrDefault(userId, BotState.INPUT_FOR_CURRENCY);
    }

    public void setStateForUser(Long userId, BotState botState) {
        userStates.put(userId, botState);
    }
}
