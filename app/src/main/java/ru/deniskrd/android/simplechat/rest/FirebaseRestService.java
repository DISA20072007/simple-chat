package ru.deniskrd.android.simplechat.rest;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import ru.deniskrd.android.simplechat.constants.AppConstants;
import ru.deniskrd.android.simplechat.model.Group;
import ru.deniskrd.android.simplechat.model.User;

public class FirebaseRestService {

    private final static String BASE_URL = "https://simple-chat-2cb80.firebaseio.com";
    private static FirebaseRestService instance;

    private static Retrofit retrofit;

    private FirebaseRestService() {
        retrofit = new Retrofit.Builder().baseUrl(BASE_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static FirebaseRestService getInstance() {
        if (instance == null) {
            instance = new FirebaseRestService();
        }
        return instance;
    }

    public Single<User> getChatUser(String token, String userName) {
        return  retrofit.create(ChatRestService.class)
                        .getUser(token, "\"userName\"", "\"" + userName + "\"")
                .map(response -> response.values().iterator().next())
                .subscribeOn(Schedulers.from(ExecutorProvider.defaultHttpExecutor()))
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<Group> getChatGroup(String token, String groupId) {
        String keyField = groupId.contains(AppConstants.PRIVATE_GROUP_SUFFIX) ? "\"name\"" : "\"$key\"";

        return retrofit.create(ChatRestService.class)
                .getGroup(token, keyField, "\"" + groupId + "\"")
                .map(response -> response.values().iterator().next())
                .subscribeOn(Schedulers.from(ExecutorProvider.defaultHttpExecutor()))
                .observeOn(AndroidSchedulers.mainThread());
    }
}
