package bg.sofia.uni.fmi.mjt.crypto.server.command;

import bg.sofia.uni.fmi.mjt.crypto.server.enums.CommandType;

public record Command(CommandType command, String[] arguments) {
}