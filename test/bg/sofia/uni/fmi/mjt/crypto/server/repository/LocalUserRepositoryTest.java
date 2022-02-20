package bg.sofia.uni.fmi.mjt.crypto.server.repository;

import bg.sofia.uni.fmi.mjt.crypto.server.dto.AssetDto;
import bg.sofia.uni.fmi.mjt.crypto.server.enums.TransactionType;
import bg.sofia.uni.fmi.mjt.crypto.server.exception.HttpException;
import bg.sofia.uni.fmi.mjt.crypto.server.exception.UnauthorizedException;
import bg.sofia.uni.fmi.mjt.crypto.server.exception.UserAlreadyExistException;
import bg.sofia.uni.fmi.mjt.crypto.server.model.Transaction;
import bg.sofia.uni.fmi.mjt.crypto.server.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalUserRepositoryTest {
    private static final String TEST_FILE_NAME = "resources/test_users.txt";
    private static final Path FILE_PATH = Path.of(TEST_FILE_NAME);
    private static UserRepository userRepository;

    @BeforeEach
    void setup() throws IOException, ClassNotFoundException {
        if (!Files.exists(FILE_PATH)) {
            Files.createFile(FILE_PATH);
        }

        userRepository = new LocalUserRepository(FILE_PATH);
    }

    @AfterEach
    void teardown() throws IOException {
        Files.deleteIfExists(FILE_PATH);
    }

    @Test
    void testRegisterUser()
        throws UserAlreadyExistException, HttpException, NoSuchAlgorithmException, InvalidKeySpecException,
        IOException {
        userRepository.register("ivan", "1234");
        User ivan = userRepository.getUserByUsername("ivan");

        assertEquals("ivan", ivan.getUsername(), "Registered user is not the same");
        assertNotEquals("1234", ivan.getPassword(), "Password should not matched with its hash");
    }

    @Test
    void testRegisterUserWithExistingUsernameThrowsUserAlreadyExistsException()
        throws UserAlreadyExistException, HttpException, NoSuchAlgorithmException, InvalidKeySpecException,
        IOException {
        userRepository.register("ivan", "1234");

        assertThrows(UserAlreadyExistException.class,
            () -> userRepository.register("ivan", "1234"),
            "Cannot have two users with the same username");
    }

    @Test
    void testRegisterUserWithEmptyUsername() {
        assertThrows(IllegalArgumentException.class,
            () -> userRepository.register("", "1234"),
            "Cannot have user with empty username");
    }

    @Test
    void testLoginUserThatDoesNotExists()
        throws UserAlreadyExistException, HttpException, NoSuchAlgorithmException, InvalidKeySpecException,
        IOException {
        assertThrows(IllegalArgumentException.class,
            () -> userRepository.login("dsa", "1234"),
            "Cannot user with empty username");
    }

    @Test
    void testLoginWithInvalidCredentialsThrowsUnauthorizedException()
        throws UserAlreadyExistException, HttpException, NoSuchAlgorithmException, InvalidKeySpecException,
        IOException {
        userRepository.register("ivan", "1234");

        assertThrows(UnauthorizedException.class, () -> userRepository.login("ivan", "test"));
    }

    @Test
    void testDepositMoneyWithInvalidUserThrowsIllegalArgumentException()
        throws IOException, UserAlreadyExistException, HttpException, NoSuchAlgorithmException,
        InvalidKeySpecException {
        User test = new User("ivan", "1233", 200);
        assertThrows(IllegalArgumentException.class, () -> userRepository.deposit(test, 12),
            "User that is not in our db cannot deposit money");
    }

    @Test
    void testDepositMoneyWithNegativeAmountThrowsIllegalArgumentException()
        throws IOException, UserAlreadyExistException, HttpException, NoSuchAlgorithmException,
        InvalidKeySpecException {
        userRepository.register("ivan", "1234");

        User test = new User("ivan", "1233", 200);
        assertThrows(IllegalArgumentException.class, () -> userRepository.deposit(test, -12),
            "Negative amount is not amount");
    }

    @Test
    void testDepositMoneyDepositsMoneyCorrectly()
        throws IOException, UserAlreadyExistException, HttpException, NoSuchAlgorithmException,
        InvalidKeySpecException {
        userRepository.register("ivan", "1234");

        User test = new User("ivan", "1234", 0);
        userRepository.deposit(test, 400);
        User dbUser = userRepository.getUserByUsername("ivan");

        assertEquals(400, dbUser.getMoney(), "Invalid wallet balance after deposit");
    }

    @Test
    void testBuyCryptoWithNegativeAmountThrowsIllegalArgumentException() throws IOException {
        User test = new User("ivan", "1234", 0);
        AssetDto testAsset = new AssetDto("test", "test", 1, 100.0,
            "test",
            "test", "test");

        assertThrows(IllegalArgumentException.class, () -> userRepository.buyCrypto(test, testAsset, -1),
            "Buying crypto with negative amount should throw exception");
    }

    @Test
    void testBuyCryptoWithInsufficientAmountInWalletThrowsIllegalArgumentException() throws IOException {
        User test = new User("ivan", "1234", 200);
        AssetDto testAsset = new AssetDto("test", "test", 1, 100.0,
            "test",
            "test", "test");

        assertThrows(IllegalArgumentException.class, () -> userRepository.buyCrypto(test, testAsset, 300),
            "Buying crypto without having money should throw exception");
    }


    @Test
    void testBuyCryptoExecutesBuyTransaction()
        throws IOException, UserAlreadyExistException, HttpException, NoSuchAlgorithmException,
        InvalidKeySpecException {
        userRepository.register("ivan", "1234");

        User test = new User("ivan", "1234", 200);
        AssetDto testAsset = new AssetDto("test", "test", 1, 100.0,
            "test",
            "test", "test");
        List<Transaction> transactions = List.of(new Transaction(190, testAsset, TransactionType.BUY));

        userRepository.buyCrypto(test, testAsset, 190);
        User dbUser = userRepository.getUserByUsername("ivan");

        assertEquals(190, dbUser.getMoneySpend().get(testAsset), "Invalid money spend on crypto");
        assertEquals(dbUser.getTransactions(), transactions);
    }

    @Test
    void testSellCryptoWithNotBoughtAssetThrowsException()
        throws IOException, UserAlreadyExistException, HttpException, NoSuchAlgorithmException,
        InvalidKeySpecException {
        userRepository.register("ivan", "1234");

        User test = new User("ivan", "1234", 200);
        AssetDto testAsset = new AssetDto("test", "test", 1, 100.0,
            "test",
            "test", "test");

        assertThrows(IllegalArgumentException.class, () -> userRepository.sellCrypto(test, testAsset),
            "Trying to sell not bought asset is not possible");
    }

    @Test
    void testSellCryptoSellsAsset()
        throws UserAlreadyExistException, HttpException, NoSuchAlgorithmException, InvalidKeySpecException,
        IOException {
        userRepository.register("ivan", "1234");
        User test = new User("ivan", "1234", 200);
        AssetDto testAsset = new AssetDto("test", "test", 1, 100.0,
            "test",
            "test", "test");

        userRepository.buyCrypto(test, testAsset, 190);
        User dbUser = userRepository.getUserByUsername("ivan");
        userRepository.sellCrypto(dbUser, testAsset);
        dbUser = userRepository.getUserByUsername("ivan");
        List<Transaction> transactions = List.of(new Transaction(190, testAsset, TransactionType.BUY),
            new Transaction(190, testAsset, TransactionType.SELL));

        assertIterableEquals(dbUser.getTransactions(), transactions, "Invalid transactions");
    }
}