package com.neo.crypto_bot.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class CommandParser {

    public List<String> getPairsFromCommand(String command) {
        List<String> pairs = new ArrayList<>();
        String userInput = command.replaceAll(" ", "").toUpperCase();
        if (userInput.contains(",")) {
            pairs.addAll(Arrays.asList(userInput.split(",")));
        } else pairs.add(userInput);
        return pairs;
    }
}
