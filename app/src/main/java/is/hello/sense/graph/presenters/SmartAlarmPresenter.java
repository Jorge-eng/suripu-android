package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SmartAlarm;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.graph.annotations.CacheDirectoryFile;
import is.hello.sense.util.CachedObject;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.Subscription;

public class SmartAlarmPresenter extends Presenter {
    private static final String CACHE_FILENAME = "Suripu-Smart-Alarms-test.json";

    private final ApiService apiService;
    private final @Nullable CachedObject<List<SmartAlarm>> alarmCache;

    public final PresenterSubject<List<SmartAlarm>> alarms = PresenterSubject.create();

    @Inject SmartAlarmPresenter(@NonNull ApiService apiService,
                                @CacheDirectoryFile @Nullable File cacheDirectory,
                                @NonNull ObjectMapper objectMapper) {
        this.apiService = apiService;

        if (cacheDirectory != null) {
            this.alarmCache = new CachedObject<>(CachedObject.getFile(cacheDirectory, CACHE_FILENAME),
                                                 new TypeReference<List<SmartAlarm>>() {},
                                                 objectMapper);
        } else {
            this.alarmCache = null;
        }
    }

    @Override
    protected void onReloadForgottenData() {
        update();
    }

    @Override
    protected boolean onForgetDataForLowMemory() {
        alarms.forget();
        return true;
    }

    public @Nullable Observable<List<SmartAlarm>> retrieveCache() {
        if (alarmCache != null) {
            return alarmCache.get();
        } else {
            return null;
        }
    }

    public @Nullable Observable<List<SmartAlarm>> saveCache(@Nullable List<SmartAlarm> alarms) {
        if (alarmCache != null) {
            return alarmCache.set(alarms);
        } else {
            return null;
        }
    }


    public void update() {
        logEvent("update()");

        Observable<List<SmartAlarm>> cache = retrieveCache();
        Subscription cacheSubscription = cache != null ? cache.subscribe(alarms) : null;
        apiService.smartAlarms().subscribe(alarms -> {
            if (cacheSubscription != null && !cacheSubscription.isUnsubscribed())
                cacheSubscription.unsubscribe();

            this.alarms.onNext(alarms);
            if (alarmCache != null) {
                alarmCache.set(alarms)
                          .subscribe(ignored -> logEvent("cache updated"), Functions.LOG_ERROR);
            }
        }, e -> {
            Logger.error(SmartAlarmPresenter.class.getSimpleName(), "Could not refresh smart alarms.", e);
            if (cacheSubscription != null && cacheSubscription.isUnsubscribed()) {
                alarms.onError(e);
            }
        });
    }

    public Observable<VoidResponse> save(@NonNull List<SmartAlarm> updatedAlarms) {
        logEvent("save()");

        return apiService.saveSmartAlarms(System.currentTimeMillis(), updatedAlarms)
                         .doOnNext(ignored -> {
                             logEvent("smart alarms saved");

                             saveCache(updatedAlarms);
                             this.alarms.onNext(updatedAlarms);
                         })
                         .doOnError(this.alarms::onError);
    }
}
