package com.neo.crypto_bot.constant;

import lombok.Getter;

@Getter
public enum TextCommands2 {

    START("start", "get started"),

    MY_DATA("my_data", "get info about user"),

    DELETE_MY_DATA("delete_my_data", "remove all info about user"),

    HELP("help", "get full commands list"),

    ADD_PAIR("add_pair", "add pair to favorites"),

    REMOVE_PAIR("remove_pair", "removes pair from favorites"),

    GET_ALL_FAVORITE_PAIRS("get_all_favorite_pairs", "get currencies of pairs from favorites list"),

    GET_POPULAR_PAIRS("get_popular_pairs", "get top 25 frequently searched pairs with currencies"),

    SETTINGS("settings", "set your preferences");

    public final String command;

    public final String description;

    TextCommands2(String command, String description) {
        this.command = command;
        this.description = description;
    }
}
