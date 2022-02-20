package bg.sofia.uni.fmi.mjt.crypto.server.model;

import bg.sofia.uni.fmi.mjt.crypto.server.dto.AssetDto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User implements Serializable {
    private String username;
    private String password;
    private double money;
    private final Map<AssetDto, Double> boughtCoins;
    private final Map<AssetDto, Double> moneySpend;
    private final List<Transaction> transactions;

    public User(String username, String password, double money) {
        this.username = username;
        this.password = password;
        this.money = money;
        this.boughtCoins = new HashMap<>();
        this.transactions = new ArrayList<>();
        this.moneySpend = new HashMap<>();
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }


    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
    }

    public void addBoughtCrypto(AssetDto assetDto, double investedMoney) {
        money -= investedMoney;

        if (boughtCoins.containsKey(assetDto)) {
            double currentCoins = boughtCoins.get(assetDto);
            double newCoins = investedMoney / assetDto.priceUsd();
            double currentMoneySpend = moneySpend.get(assetDto);

            boughtCoins.put(assetDto, currentCoins + newCoins);
            moneySpend.put(assetDto, currentMoneySpend + investedMoney);
            return;
        }

        boughtCoins.put(assetDto, investedMoney / assetDto.priceUsd());
        moneySpend.put(assetDto, investedMoney);
    }

    public double sellCrypto(AssetDto assetDto) {
        double coins = boughtCoins.get(assetDto);
        money += coins * assetDto.priceUsd();
        boughtCoins.remove(assetDto);
        moneySpend.remove(assetDto);

        return coins * assetDto.priceUsd();
    }

    public Map<AssetDto, Double> getBoughtCoins() {
        return boughtCoins;
    }

    public Map<AssetDto, Double> getMoneySpend() {
        return moneySpend;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        this.money = money;
    }

    @Override
    public String toString() {
        return String.format("""
            User {
                username: '%s',
                password: '%s',
                money: '%s'
            }""", username, password, money);
    }
}
