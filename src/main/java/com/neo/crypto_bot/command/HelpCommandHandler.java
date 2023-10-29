package com.neo.crypto_bot.command;

import com.neo.crypto_bot.constant.TextCommands;
import com.neo.crypto_bot.repository.BotUserRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

//@Log4j2
@Component
public class HelpCommandHandler extends BotCommand {

    public HelpCommandHandler(@Value(TextCommands.HELP) String commandIdentifier,
                              @Value(TextCommands.HELP_DESCRIPTION) String description) {
        super(commandIdentifier, description);
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {;
        StringBuilder sb = new StringBuilder();
        sb.append("/start - the command is necessary to get full functionality of bot.").append("\n\n");
        sb.append("It performs user registration and generates reply keyboard with top 25 trading pairs to choose from.").append("\n\n");
        sb.append("/my_data - shows all private info gathered by bot about you and stored in DB.").append("\n\n");
        sb.append("/delete_my_data - removes all private info gathered by bot about you from DB.").append("\n\n");
        sb.append("/add_pair - adds pair to favorites, makes subscription for every day info about pairs in favorite list and notification in case more than 5% price changes").append("\n\n");
        sb.append("Example of using: /add_pair BTCUSD").append("\n\n");
        sb.append("/remove_pair - removes pair from favorites").append("\n\n");
        sb.append("Example of using: /remove_pair BTCUSD").append("\n\n");
        sb.append("/get_all_favorite_pairs - returns actual currencies about pairs in favorite list").append("\n\n");
        sb.append("/get_popular_pairs - shows most popular pairs according to bot internal statistics").append("\n\n");
        SendMessage messageToSend = SendMessage.builder()
                .chatId(chat.getId())
                .text(sb.toString())
                .build();
        try {
            absSender.execute(messageToSend);
        } catch (TelegramApiException e) {
            //log.error("Got some error in help block: " + e.getMessage());
        }
    }
}