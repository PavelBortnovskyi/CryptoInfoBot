package com.neo.crypto_bot.config;

import com.neo.crypto_bot.constant.BotState;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
@Getter
public class BotStateKeeper {

    private BotState botState = BotState.INPUT_FOR_CURRENCY;

    public void changeState(BotState botState) {
        this.botState = botState;
    }
}
