package com.wassim.androidmvpbase.data;

import com.wassim.androidmvpbase.data.local.database.DatabaseHelper;
import com.wassim.androidmvpbase.data.local.preferences.PreferencesHelper;
import com.wassim.androidmvpbase.data.model.Movie;
import com.wassim.androidmvpbase.data.remote.ApiService;
import com.wassim.androidmvpbase.util.RxEventBusHelper;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

public class DataManager {

    private final ApiService mApiService;
    private final DatabaseHelper mDatabaseHelper;
    private final PreferencesHelper mPreferencesHelper;
    private final RxEventBusHelper mEventPoster;


    @Inject
    public DataManager(ApiService apiService, PreferencesHelper preferencesHelper,
                       DatabaseHelper databaseHelper, RxEventBusHelper rxEventBusHelper) {
        mApiService = apiService;
        mPreferencesHelper = preferencesHelper;
        mDatabaseHelper = databaseHelper;
        mEventPoster = rxEventBusHelper;
    }

    public PreferencesHelper getPreferencesHelper() {
        return mPreferencesHelper;
    }

    public ApiService getApiService() {
        return mApiService;
    }

    public DatabaseHelper getDatabaseHelper() {
        return mDatabaseHelper;
    }

    public RxEventBusHelper getEventPoster() {
        return mEventPoster;
    }

    public boolean verifyMovie(int id){
        return getDatabaseHelper().findMovie(id) == null ? false : true;
    }


    public void removeMovie(Movie movie){
         getDatabaseHelper().removeMovie(movie);
    }


    public void addMovie(Movie movie){
        getDatabaseHelper().addMovie(movie);
    }


    public Observable<Movie> getAndSaveMovies() {
        return getApiService().getMovies().concatMap(new Func1<List<Movie>, Observable<Movie>>() {
            @Override
            public Observable<Movie> call(List<Movie> movies) {
                return mDatabaseHelper.saveMovies(movies);
            }
        });
    }

    public Observable<List<Movie>> getMovies(){
        return getApiService().getMovies().distinct();
    }

}
