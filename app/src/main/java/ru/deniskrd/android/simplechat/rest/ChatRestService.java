package ru.deniskrd.android.simplechat.rest;

import java.util.Map;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Query;
import ru.deniskrd.android.simplechat.model.Group;
import ru.deniskrd.android.simplechat.model.User;

public interface ChatRestService {

    @GET("/users.json")
    Single<Map<String, User>> getUser(@Query("auth") String token, @Query("orderBy") String keyField, @Query("equalTo") String value);

    @GET("groups.json")
    Single<Map<String, Group>> getGroup(@Query("auth") String token, @Query("orderBy") String keyField, @Query("equalTo") String value);
}
