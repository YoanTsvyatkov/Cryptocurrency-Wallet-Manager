package bg.sofia.uni.fmi.mjt.crypto.server.repository;

import bg.sofia.uni.fmi.mjt.crypto.server.dto.AssetDto;
import bg.sofia.uni.fmi.mjt.crypto.server.enums.TransactionType;
import bg.sofia.uni.fmi.mjt.crypto.server.exception.UnauthorizedException;
import bg.sofia.uni.fmi.mjt.crypto.server.exception.UserAlreadyExistException;
import bg.sofia.uni.fmi.mjt.crypto.server.model.Transaction;
import bg.sofia.uni.fmi.mjt.crypto.server.model.User;
import bg.sofia.uni.fmi.mjt.crypto.server.util.PasswordAuthentication;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class LocalUserRepository implements UserRepository {
    private final Path usersPath;
    private Map<String, User> users = new HashMap<>();
    private final PasswordAuthentication passwordAuthentication = new PasswordAuthentication();

    public LocalUserRepository(Path usersFile) throws IOException, ClassNotFoundException {
        this.usersPath = usersFile;
        loadUsers();
    }

    @Override
    public void register(String username, String password)
        throws IOException, UserAlreadyExistException {
        if (users.containsKey(username)) {
            throw new UserAlreadyExistException("This username is already used");
        }
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        String hashedPassword = passwordAuthentication.hash(password.toCharArray());
        User user = new User(username, hashedPassword, 0.0);
        users.put(username, user);
        persistUsers();
    }

    @Override
    public void deposit(User user, double money) throws IOException {
        if (!users.containsKey(user.getUsername())) {
            throw new IllegalArgumentException("This user does not exist in the database");
        }
        if (money <= 0) {
            throw new IllegalArgumentException("You cannot deposit negative amount of money");
        }

        final User dbUser = users.get(user.getUsername());
        dbUser.setMoney(dbUser.getMoney() + money);
        persistUsers();
    }

    @Override
    public void login(String username, String password) throws UnauthorizedException {
        User user = getUserByUsername(username);

        if (!passwordAuthentication.authenticate(password.toCharArray(), user.getPassword())) {
            throw new UnauthorizedException("Invalid username or password");
        }
    }

    @Override
    public User getUserByUsername(String username) {
        if (!users.containsKey(username)) {
            throw new IllegalArgumentException("User with this username does not exist");
        }

        return users.get(username);
    }


    @Override
    public void buyCrypto(User user, AssetDto assetDto, double money) throws IOException {
        if (money <= 0) {
            throw new IllegalArgumentException("Money cannot be zero or negative");
        }
        if (user.getMoney() < money) {
            throw new IllegalArgumentException("Not enough money to invest");
        }

        Transaction transaction = new Transaction(money, assetDto, TransactionType.BUY);
        User dbUser = users.get(user.getUsername());
        dbUser.addBoughtCrypto(assetDto, money);
        dbUser.addTransaction(transaction);
        persistUsers();
    }

    @Override
    public void sellCrypto(User user, AssetDto assetDto) throws IOException {
        if (!user.getBoughtCoins().containsKey(assetDto)) {
            throw new IllegalArgumentException("You cannot sell crypto that you don`t have");
        }

        final User dbUser = users.get(user.getUsername());
        double earnedMoney = dbUser.sellCrypto(assetDto);
        dbUser.addTransaction(new Transaction(earnedMoney, assetDto, TransactionType.SELL));
        persistUsers();
    }

    private void persistUsers() throws IOException {
        try (var outputStream = new ObjectOutputStream(Files.newOutputStream(usersPath))) {
            outputStream.writeObject(users);
        }
    }

    private void loadUsers() throws IOException, ClassNotFoundException {
        File file = usersPath.toFile();
        if (file.exists() && file.length() > 0) {
            try (var fileInputStream = Files.newInputStream(usersPath);
                 var objectInputStream = new ObjectInputStream(fileInputStream)
            ) {
                users = (Map<String, User>) objectInputStream.readObject();
            }
        }
    }
}
