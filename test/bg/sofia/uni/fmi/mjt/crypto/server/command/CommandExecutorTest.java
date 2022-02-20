package bg.sofia.uni.fmi.mjt.crypto.server.command;

import bg.sofia.uni.fmi.mjt.crypto.server.cache.AssetsCache;
import bg.sofia.uni.fmi.mjt.crypto.server.dto.AssetDto;
import bg.sofia.uni.fmi.mjt.crypto.server.enums.TransactionType;
import bg.sofia.uni.fmi.mjt.crypto.server.exception.HttpException;
import bg.sofia.uni.fmi.mjt.crypto.server.exception.UnauthorizedException;
import bg.sofia.uni.fmi.mjt.crypto.server.exception.UserAlreadyExistException;
import bg.sofia.uni.fmi.mjt.crypto.server.model.Transaction;
import bg.sofia.uni.fmi.mjt.crypto.server.model.User;
import bg.sofia.uni.fmi.mjt.crypto.server.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandExecutorTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private AssetsCache assetsCache;

    @InjectMocks
    private CommandExecutor commandExecutor;

    @Test
    void testExecuteReturnsUnknownCommand()
        throws UserAlreadyExistException, HttpException, NoSuchAlgorithmException, InvalidKeySpecException, IOException,
        URISyntaxException {
        assertEquals("Unknown command", commandExecutor.execute(CommandCreator.newCommand("test")),
            "Unknown command was not reached");
        assertEquals("Unknown command", commandExecutor.execute(CommandCreator.newCommand("test"),
                null),
            "Unknown command was not reached");
    }

    @Test
    void testLoginSuccessfully()
        throws HttpException, UserAlreadyExistException, NoSuchAlgorithmException, InvalidKeySpecException, IOException,
        URISyntaxException {
        String actual = commandExecutor.execute(CommandCreator.newCommand("login ivan 1234"));

        assertEquals("Successfully logged in as ivan", actual, "Invalid login result");
    }

    @Test
    void testLoginThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
            () -> commandExecutor.execute(CommandCreator.newCommand("login ivan")),
            "IllegalArgumentException was not thrown");
    }

    @Test
    void testLoginThrowsUnauthorizedException()
        throws HttpException {
        doThrow(UnauthorizedException.class).when(userRepository).login(anyString(), anyString());

        assertThrows(UnauthorizedException.class,
            () -> commandExecutor.execute(CommandCreator.newCommand("login ivan 1234")),
            "UnauthorizedException was not thrown");
    }

    @Test
    void testRegisterSuccessfully()
        throws UserAlreadyExistException, HttpException, NoSuchAlgorithmException, InvalidKeySpecException, IOException,
        URISyntaxException {
        String actual = commandExecutor.execute(CommandCreator.newCommand("register ivan 1234"));

        assertEquals("Successfully registered as ivan", actual, "Invalid register result");
    }

    @Test
    void testRegisterThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
            () -> commandExecutor.execute(CommandCreator.newCommand("register ivan")),
            "IllegalArgumentException was not successfully");
    }

    @Test
    void testHelpReturnsDescription()
        throws UserAlreadyExistException, HttpException, NoSuchAlgorithmException, InvalidKeySpecException, IOException,
        URISyntaxException {
        String actual = commandExecutor.execute(CommandCreator.newCommand("help"));
        String expected = """
            Supported commands:
            login <username> <password> - Login with your wallet credentials
            register <username> <password> - Register in our system
            list-offerings - Shows 50 cryptos from the api
            deposit <amount> - Deposits amount to wallet
            buy --offering=<offering_code> --money=<amount> - Buy crypto by offering code (Only authenticated users)
            sell --offering=<offering_code> - Sell crypto by offering code (Only authenticated users)
            get-wallet-summary - Shows wallet money and profile transactions (Only authenticated users)
            get-wallet-overall-summary - Shows info about current crypto win/lose (Only authenticated users)""";
        assertEquals(expected, actual, "Incorrect help result");
    }

    @Test
    void testListOfferingsThrowsHttpException() throws URISyntaxException, HttpException {
        when(assetsCache.getAllAssets()).thenThrow(HttpException.class);

        assertThrows(HttpException.class, () ->
                commandExecutor.execute(CommandCreator.newCommand("list-offerings")),
            "list-offerings did not throw exception");
    }

    @Test
    void testListOfferingsReturnsAssets()
        throws HttpException, URISyntaxException, UserAlreadyExistException, NoSuchAlgorithmException,
        InvalidKeySpecException, IOException {
        AssetDto assetDto1 = new AssetDto("test", "test", 1, 100.0,
            "test",
            "test", "test");
        AssetDto assetDto2 = new AssetDto("test2", "test2", 1, 200.0,
            "test2",
            "test2", "test2");
        List<AssetDto> assetDtos = List.of(assetDto1, assetDto2);

        String expected = assetDtos.stream().map(AssetDto::toString).collect(Collectors.joining()).trim();
        when(assetsCache.getAllAssets()).thenReturn(assetDtos);

        String actual = commandExecutor.execute(CommandCreator.newCommand("list-offerings"));
        assertEquals(expected, actual, "Invalid response of list offerings");
    }

    @Test
    void testDepositReturnsSuccessfully() throws HttpException, IOException, URISyntaxException {
        User test = new User("test", "test", 100);
        String actual = commandExecutor.execute(CommandCreator.newCommand("deposit-money 1000"), test);

        assertEquals("1000.0 dollars were transferred to your account", actual,
            "1000.0 dollars were not transferred");
    }

    @Test
    void testDepositWithWrongMoneyThrowsException() {
        User test = new User("test", "test", 100);
        assertThrows(IllegalArgumentException.class,
            () -> commandExecutor.execute(CommandCreator.newCommand("deposit-money test"), test),
            "Command executor should have failed with invalid money as argument");
    }

    @Test
    void testDepositWithWrongArgumentsThrowsException() {
        User test = new User("test", "test", 100);
        assertThrows(IllegalArgumentException.class,
            () -> commandExecutor.execute(CommandCreator.newCommand("deposit-money"), test),
            "Command executor should have failed without amount as argument");
    }

    @Test
    void testBuyCryptoWithInvalidArgumentsThrowsInvalidArgumentsException() {
        User test = new User("test", "test", 100);
        assertThrows(IllegalArgumentException.class, () -> commandExecutor.execute(CommandCreator.newCommand(
            "buy"
        ), test), "Command executor should have failed with wrong arguments");
    }

    @Test
    void testBuyCryptoWithIllegalFormatOfOfferingCodeThrowsInvalidArgumentsException() {
        User test = new User("test", "test", 100);
        assertThrows(IllegalArgumentException.class, () -> commandExecutor.execute(CommandCreator.newCommand(
            "buy --offering-code= --money=1000"
        ), test), "Command executor should have failed with wrong format of offerings code");
    }

    @Test
    void testBuyCryptoWithMissingOfferingCodeThrowsInvalidArgumentsException() {
        User test = new User("test", "test", 100);
        assertThrows(IllegalArgumentException.class, () -> commandExecutor.execute(CommandCreator.newCommand(
            "buy --code=32 --money=1000"
        ), test), "Command executor should have failed with missing offering code");
    }

    @Test
    void testBuyCryptoWithIllegalFormatOfMoneyThrowsInvalidArgumentsException() {
        User test = new User("test", "test", 100);
        assertThrows(IllegalArgumentException.class, () -> commandExecutor.execute(CommandCreator.newCommand(
            "buy --offering=BTC --money="
        ), test), "Command executor should have failed with wrong format of money");
    }

    @Test
    void testBuyCryptoWithMissingMoneyThrowsInvalidArgumentsException() {
        User test = new User("test", "test", 100);
        assertThrows(IllegalArgumentException.class, () -> commandExecutor.execute(CommandCreator.newCommand(
            "buy --offering=BTC --amount=65"
        ), test), "Command executor should have failed with missing money");
    }

    @Test
    void testBuyCryptoWithWrongMoneyArgumentInvalidArgumentsException() {
        User test = new User("test", "test", 100);
        assertThrows(IllegalArgumentException.class, () -> commandExecutor.execute(CommandCreator.newCommand(
            "buy --offering=BTC --money=test"
        ), test), "Command executor should have failed with illegal number passed to money");
    }

    @Test
    void testBuyCryptoSuccessfulPurchase()
        throws UserAlreadyExistException, HttpException, NoSuchAlgorithmException, InvalidKeySpecException, IOException,
        URISyntaxException {
        User test = new User("test", "test", 100);
        String actual =
            commandExecutor.execute(CommandCreator.newCommand("buy --offering=BTC --money=1000"), test);

        assertEquals("Successfully bought BTC", actual, "BTC was not bought");
    }

    @Test
    void testSellWithoutArgumentThrowsIllegalArgumentException() {
        User test = new User("test", "test", 100);
        assertThrows(IllegalArgumentException.class, () -> commandExecutor.execute(CommandCreator.newCommand(
            "sell"
        ), test), "Command executor should have failed when no arguments passed");
    }

    @Test
    void testSellSuccessfullySoldCrypto() throws HttpException, IOException, URISyntaxException {
        User test = new User("test", "test", 100);
        String actual = commandExecutor.execute(CommandCreator.newCommand("sell --offering=BTC"), test);

        assertEquals("BTC was successfully sold", actual, "BTC was no sold");
    }

    @Test
    void testGetWalletSummaryReturnsCorrectWalletInfo() throws HttpException, IOException, URISyntaxException {
        AssetDto test1 = new AssetDto("test", "test", 1, 100.0,
            "test",
            "test", "test");
        AssetDto test2 = new AssetDto("test2", "test2", 1, 200.0,
            "test2",
            "test2", "test2");
        Transaction testTransaction1 = new Transaction(100, test1, TransactionType.BUY);
        Transaction testTransaction2 = new Transaction(100, test2, TransactionType.SELL);
        User test = new User("test", "test", 100.0);
        test.addTransaction(testTransaction1);
        test.addTransaction(testTransaction2);

        String transactionsStr = Stream.of(testTransaction1, testTransaction2)
            .map(Transaction::toString)
            .collect(Collectors.joining(System.lineSeparator()))
            .trim();
        String expected = String.format("%s\n%s", "Wallet balance: 100.00", transactionsStr);
        String actual = commandExecutor.execute(CommandCreator.newCommand("get-wallet-summary"), test);

        assertEquals(expected, actual, "Wallet summary is wrong");
    }

    @Test
    void testGetWalletOverallSummaryReturnsNoInfo() throws HttpException, IOException, URISyntaxException {
        User test = new User("test", "test", 100.0);

        String actual = commandExecutor.execute(CommandCreator.newCommand("get-wallet-overall-summary"),
            test);

        assertEquals("No info", actual, "No info was expected");
    }

    @Test
    void testGetWalletOverallSummaryReturnsInfo() throws HttpException, IOException, URISyntaxException {
        User test = new User("test", "test", 100.0);
        AssetDto test1 = new AssetDto("test", "test", 1, 100.0,
            "test",
            "test", "test");
        AssetDto test2 = new AssetDto("test2", "test2", 1, 200.0,
            "test2",
            "test2", "test2");
        test.addBoughtCrypto(test1, 100);
        test.addBoughtCrypto(test2, 200);

        when(assetsCache.getAssetByOfferingCode("test")).thenReturn(new AssetDto("test", "test",
            1, 150.0, "test", "test", "test"));
        when(assetsCache.getAssetByOfferingCode("test2")).thenReturn(new AssetDto("test2", "test2",
            1, 170.0,
            "test2",
            "test2", "test2"));

        String expected = """
            test {
                buyValue: '100.00',
                sellValue: '150.00',
                gained: '50.00',
                lost: '0.00'
            }
            test2 {
                buyValue: '200.00',
                sellValue: '170.00',
                gained: '0.00',
                lost: '30.00'
            }""";
        String actual = commandExecutor.execute(CommandCreator.newCommand("get-wallet-overall-summary"),
            test);

        assertEquals(expected, actual, "Invalid overall summary");
    }


}