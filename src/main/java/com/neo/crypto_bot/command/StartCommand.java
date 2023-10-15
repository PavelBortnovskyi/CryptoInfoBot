//package com.neo.crypto_bot.command;
//
//import com.neo.crypto_bot.constant.TextCommands;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
//import org.telegram.telegrambots.meta.api.objects.Message;
//import org.telegram.telegrambots.meta.bots.AbsSender;
//import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
//
//@Component
//public class StartCommand extends BaseTextCommand {
//    public StartCommand(@Value(TextCommands.START) String textCommandIdentified,
//                        @Value(TextCommands.START_DESCRIPTION) String description) {
//        super(textCommandIdentified, description);
//    }
//
//    @Override
//    public void processMessage(AbsSender absSender, Message message, String[] strings) {
//        var sendMessage = SendMessage.builder()
//                .text("I'm response from StartCommand")
//                .chatId(message.getChatId())
//                .build();
//        try {
//            absSender.execute(sendMessage);
//        } catch (TelegramApiException e) {
//            throw new RuntimeException(e);
//        }
//    }
//}
