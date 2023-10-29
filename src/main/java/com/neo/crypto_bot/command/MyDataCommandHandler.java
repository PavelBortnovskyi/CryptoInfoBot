package com.neo.crypto_bot.command;

import com.neo.crypto_bot.constant.TextCommands;
import com.neo.crypto_bot.model.BotUser;
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

import java.time.format.DateTimeFormatter;

//@Log4j2
@Component
public class MyDataCommandHandler extends BotCommand {

    private final BotUserRepository botUserRepository;

    public MyDataCommandHandler(@Value(TextCommands.MY_DATA) String commandIdentifier,
                                @Value(TextCommands.MD_DESCRIPTION) String description,
                                BotUserRepository botUserRepository) {
        super(commandIdentifier, description);
        this.botUserRepository = botUserRepository;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        BotUser currUser = botUserRepository.findById(chat.getId()).orElse(new BotUser());
        SendMessage messageToSend = SendMessage.builder().chatId(chat.getId()).text("").build();
        if (currUser.getId() != null) {
            StringBuilder sb = new StringBuilder("This bot have some data about you: \n\n");
            sb.append("Nickname: ").append(currUser.getNickName()).append("\n")
                    .append("FirstName: ").append(currUser.getFirstName()).append("\n")
                    .append("LastName: ").append(currUser.getLastName()).append("\n")
                    .append("Registered at: ").append(currUser.getRegisteredAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            messageToSend.setText(sb.toString()); //TODO: add inline button for delete my data command
        } else
            messageToSend.setText("We no have any data about you, press /start to register");
        try {
            absSender.execute(messageToSend);
        } catch (TelegramApiException e) {
            //log.error("Got some error in my data block: " + e.getMessage());
        }
    }
}