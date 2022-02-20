package bg.sofia.uni.fmi.mjt.crypto.server.dto;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Objects;

public class AssetDto implements Serializable {
    @SerializedName("asset_id")
    private String assetId;

    private String name;

    @SerializedName("type_is_crypto")
    private int typeIsCrypto;

    @SerializedName("price_usd")
    private double priceUsd;

    @SerializedName("id_icon")
    private String idIcon;

    @SerializedName("data_start")
    private String dataStart;

    @SerializedName("data_end")
    private String dataEnd;

    public AssetDto(
        String assetId,
        String name,
        int typeIsCrypto,
        double priceUsd,
        String idIcon,
        String dataStart,
        String dataEnd
    ) {
        this.assetId = assetId;
        this.name = name;
        this.typeIsCrypto = typeIsCrypto;
        this.priceUsd = priceUsd;
        this.idIcon = idIcon;
        this.dataStart = dataStart;
        this.dataEnd = dataEnd;
    }

    public String assetId() {
        return assetId;
    }

    public String name() {
        return name;
    }

    public int typeIsCrypto() {
        return typeIsCrypto;
    }

    public double priceUsd() {
        return priceUsd;
    }

    public String idIcon() {
        return idIcon;
    }

    public String dataStart() {
        return dataStart;
    }

    public String dataEnd() {
        return dataEnd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssetDto)) return false;
        AssetDto assetDto = (AssetDto) o;
        return assetId.equals(assetDto.assetId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assetId);
    }

    @Override
    public String toString() {
        return String.format("""
            Asset {
                assetId: '%s',
                name: '%s',
                priceUsd: '%f',
                dataStart: '%s',
                dataEnd: '%s'
            }
            """, assetId, name, priceUsd, dataStart, dataEnd);
    }

}
