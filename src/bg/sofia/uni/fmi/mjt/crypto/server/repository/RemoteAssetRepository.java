package bg.sofia.uni.fmi.mjt.crypto.server.repository;

import bg.sofia.uni.fmi.mjt.crypto.server.dto.AssetDto;
import bg.sofia.uni.fmi.mjt.crypto.server.dto.ErrorDto;
import bg.sofia.uni.fmi.mjt.crypto.server.util.ApiResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.net.HttpURLConnection.HTTP_OK;

public class RemoteAssetRepository implements AssetRepository {
    private static final String ASSETS_ENDPOINT = "https://rest.coinapi.io/v1/assets";
    private static final String API_KEY_NAME = "X-CoinAPI-Key";

    private final Gson gson = new GsonBuilder()
        .create();

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public CompletableFuture<ApiResponse<List<AssetDto>>> getAssets() throws URISyntaxException {
        HttpRequest request = HttpRequest.newBuilder(new URI(ASSETS_ENDPOINT))
            .GET()
            .header(API_KEY_NAME, System.getenv(API_KEY_NAME))
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(this::mapResponseToStatusCodeAssets);
    }

    @Override
    public CompletableFuture<ApiResponse<AssetDto>> getAssetById(String assetId) throws URISyntaxException {
        String url = String.format("%s/%s", ASSETS_ENDPOINT, assetId);

        HttpRequest request = HttpRequest.newBuilder(new URI(url))
            .GET()
            .header(API_KEY_NAME, System.getenv(API_KEY_NAME))
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(this::mapResponseToStatusCodeAsset);
    }

    private ApiResponse<AssetDto> mapResponseToStatusCodeAsset(HttpResponse<String> response) {
        if (response.statusCode() != HTTP_OK) {
            String message = gson.fromJson(response.body(), ErrorDto.class).getMessage();
            return new ApiResponse<>(null, response.statusCode(), message);
        }

        Type assetsListType = new TypeToken<ArrayList<AssetDto>>() {
        }.getType();
        List<AssetDto> list = gson.fromJson(response.body(), assetsListType);
        AssetDto assetDto = getFilteredList(list).stream().findFirst().orElse(null);
        return new ApiResponse<>(assetDto, response.statusCode(), null);
    }

    private ApiResponse<List<AssetDto>> mapResponseToStatusCodeAssets(HttpResponse<String> response) {
        if (response.statusCode() != HTTP_OK) {
            String message = gson.fromJson(response.body(), ErrorDto.class).getMessage();
            return new ApiResponse<>(null, response.statusCode(), message);
        }

        Type assetsListType = new TypeToken<ArrayList<AssetDto>>() {
        }.getType();
        List<AssetDto> list = gson.fromJson(response.body(), assetsListType);
        return new ApiResponse<>(getFilteredList(list),
            response.statusCode(), null);
    }

    private List<AssetDto> getFilteredList(List<AssetDto> assetDtos) {
        return assetDtos.stream()
            .filter(elem -> elem.typeIsCrypto() == 1 && elem.priceUsd() > 0)
            .toList();
    }
}
