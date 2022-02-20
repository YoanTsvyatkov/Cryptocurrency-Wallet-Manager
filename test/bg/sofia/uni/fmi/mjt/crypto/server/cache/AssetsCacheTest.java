package bg.sofia.uni.fmi.mjt.crypto.server.cache;

import bg.sofia.uni.fmi.mjt.crypto.server.dto.AssetDto;
import bg.sofia.uni.fmi.mjt.crypto.server.exception.HttpException;
import bg.sofia.uni.fmi.mjt.crypto.server.repository.AssetRepository;
import bg.sofia.uni.fmi.mjt.crypto.server.util.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetsCacheTest {
    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private AssetsCache assetsCache;

    @Test
    void testGetAssetByOfferingCodeWithEmptyOfferingCodeThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> assetsCache.getAssetByOfferingCode(""));
    }

    @Test
    void testGetAssetByOfferingCodeReturnsCorrectAsset() throws URISyntaxException, HttpException {
        AssetDto testAsset = new AssetDto("test", "test", 1, 100.0,
            "test",
            "test", "test");
        ApiResponse<AssetDto> apiResponse = new ApiResponse<>(
            testAsset,
            HTTP_OK, null
        );
        when(assetRepository.getAssetById(anyString())).thenReturn(
            CompletableFuture.completedFuture(apiResponse)
        );

        assertEquals(testAsset, assetsCache.getAssetByOfferingCode("test"),
            "Incorrect asset returned from cache");
    }

    @Test
    void testGetAssetByOfferingCodeWithNonExistingOfferingCodeThrowsException() throws URISyntaxException {
        ApiResponse<AssetDto> apiResponse = new ApiResponse<>(
            null,
            HTTP_OK, null
        );
        when(assetRepository.getAssetById(anyString())).thenReturn(
            CompletableFuture.completedFuture(apiResponse)
        );

        assertThrows(IllegalArgumentException.class, () -> assetsCache.getAssetByOfferingCode("test"),
            "Response with null data should have resulted in IllegalArgumentException");
    }

    @Test
    void testGetAssetByOfferingCodeThrowsHttpException() throws URISyntaxException {
        AssetDto testAsset = new AssetDto("test", "test", 1, 100.0,
            "test",
            "test", "test");
        ApiResponse<AssetDto> apiResponse = new ApiResponse<>(
            testAsset,
            HTTP_BAD_REQUEST, null
        );
        when(assetRepository.getAssetById(anyString())).thenReturn(
            CompletableFuture.completedFuture(apiResponse)
        );

        assertThrows(HttpException.class, () -> assetsCache.getAssetByOfferingCode("test"),
            "Response with status code != 200 should result in exception");
    }

    @Test
    void testGetAssetsFromApiThrowsHttpException() throws URISyntaxException {
        AssetDto testAsset = new AssetDto("test", "test", 1, 100.0,
            "test",
            "test", "test");
        ApiResponse<List<AssetDto>> apiResponse = new ApiResponse<>(
            List.of(testAsset),
            HTTP_BAD_REQUEST, null
        );

        when(assetRepository.getAssets()).thenReturn(
            CompletableFuture.completedFuture(apiResponse)
        );

        assertThrows(HttpException.class, () -> assetsCache.getAllAssets(),
            "Response with status code != 200 should result in exception");
    }

    @Test
    void testGetAssetsFromApiReturnsAssets() throws URISyntaxException, HttpException {
        AssetDto testAsset = new AssetDto("test", "test", 1, 100.0,
            "test",
            "test", "test");
        List<AssetDto> listData = List.of(testAsset);

        ApiResponse<List<AssetDto>> apiResponse = new ApiResponse<>(
            listData,
            HTTP_OK, null
        );

        when(assetRepository.getAssets()).thenReturn(
            CompletableFuture.completedFuture(apiResponse)
        );

        assertIterableEquals(listData, assetsCache.getAllAssets(), "Invalid list result");
    }
}