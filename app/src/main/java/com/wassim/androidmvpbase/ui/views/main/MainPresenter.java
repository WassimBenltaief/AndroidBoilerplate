package com.wassim.androidmvpbase.ui.views.main;

import android.content.Context;
import android.util.Log;

import com.wassim.androidmvpbase.data.DataManager;
import com.wassim.androidmvpbase.data.model.Movie;
import com.wassim.androidmvpbase.data.model.NetworkConnectivity;
import com.wassim.androidmvpbase.data.model.SyncTask;
import com.wassim.androidmvpbase.injection.ActivityContext;
import com.wassim.androidmvpbase.ui.base.BasePresenter;
import com.wassim.androidmvpbase.util.NetworkUtil;

import java.util.List;

import javax.inject.Inject;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

public class MainPresenter extends BasePresenter<MainMvpView> {
    private final String mTAG = "MainPresenter";
    private DataManager mDataManager;
    private CompositeSubscription mCompositeSubscription;

    @Inject
    @ActivityContext
    Context context;

    @Inject
    public MainPresenter(DataManager dataManager) {
        this.mDataManager = dataManager;
        this.mCompositeSubscription = new CompositeSubscription();
        registerEventHandler();
    }

    public void getNetworkStatus(){
        setConnectivityNotification(NetworkUtil.isNetworkConnected(context));
    }

    private void setConnectivityNotification(boolean isConnected) {
        getMvpView().showNetworkStatus(isConnected);
    }

    private void registerEventHandler() {
        ConnectableObservable<Object> syncEvent = mDataManager
                .getEventPoster()
                .toObserverable()
                .publish();

        mCompositeSubscription.add(mDataManager
                .getEventPoster()
                .toObserverable()
                .distinctUntilChanged()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        Log.d("MainPresenter", "registerEventHandler got an event Object");
                        if (o instanceof SyncTask) {
                            if (isViewAttached()) {
                                Log.d("MainPresenter", "registerEventHandler " +
                                      "got an event SyncTask");
                                loadCashedMovies();
                            }
                        }
                        if (o instanceof NetworkConnectivity){
                            Log.d("MainPresenter", "NetworkConnectivity got a change");
                            setConnectivityNotification(((NetworkConnectivity) o).isConnected);
                        }
                    }
                })
        );
    }

    public void loadCashedMovies() {
        getMvpView().showProgress();
        mCompositeSubscription.add(
                mDataManager.getCachedMovies()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Subscriber<List<Movie>>() {
                            @Override
                            public void onCompleted() {
                                getMvpView().hideProgress();
                                Timber.d("MainPresenter.loadCashedMovies().getMovies() completed.");
                            }

                            @Override
                            public void onError(Throwable e) {
                                getMvpView().hideProgress();
                                Timber.e(e, "MainPresenter.loadCashedMovies.getMovies : " +
                                        "There was an error loading the movies");
                                getMvpView().showError();
                            }

                            @Override
                            public void onNext(List<Movie> movies) {
                                Timber.d("MainPresenter.loadCashedMovies.getMovies loaded " +
                                        movies.size());
                                if (movies.isEmpty()) {
                                    getMvpView().showEmpty();
                                } else {
                                    getMvpView().showMovies(movies);
                                }
                            }
                        }));
    }

    @Override
    public void detachView() {
        super.detachView();
        if (mCompositeSubscription != null && mCompositeSubscription.isUnsubscribed())
            mCompositeSubscription.unsubscribe();
    }
}
