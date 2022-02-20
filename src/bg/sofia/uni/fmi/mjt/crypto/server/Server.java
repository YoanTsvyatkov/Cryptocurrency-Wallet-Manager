package bg.sofia.uni.fmi.mjt.crypto.server;

import bg.sofia.uni.fmi.mjt.crypto.server.cache.AssetsCache;
import bg.sofia.uni.fmi.mjt.crypto.server.command.Command;
import bg.sofia.uni.fmi.mjt.crypto.server.command.CommandCreator;
import bg.sofia.uni.fmi.mjt.crypto.server.command.CommandExecutor;
import bg.sofia.uni.fmi.mjt.crypto.server.enums.CommandType;
import bg.sofia.uni.fmi.mjt.crypto.server.exception.HttpException;
import bg.sofia.uni.fmi.mjt.crypto.server.exception.UserAlreadyExistException;
import bg.sofia.uni.fmi.mjt.crypto.server.model.User;
import bg.sofia.uni.fmi.mjt.crypto.server.repository.AssetRepository;
import bg.sofia.uni.fmi.mjt.crypto.server.repository.LocalUserRepository;
import bg.sofia.uni.fmi.mjt.crypto.server.repository.RemoteAssetRepository;
import bg.sofia.uni.fmi.mjt.crypto.server.repository.UserRepository;
import bg.sofia.uni.fmi.mjt.crypto.server.util.ErrorLoggerUtil;
import bg.sofia.uni.fmi.mjt.crypto.server.util.FileConstants;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Iterator;
import java.util.concurrent.CompletionException;

public class Server {
    private static final int SERVER_PORT = 7777;
    private static final int BUFFER_SIZE = 8192;
    private static final String HOST = "localhost";
    private final UserRepository userRepository;
    private final CommandExecutor commandExecutor;

    private final int port;
    private boolean isServerWorking = true;

    private final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
    private Selector selector;

    public Server(int port, CommandExecutor commandExecutor, UserRepository userRepository) {
        this.port = port;
        this.commandExecutor = commandExecutor;
        this.userRepository = userRepository;
    }

    public void start() {
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            selector = Selector.open();
            configureServerSocketChannel(serverSocketChannel, selector);
            while (isServerWorking) {
                try {
                    int readyChannels = selector.select();
                    if (readyChannels == 0) {
                        continue;
                    }

                    Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                    while (keyIterator.hasNext()) {
                        SelectionKey key = keyIterator.next();

                        if (key.isReadable()) {
                            SocketChannel clientChannel = (SocketChannel) key.channel();
                            String clientInput = getClientInput(clientChannel);
                            if (clientInput == null) {
                                clientChannel.close();
                                keyIterator.remove();
                                continue;
                            }

                            try {
                                executeClientCommand(key, clientChannel, clientInput);
                            } catch (UserAlreadyExistException | HttpException | NoSuchAlgorithmException
                                | InvalidKeySpecException | IllegalArgumentException | URISyntaxException e) {
                                writeClientOutput(clientChannel, e.getMessage());
                            } catch (CompletionException e) {
                                ErrorLoggerUtil.logException(e);
                                writeClientOutput(clientChannel,
                                    "Please check your internet connection and try again");
                            } catch (Exception e) {
                                ErrorLoggerUtil.logException(e);
                                writeClientOutput(clientChannel, "Something went wrong... Please try again later");
                            }
                        } else if (key.isAcceptable()) {
                            accept(selector, key);
                        }

                        keyIterator.remove();
                    }
                } catch (IOException e) {
                    ErrorLoggerUtil.logException(e);
                    System.out.println("Error occurred while processing client request: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            ErrorLoggerUtil.logException(e);
        }
    }

    private void executeClientCommand(SelectionKey key, SocketChannel clientChannel, String clientInput)
        throws UserAlreadyExistException, NoSuchAlgorithmException, InvalidKeySpecException,
        IOException, HttpException, URISyntaxException {
        Command command = CommandCreator.newCommand(clientInput);
        CommandType commandType = command.command();

        String output = switch (commandType) {
            case LOGIN, REGISTER: {
                String commandRes = commandExecutor.execute(command);
                User user = userRepository.getUserByUsername(command.arguments()[0]);
                key.attach(user);
                yield commandRes;
            }
            case GET_CRYPTO, HELP: {
                 yield commandExecutor.execute(command);
            }
            default: {
                if (key.attachment() == null) {
                    yield "Unregistered user cannot execute this command";
                }

                User user = (User) key.attachment();
                String commandRes = commandExecutor.execute(command, user);
                User newUser = userRepository.getUserByUsername(user.getUsername());
                key.attach(newUser);
                yield commandRes;
                //falls through
            }
        };

        writeClientOutput(clientChannel, output);
    }

    public static void main(String[] args) {
        AssetRepository assetRepository = new RemoteAssetRepository();
        try {
            UserRepository userRepo = new LocalUserRepository(Path.of(FileConstants.USERS_FILE));
            AssetsCache assetsCache = new AssetsCache(assetRepository);

            CommandExecutor commandExecutorFactory = new CommandExecutor(assetsCache, userRepo);
            Server server = new Server(SERVER_PORT, commandExecutorFactory, userRepo);
            server.start();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error occurred while starting server: " + e.getMessage());
        }
    }

    public void stop() {
        this.isServerWorking = false;
        if (selector.isOpen()) {
            selector.wakeup();
        }
    }

    private void configureServerSocketChannel(ServerSocketChannel channel, Selector selector) throws IOException {
        channel.bind(new InetSocketAddress(HOST, this.port));
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_ACCEPT);
    }

    private String getClientInput(SocketChannel clientChannel) throws IOException {
        buffer.clear();

        int readBytes = clientChannel.read(buffer);
        if (readBytes < 0) {
            clientChannel.close();
            return null;
        }

        buffer.flip();

        byte[] clientInputBytes = new byte[buffer.remaining()];
        buffer.get(clientInputBytes);

        return new String(clientInputBytes, StandardCharsets.UTF_8);
    }

    private void writeClientOutput(SocketChannel clientChannel, String output) throws IOException {
        buffer.clear();
        buffer.put(output.getBytes());
        buffer.flip();

        clientChannel.write(buffer);
    }

    private void accept(Selector selector, SelectionKey key) throws IOException {
        ServerSocketChannel sockChannel = (ServerSocketChannel) key.channel();
        SocketChannel accept = sockChannel.accept();

        accept.configureBlocking(false);
        accept.register(selector, SelectionKey.OP_READ);
    }
}