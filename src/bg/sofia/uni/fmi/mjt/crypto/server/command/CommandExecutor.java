package bg.sofia.uni.fmi.mjt.crypto.server.command;

import bg.sofia.uni.fmi.mjt.crypto.server.cache.AssetsCache;
import bg.sofia.uni.fmi.mjt.crypto.server.dto.AssetDto;
import bg.sofia.uni.fmi.mjt.crypto.server.exception.HttpException;
import bg.sofia.uni.fmi.mjt.crypto.server.exception.UnauthorizedException;
import bg.sofia.uni.fmi.mjt.crypto.server.exception.UserAlreadyExistException;
import bg.sofia.uni.fmi.mjt.crypto.server.model.Transaction;
import bg.sofia.uni.fmi.mjt.crypto.server.model.User;
import bg.sofia.uni.fmi.mjt.crypto.server.repository.UserRepository;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.Map;

public class CommandExecutor {
    private static final String UNKNOWN_COMMAND = "Unknown command";
    private static final String NO_INFO_COMMAND = "No info";
    private static final String OFFERINGS = "--offering";
    private static final int TWO_ARGUMENTS_COMMAND_LENGTH = 2;
    private static final String MONEY = "--money";
    private static final String SEPARATOR = "=";

    private AssetsCache assetsCache;
    private UserRepository userRepository;

    public CommandExecutor(AssetsCache assetsCache,
                           UserRepository userRepository) {
        this.assetsCache = assetsCache;
        this.userRepository = userRepository;
    }

    public String execute(Command command)
        throws HttpException, NoSuchAlgorithmException, InvalidKeySpecException,
        IOException, URISyntaxException, UserAlreadyExistException {
        return switch (command.command()) {
            case LOGIN -> login(command);
            case REGISTER -> register(command);
            case GET_CRYPTO -> getCrypto();
            case HELP -> help();
            default -> UNKNOWN_COMMAND;
        };
    }

    public String execute(Command command, User user) throws IOException, HttpException, URISyntaxException {
        return switch (command.command()) {
            case DEPOSIT -> deposit(command, user);
            case BUY_CRYPTO -> buyCrypto(command, user);
            case SELL_CRYPTO -> sellCrypto(command, user);
            case WALLET_SUMMERY -> getWalletSummery(user);
            case WALLET_OVERALL_SUMMERY -> getWalletOverallSummery(user);
            default -> UNKNOWN_COMMAND;
        };
    }

    private String help() {
        return """
                Supported commands:
                login <username> <password> - Login with your wallet credentials
                register <username> <password> - Register in our system
                list-offerings - Shows 50 cryptos from the api
                deposit <amount> - Deposits amount to wallet
                buy --offering=<offering_code> --money=<amount> - Buy crypto by offering code (Only authenticated users)
                sell --offering=<offering_code> - Sell crypto by offering code (Only authenticated users)
                get-wallet-summary - Shows wallet money and profile transactions (Only authenticated users)
                get-wallet-overall-summary - Shows info about current crypto win/lose (Only authenticated users)""";
    }

    private String getWalletSummery(User user) {
        double moneyInAccount = user.getMoney();
        List<Transaction> transactions = user.getTransactions();
        StringBuilder result = new StringBuilder();

        result.append(String.format("Wallet balance: %.02f\n", moneyInAccount));
        for (Transaction transaction : transactions) {
            result.append(transaction.toString());
            result.append(System.lineSeparator());
        }
        return result.toString().trim();
    }

