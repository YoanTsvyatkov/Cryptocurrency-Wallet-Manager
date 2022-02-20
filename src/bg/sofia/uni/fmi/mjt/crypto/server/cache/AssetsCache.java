package bg.sofia.uni.fmi.mjt.crypto.server.cache;

import bg.sofia.uni.fmi.mjt.crypto.server.dto.AssetDto;
import bg.sofia.uni.fmi.mjt.crypto.server.exception.BadRequestException;
import bg.sofia.uni.fmi.mjt.crypto.server.exception.ForbiddenException;
import bg.sofia.uni.fmi.mjt.crypto.server.exception.HttpException;
import bg.sofia.uni.fmi.mjt.crypto.server.exception.TooManyRequestsException;
import bg.sofia.uni.fmi.mjt.crypto.server.exception.UnauthorizedException;
import bg.sofia.uni.fmi.mjt.crypto.server.repository.AssetRepository;
import bg.sofia.uni.fmi.mjt.crypto.server.util.ApiResponse;

import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

public class AssetsCache {
    private static final int MINUTES_BEFORE_UPDATE = 30;
    private static final int CACHE_CAPACITY = 50;
    private static final int HTTP_TOO_MANY_REQUESTS_CODE = 429;

    private LocalDateTime timeOfLastUpdate;
    private AssetRepository assetRepository;
    private Map<String, AssetDto> cachedData;

    public AssetsCache(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
        this.cachedData = new HashMap<>(CACHE_CAPACITY);
    }

    public AssetDto getAssetByOfferingCode(String offeringCode) throws HttpException, URISyntaxException {
        if (offeringCode == null || offeringCode.isEmpty()) {
            throw new IllegalArgumentException("Cannot pass empty or null id");
        }

        if (isCachedDataOld()) {
            updateCache();
        }

        if (!cachedData.containsKey(offeringCode)) {
            AssetDto assetDto = getAssetFromApiByOfferingCode(offeringCode);
            putAssetInCache(assetDto);
        }
        return cachedData.get(offeringCode);
    }

    public List<AssetDto> getAllAssets() throws HttpException, URISyntaxException {
        if (cachedData.size() != CACHE_CAPACITY) {
            cachedData.clear();
            timeOfLastUpdate = LocalDateTime.now();

            List<AssetDto> assetsFromApi = getAssetsFromApi();
            for (AssetDto assetDto : assetsFromApi) {
                cachedData.put(assetDto.assetId(), assetDto);
            }
        }

        if (isCachedDataOld()) {
            updateCache();
        }

        return cachedData.values().stream().toList();
    }

    private void putAssetInCache(AssetDto assetDto) {
        Collection<AssetDto> list = cachedData.values();
        if (list.size() == CACHE_CAPACITY) {
            AssetDto asset = list.stream().findFirst().orElse(null);
            cachedData.remove(asset.assetId());
        }

        cachedData.put(assetDto.assetId(), assetDto);
    }

    private List<AssetDto> getAssetsFromApi()
        throws URISyntaxException, HttpException {
        ApiResponse<List<AssetDto>> response = assetRepository.getAssets().join();
        if (response.getStatusCode() != HTTP_OK) {
            throwException(response.getStatusCode(), response.getMessage());
        }

        return response.getData()
            .stream()
            .limit(CACHE_CAPACITY)
            .toList();
    }

    private AssetDto getAssetFromApiByOfferingCode(String offeringCode)
        throws URISyntaxException, HttpException {
        ApiResponse<AssetDto> assetResponse = assetRepository.getAssetById(offeringCode).join();
        if (assetResponse.getData() == null) {
            throw new IllegalArgumentException("Crypto with this code does not exist");
        }

        return getAssetDataFromResponse(assetResponse);
    }

    private void throwException(int statusCode, String message)
        throws HttpException {
        switch (statusCode) {
            case HTTP_BAD_REQUEST -> throw new BadRequestException(message);
            case HTTP_UNAUTHORIZED -> throw new UnauthorizedException(message);
            case HTTP_FORBIDDEN -> throw new ForbiddenException(message);
            case HTTP_TOO_MANY_REQUESTS_CODE -> throw new TooManyRequestsException(message);
        }
    }

    private boolean isCachedDataOld() {
        if (timeOfLastUpdate == null) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        long minutesDiff = timeOfLastUpdate.until(now, ChronoUnit.MINUTES);
        return minutesDiff >= MINUTES_BEFORE_UPDATE;
    }

    private void updateCache() throws URISyntaxException, HttpException {
        timeOfLastUpdate = LocalDateTime.now();

        List<CompletableFuture<ApiResponse<AssetDto>>> list = new ArrayList<>();

        for (Map.Entry<String, AssetDto> assetDtoEntry : cachedData.entrySet()) {
            list.add(assetRepository.getAssetById(assetDtoEntry.getKey()));
        }

        cachedData.clear();
        List<ApiResponse<AssetDto>> apiResponses = list.stream().map(CompletableFuture::join).toList();

        for (ApiResponse<AssetDto> apiResponse : apiResponses) {
            AssetDto assetDto = getAssetDataFromResponse(apiResponse);
            cachedData.put(assetDto.assetId(), assetDto);
        }
    }

    private AssetDto getAssetDataFromResponse(ApiResponse<AssetDto> assetResponse) throws HttpException {
        if (assetResponse.getStatusCode() != HTTP_OK) {
            throwException(assetResponse.getStatusCode(), assetResponse.getMessage());
        }

        return assetResponse.getData();
    }
}
