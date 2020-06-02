package com.jon.cotbeacon.updating;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface GithubApi {
    @GET("/repos/jonapoul/cotbeacon/releases")
    Call<List<Release>> getAllReleases();
}
