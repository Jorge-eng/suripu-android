package is.hello.sense.api;

import android.support.annotation.NonNull;

import java.util.List;

import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.RoomConditions;
import is.hello.sense.api.model.SensorHistory;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.api.sessions.OAuthCredentials;
import is.hello.sense.api.sessions.OAuthSession;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;
import rx.Observable;

public interface ApiService {
    // TODO: Make these dynamic
    public static final String CLIENT_ID = "android_dev";
    public static final String CLIENT_SECRET = "99999secret";
    public static final String BASE_URL = "https://dev-api.hello.is/v1";

    //region OAuth

    @POST("/oauth2/token")
    Observable<OAuthSession> authorize(@Body OAuthCredentials request);

    //endregion


    //region Account

    @GET("/account")
    Observable<Account> getAccount();

    @POST("/account")
    Observable<Account> createAccount(@Body Account account);

    @PUT("/account")
    Observable<Account> updateAccount(@Body Account account);

    //endregion


    //region Timeline

    @GET("/timeline/{month}-{day}-{year}")
    Observable<List<Timeline>> timelineForDate(@NonNull @Path("month") String month,
                                               @NonNull @Path("day") String day,
                                               @NonNull @Path("year") String year);

    //endregion


    //region Room Conditions

    @GET("/room/current")
    Observable<RoomConditions> currentRoomConditions();

    @GET("/room/{sensor}/day")
    Observable<List<SensorHistory>> sensorHistoryForDay(@Path("sensor") String sensor,
                                                        @Query("timestamp_millis") long timestamp);

    @GET("/room/{sensor}/week")
    Observable<List<SensorHistory>> sensorHistoryForWeek(@Path("sensor") String sensor,
                                                         @Query("timestamp_millis") long timestamp);

    //endregion
}
