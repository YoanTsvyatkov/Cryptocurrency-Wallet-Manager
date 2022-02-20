package bg.sofia.uni.fmi.mjt.crypto.server.repository;

import bg.sofia.uni.fmi.mjt.crypto.server.exception.HttpException;
import bg.sofia.uni.fmi.mjt.crypto.server.exception.UnauthorizedException;
import bg.sofia.uni.fmi.mjt.crypto.server.exception.UserAlreadyExistException;
import bg.sofia.uni.fmi.mjt.crypto.server.dto.AssetDto;
import bg.sofia.uni.fmi.mjt.crypto.server.model.User;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public interface UserRepository {
    void register(String username, String password)
        throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, HttpException, UserAlreadyExistException;

    void login(String username, String password) throws UnauthorizedException;

    User getUserByUsername(String username);

    void deposit(User user, double money) throws IOException;

    void buyCrypto(User user, AssetDto assetDto, double money) throws IOException;

    void sellCrypto(User user, AssetDto assetDto) throws IOException;
}
