package is.hello.sense.ui.widget.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.api.model.TimelineSegment;

public final class Styles {
    public static final int TIMELINE_HOURS_ON_SCREEN = 10;


    public static @NonNull Typeface getTypeface(@NonNull Context context, @NonNull String typeface) {
        return Typeface.createFromAsset(context.getAssets(), typeface);
    }


    public static @ColorRes @DrawableRes int getSleepDepthColorRes(int sleepDepth) {
        if (sleepDepth == 0)
            return R.color.sleep_awake;
        else if (sleepDepth == 100)
            return R.color.sleep_deep;
        else if (sleepDepth < 60)
            return R.color.sleep_light;
        else
            return R.color.sleep_intermediate;
    }

    public static @ColorRes @DrawableRes int getSleepDepthDimmedColorRes(int sleepDepth) {
        if (sleepDepth == 0)
            return R.color.sleep_awake_dimmed;
        else if (sleepDepth == 100)
            return R.color.sleep_deep_dimmed;
        else if (sleepDepth < 60)
            return R.color.sleep_light_dimmed;
        else
            return R.color.sleep_intermediate_dimmed;
    }

    public static @ColorRes @DrawableRes int getSleepScoreColorRes(int sleepScore) {
        if (sleepScore < 45)
            return R.color.sensor_warning;
        else if (sleepScore < 80)
            return R.color.sensor_alert;
        else
            return R.color.sensor_ideal;
    }

    public static int getSleepScoreColor(@NonNull Context context, int sleepScore) {
        return context.getResources().getColor(getSleepScoreColorRes(sleepScore));
    }


    public static @DrawableRes int getTimelineSegmentIconRes(@NonNull TimelineSegment segment) {
        if (segment.getEventType() == null) {
            return 0;
        }

        switch (segment.getEventType()) {
            case MOTION: {
                return R.drawable.timeline_movement;
            }

            case NOISE: {
                return R.drawable.timeline_sound;
            }

            case SNORING: {
                return R.drawable.timeline_sound;
            }

            case SLEEP_TALK: {
                return R.drawable.timeline_sound;
            }

            case LIGHT: {
                return R.drawable.timeline_light;
            }

            case LIGHTS_OUT: {
                return R.drawable.timeline_lights_out;
            }

            case SLEEP_MOTION: {
                return R.drawable.timeline_movement;
            }

            case IN_BED: {
                return R.drawable.timeline_in_bed;
            }

            case SLEEP: {
                return R.drawable.timeline_asleep;
            }

            case SUNSET: {
                return R.drawable.timeline_sunset;
            }

            case SUNRISE: {
                return R.drawable.timeline_sunrise;
            }

            case PARTNER_MOTION: {
                return R.drawable.timeline_partner;
            }

            case OUT_OF_BED: {
                return R.drawable.timeline_out_of_bed;
            }

            case WAKE_UP: {
                return R.drawable.timeline_wakeup;
            }

            default:
            case UNKNOWN: {
                return R.drawable.timeline_movement;
            }
        }
    }


    public static @NonNull Drawable createGraphFillDrawable(@NonNull Resources resources) {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {
                resources.getColor(R.color.graph_fill_gradient_top),
                resources.getColor(R.color.graph_fill_gradient_bottom),
        });
    }

    public static @NonNull TextView createItemView(@NonNull Context context,
                                                   @StringRes int titleRes,
                                                   @StyleRes int textAppearanceRes,
                                                   @NonNull View.OnClickListener onClick) {
        TextView itemView = new TextView(context);
        itemView.setBackgroundResource(R.drawable.selectable_dark_bounded);
        itemView.setTextAppearance(context, textAppearanceRes);
        itemView.setText(titleRes);

        Resources resources = context.getResources();
        int itemTextHorizontalPadding = resources.getDimensionPixelSize(R.dimen.gap_outer);
        int itemTextVerticalPadding = resources.getDimensionPixelSize(R.dimen.gap_medium);
        itemView.setPadding(itemTextHorizontalPadding, itemTextVerticalPadding, itemTextHorizontalPadding, itemTextVerticalPadding);

        Views.setSafeOnClickListener(itemView, onClick);

        return itemView;
    }

    public static void applyRefreshLayoutStyle(@NonNull SwipeRefreshLayout refreshLayout) {
        refreshLayout.setColorSchemeResources(R.color.sensor_alert, R.color.sensor_warning, R.color.sensor_ideal);
    }

    public static void applyGraphLineParameters(@NonNull Paint paint) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    public static void addCardSpacingHeaderAndFooter(@NonNull ListView listView) {
        Context context = listView.getContext();
        Resources resources = listView.getResources();

        int spacingHeight = resources.getDimensionPixelSize(R.dimen.gap_outer);
        View topSpacing = new View(context);
        topSpacing.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, spacingHeight));
        listView.addHeaderView(topSpacing, null, false);

        View bottomSpacing = new View(context);
        bottomSpacing.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, spacingHeight));
        listView.addFooterView(bottomSpacing, null, false);
    }


    //region Dividers

    public static View createHorizontalDivider(@NonNull Context context, int width) {
        View view = new View(context);
        view.setBackgroundResource(R.color.border);
        view.setLayoutParams(new ViewGroup.LayoutParams(width, context.getResources().getDimensionPixelSize(R.dimen.divider_size)));
        return view;
    }

    public static View createVerticalDivider(@NonNull Context context, int height) {
        View view = new View(context);
        view.setBackgroundResource(R.color.border);
        view.setLayoutParams(new ViewGroup.LayoutParams(context.getResources().getDimensionPixelSize(R.dimen.divider_size), height));
        return view;
    }

    //endregion
}
