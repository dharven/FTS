package com.dharven.fts.repository;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatGPTClient {
    private static final String BASE_URL = "https://api.openai.com/";
    private ChatGPTApi chatGPTApi;

    public ChatGPTClient() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        chatGPTApi = retrofit.create(ChatGPTApi.class);
    }

    public void getChatResponse(ApiRequest request, final ChatResponseCallback callback) {
        Call<ChatResponse> call = chatGPTApi.getChatResponse(request);
        call.enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                if (response.isSuccessful()) {
                    ChatResponse chatResponse = response.body();
                    if (chatResponse != null && !chatResponse.getChoices().isEmpty()) {
                        callback.onSuccess(chatResponse.getChoices().get(0).getMessage().getContent());
                    } else {
                        callback.onError("Empty response");
                    }
                } else {
                    callback.onError("Request failed with status code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ChatResponse> call, Throwable t) {
                callback.onError("Request failed: " + t.getMessage());
            }
        });
    }

    public interface ChatResponseCallback {
        void onSuccess(String reply);
        void onError(String errorMessage);
    }
}

