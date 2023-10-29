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
public class DeleteMyDataCommandHandler extends BotCommand {

    private final BotUserRepository botUserRepository;

    public DeleteMyDataCommandHandler(@Value(TextCommands.DELETE_MY_DATA) String commandIdentifier,
                                      @Value(TextCommands.DMD_DESCRIPTION) String description,
                                      BotUserRepository botUserRepository) {
        super(commandIdentifier, description);
        this.botUserRepository = botUserRepository;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        long chatId = chat.getId();
        SendMessage messageToSend = SendMessage.builder().chatId(chatId).text("").build();
        if (botUserRepository.findById(chatId).isPresent()) {
            botUserRepository.deleteById(chatId);
            messageToSend.setText("Info about you was successfully deleted, you can check it by /my_data command");
            //log.info("Deleted data about user with chatID: " + chatId);
        } else
            messageToSend.setText("We no have any data about you, press /start to register");
        try {
            absSender.execute(messageToSend);
        } catch (TelegramApiException e) {
            //log.error("Got some error in delete my data block: " + e.getMessage());
        }
    }
}