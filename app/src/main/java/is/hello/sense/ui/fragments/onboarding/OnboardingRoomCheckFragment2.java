package is.hello.sense.ui.fragments.onboarding;

import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.presenters.RoomConditionsPresenter;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.animation.AnimatorContext;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.widget.SensorConditionView;
import is.hello.sense.ui.widget.SensorTickerView;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Markdown;
import rx.Scheduler;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class OnboardingRoomCheckFragment2 extends InjectionFragment {
    private static final long CONDITION_VISIBLE_MS = 2500;

    @Inject RoomConditionsPresenter presenter;
    @Inject Markdown markdown;

    private ImageView sense;
    private final List<SensorConditionView> sensorViews = new ArrayList<>();
    private TextView status;
    private SensorTickerView ticker;

    private final Scheduler.Worker deferWorker = observeScheduler.createWorker();

    private final List<SensorState> conditions = new ArrayList<>();
    private final List<String> conditionUnits = new ArrayList<>();
    private final @StringRes int[] conditionStrings = {
        R.string.checking_condition_temperature,
        R.string.checking_condition_humidity,
        R.string.checking_condition_sound,
        R.string.checking_condition_light,
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        presenter.update();
        addPresenter(presenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_room_check2, container, false);

        this.sense = (ImageView) view.findViewById(R.id.fragment_onboarding_room_check_sense);
        this.status = (TextView) view.findViewById(R.id.fragment_onboarding_room_check_status);

        AnimatorContext animatorContext = getAnimatorContext();
        this.ticker = (SensorTickerView) view.findViewById(R.id.fragment_onboarding_room_check_ticker);
        ticker.setAnimatorContext(animatorContext);

        ViewGroup sensors = (ViewGroup) view.findViewById(R.id.fragment_onboarding_room_check_sensors);
        for (int i = 0, count = sensors.getChildCount(); i < count; i++) {
            View sensorChild = sensors.getChildAt(i);
            if (sensorChild instanceof SensorConditionView) {
                SensorConditionView conditionView = (SensorConditionView) sensorChild;
                conditionView.setAnimatorContext(animatorContext);
                sensorViews.add(conditionView);
            }
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(presenter.currentConditions,
                         this::bindConditions,
                         this::conditionsUnavailable);
    }


    //region Animations

    private void animateConditionAt(int position) {
        if (position >= conditions.size()) {
            jumpToEnd();
            return;
        }

        animateSenseToGrey();

        SensorConditionView conditionView = sensorViews.get(position);
        status.setTextAppearance(getActivity(), R.style.AppTheme_Text_SectionHeading);
        status.setText(conditionStrings[position]);
        conditionView.crossFadeToFill(R.drawable.room_check_sensor_border_loading, true, () -> {
            SensorState sensor = conditions.get(position);

            Resources resources = getResources();
            int startColor = resources.getColor(Condition.ALERT.colorRes);
            int endColor = resources.getColor(sensor.getCondition().colorRes);

            int value = sensor.getValue() != null ? sensor.getValue().intValue() : 0;
            String unit = conditionUnits.get(position);
            long duration = ticker.animateToValue(value, unit, ignored -> {
                animate(status, getAnimatorContext())
                        .fadeOut(View.VISIBLE)
                        .addOnAnimationCompleted(finished -> {
                            status.setTextAppearance(getActivity(), R.style.AppTheme_Text_Body);
                            status.setText(null);
                            status.setTransformationMethod(null);
                            markdown.renderInto(status, sensor.getMessage());

                            animateSenseCondition(sensor.getCondition());
                            conditionView.crossFadeToFill(R.drawable.room_check_sensor_border_filled, false, () -> {
                                deferWorker.schedule(() -> animateConditionAt(position + 1), CONDITION_VISIBLE_MS, TimeUnit.MILLISECONDS);
                            });
                        })
                        .andThen()
                        .fadeIn()
                        .start();
            });


            ValueAnimator scoreAnimator = Animation.createColorAnimator(startColor, endColor);
            scoreAnimator.addUpdateListener(a -> {
                int color = (int) a.getAnimatedValue();
                conditionView.setTint(color);
                ticker.setTextColor(color);
            });
            scoreAnimator.setDuration(duration);
            scoreAnimator.start();
        });
    }

    private void jumpToEnd() {
        int totalConditionValue = Lists.sumInt(conditions, c -> c.getCondition().ordinal());
        int roundedAverage = Math.round((totalConditionValue / conditions.size()) + 0.5f);
        Condition condition = Condition.values()[roundedAverage];
        animateSenseCondition(condition);
    }

    private void animateSenseToGrey() {
        Drawable senseDrawable = sense.getDrawable();
        if (senseDrawable instanceof TransitionDrawable) {
            ((TransitionDrawable) senseDrawable).reverseTransition(Animation.DURATION_NORMAL);
        }
    }

    private void animateSenseCondition(@NonNull Condition condition) {
        int drawableRes = 0;
        switch (condition) {
            case UNKNOWN:
                drawableRes = R.drawable.room_check_sense_gray;
                break;
            case ALERT:
                drawableRes = R.drawable.room_check_sense_red;
                break;
            case WARNING:
                drawableRes = R.drawable.room_check_sense_yellow;
                break;
            case IDEAL:
                drawableRes = R.drawable.room_check_sense_green;
                break;
        }

        Resources resources = getResources();
        TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[] {
            resources.getDrawable(R.drawable.room_check_sense_gray),
            resources.getDrawable(drawableRes),
        });
        transitionDrawable.setCrossFadeEnabled(true);
        sense.setImageDrawable(transitionDrawable);
        transitionDrawable.startTransition(Animation.DURATION_NORMAL);
    }

    //endregion


    //region Binding

    public void bindConditions(@NonNull RoomConditionsPresenter.Result current) {
        conditions.clear();

        conditions.add(current.conditions.getTemperature());
        conditionUnits.add(current.units.getTemperatureUnit());
        conditions.add(current.conditions.getHumidity());
        conditionUnits.add(current.units.getHumidityUnit());
        conditions.add(current.conditions.getSound());
        conditionUnits.add(current.units.getSoundUnit());
        conditions.add(current.conditions.getLight());
        conditionUnits.add(current.units.getLightUnit());

        animateConditionAt(0);
    }

    public void conditionsUnavailable(Throwable e) {
        Analytics.trackError(e, "Room check");
        Logger.error(getClass().getSimpleName(), "Could not load conditions for room check", e);

        conditions.clear();

        jumpToEnd();
    }

    //endregion
}
