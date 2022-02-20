package bg.sofia.uni.fmi.mjt.crypto.server.repository;

import bg.sofia.uni.fmi.mjt.crypto.server.dto.AssetDto;
import bg.sofia.uni.fmi.mjt.crypto.server.util.ApiResponse;

import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AssetRepository {
    CompletableFuture<ApiResponse<List<AssetDto>>> getAssets() throws URISyntaxException;

    CompletableFuture<ApiResponse<AssetDto>> getAssetById(String assetId) throws URISyntaxException;
}
