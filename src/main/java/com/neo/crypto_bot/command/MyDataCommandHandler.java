package com.neo.crypto_bot.command;

import com.neo.crypto_bot.constant.TextCommands;
import com.neo.crypto_bot.model.BotUser;
import com.neo.crypto_bot.repository.BotUserRepository;
import com.neo.crypto_bot.service.LocalizationManager;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLOutput;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

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
        Optional<BotUser> maybeCurrUser = botUserRepository.findById(chat.getId());
        SendMessage messageToSend = SendMessage.builder().chatId(chat.getId()).text("").build();
        if (maybeCurrUser.isPresent()) {
            BotUser currUser = maybeCurrUser.get();
            LocalizationManager.setLocale(new Locale(currUser.getLanguage().toLowerCase()));
            String answer = MessageFormat.format(LocalizationManager.getString("my_data_message"),
                    currUser.getNickName(), currUser.getFirstName(), currUser.getLastName(),
                    currUser.getRegisteredAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    currUser.getLanguage());
            messageToSend.setText(answer);
        } else {
            messageToSend.setText("We no have any data about you, press /start to register");
        }
        try {
            absSender.execute(messageToSend);
        } catch (TelegramApiException e) {
            System.out.println("Got some error in my data block:" + e.getMessage() + ":Thread" + Thread.currentThread());
            //log.error("Got some error in my data block: " + e.getMessage());
        }
    }
}