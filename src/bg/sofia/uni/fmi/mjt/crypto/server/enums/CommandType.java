package bg.sofia.uni.fmi.mjt.crypto.server.enums;

public enum CommandType {
    LOGIN("login"),
    REGISTER("register"),
    DEPOSIT("deposit-money"),
    BUY_CRYPTO("buy"),
    SELL_CRYPTO("sell"),
    GET_CRYPTO("list-offerings"),
    WALLET_SUMMERY("get-wallet-summary"),
    WALLET_OVERALL_SUMMERY("get-wallet-overall-summary"),
    HELP("help"),
    UNKNOWN("");

    public final String name;

    public static CommandType valueOfCommandName(String name) {
        for (CommandType commandType : values()) {
            if (commandType.name.equals(name)) {
                return commandType;
            }
        }

        return UNKNOWN;
    }

    CommandType(String name) {
        this.name = name;
    }
}
