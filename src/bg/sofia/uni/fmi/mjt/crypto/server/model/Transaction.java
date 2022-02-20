package bg.sofia.uni.fmi.mjt.crypto.server.model;

import bg.sofia.uni.fmi.mjt.crypto.server.dto.AssetDto;
import bg.sofia.uni.fmi.mjt.crypto.server.enums.TransactionType;

import java.io.Serializable;

public record Transaction(
    double amount,
    AssetDto asset,
    TransactionType transactionType
) implements Serializable {

    @Override
    public String toString() {
        return String.format("""
                Transaction {
                    amount: '%s',
                    transactionType: '%s',
                    asset: {
                        name: '%s',
                        priceUsd: '%f',
                        dataStart: '%s',
                        dataEnd: '%s'
                    }
                }""", amount, transactionType, asset.name(), asset.priceUsd(), asset.dataStart(), asset.dataEnd());
    }
}