    private String getWalletOverallSummery(User user) throws HttpException, URISyntaxException {
        Map<AssetDto, Double> moneySpendMap = user.getMoneySpend();
        Map<AssetDto, Double> boughtCoins = user.getBoughtCoins();
        StringBuilder res = new StringBuilder();

        for (Map.Entry<AssetDto, Double> entrySet : moneySpendMap.entrySet()) {
            double moneySpend = entrySet.getValue();
            double numOfCoins = boughtCoins.get(entrySet.getKey());
            AssetDto asset = assetsCache.getAssetByOfferingCode(entrySet.getKey().assetId());
            double currentMarketMoney = numOfCoins * asset.priceUsd();
            double diff = moneySpend - currentMarketMoney;
            double gained = 0.0;
            double lost = 0.0;

            if (diff <= 0) {
                gained = Math.abs(diff);
            } else {
                lost = diff;
            }

            res.append(String.format("""
                %s {
                    buyValue: '%.02f',
                    sellValue: '%.02f',
                    gained: '%.02f',
                    lost: '%.02f'
                }""", asset.name(), moneySpend, currentMarketMoney, gained, lost));
            res.append("\n");
        }

        if (res.isEmpty()) {
            return NO_INFO_COMMAND;
        }

        return res.toString().trim();
    }

    private String getCrypto() throws HttpException, URISyntaxException {
        StringBuilder stringBuilder = new StringBuilder();
        for (AssetDto asset : assetsCache.getAllAssets()) {
            stringBuilder.append(asset.toString());
        }

        return stringBuilder.toString().trim();
    }

    private String deposit(Command command, User user) throws IOException {
        String[] arguments = command.arguments();
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Invalid arguments to deposit");
        }

        try {
            double money = Double.parseDouble(arguments[0]);
            userRepository.deposit(user, money);
            return String.format("%s dollars were transferred to your account", money);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Illegal money argument");
        }
    }

    private String sellCrypto(Command command, User user)
        throws HttpException,
        URISyntaxException, IOException {
        String[] arguments = command.arguments();
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Invalid arguments to sell");
        }

        String offeringCode = getOfferingCode(arguments[0]);
        AssetDto asset = assetsCache.getAssetByOfferingCode(offeringCode);
        userRepository.sellCrypto(user, asset);
        return String.format("%s was successfully sold", offeringCode);
    }

    private String buyCrypto(Command command, User user)
        throws HttpException,
        URISyntaxException, IOException {
        String[] arguments = command.arguments();
        if (arguments.length != TWO_ARGUMENTS_COMMAND_LENGTH) {
            throw new IllegalArgumentException("Invalid arguments to buy");
        }

        String offeringCode = getOfferingCode(arguments[0]);
        String[] moneyArgument = arguments[1].split(SEPARATOR);
        if (moneyArgument.length != TWO_ARGUMENTS_COMMAND_LENGTH) {
            throw new IllegalArgumentException("Illegal format of money argument");
        }
        if (!MONEY.equals(moneyArgument[0])) {
            throw new IllegalArgumentException("Money argument was not passed");
        }

        try {
            double money = Double.parseDouble(moneyArgument[1]);
            AssetDto asset = assetsCache.getAssetByOfferingCode(offeringCode);

            userRepository.buyCrypto(user, asset, money);
            return String.format("Successfully bought %s", offeringCode);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Illegal money argument");
        }
    }

    private String getOfferingCode(String offeringCode) {
        String[] offeringCodeArgument = offeringCode.split(SEPARATOR);
        if (offeringCodeArgument.length != TWO_ARGUMENTS_COMMAND_LENGTH) {
            throw new IllegalArgumentException("Illegal format of offering code");
        }
        if (!OFFERINGS.equals(offeringCodeArgument[0])) {
            throw new IllegalArgumentException("Offering code was not passed");
        }

        return offeringCodeArgument[1];
    }

    private String login(Command command) throws UnauthorizedException {
        String[] arguments = command.arguments();
        if (arguments.length != TWO_ARGUMENTS_COMMAND_LENGTH) {
            throw new IllegalArgumentException("Invalid arguments to login command");
        }

        userRepository.login(arguments[0], arguments[1]);
        return String.format("Successfully logged in as %s", arguments[0]);
    }

    private String register(Command command)
        throws HttpException, NoSuchAlgorithmException, InvalidKeySpecException, IOException,
        UserAlreadyExistException {
        String[] arguments = command.arguments();
        if (arguments.length != TWO_ARGUMENTS_COMMAND_LENGTH) {
            throw new IllegalArgumentException("Invalid arguments to register command");
        }

        userRepository.register(arguments[0], arguments[1]);
        return String.format("Successfully registered as %s", arguments[0]);
    }
}
