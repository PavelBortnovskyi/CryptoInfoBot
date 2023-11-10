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

import java.util.Locale;
import java.util.Optional;

//@Log4j2
@Component
public class HelpCommandHandler extends BotCommand {

    private final BotUserRepository botUserRepository;

    public HelpCommandHandler(@Value(TextCommands.HELP) String commandIdentifier,
                              @Value(TextCommands.HELP_DESCRIPTION) String description,
                              BotUserRepository botUserRepository) {
        super(commandIdentifier, description);
        this.botUserRepository = botUserRepository;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        Optional<BotUser> maybeCurrUser = botUserRepository.findById(chat.getId());
        maybeCurrUser.ifPresent(botUser -> LocalizationManager.setLocale(new Locale(botUser.getLanguage().toLowerCase())));
        SendMessage messageToSend = SendMessage.builder()
                .chatId(chat.getId())
                .text(LocalizationManager.getString("help_message"))
                .build();
        try {
            absSender.execute(messageToSend);
        } catch (TelegramApiException e) {
            //log.error("Got some error in help block: " + e.getMessage());
        }
    }
}