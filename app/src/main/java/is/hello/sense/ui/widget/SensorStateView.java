package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.units.UnitFormatter;

public class SensorStateView extends FrameLayout {
    private TextView titleText;
    private TextView readingText;

    @SuppressWarnings("UnusedDeclaration")
    public SensorStateView(Context context) {
        super(context);
        initialize(null, 0);
    }

    @SuppressWarnings("UnusedDeclaration")
    public SensorStateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(attrs, 0);
    }

    @SuppressWarnings("UnusedDeclaration")
    public SensorStateView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize(attrs, defStyle);
    }

    protected void initialize(@Nullable AttributeSet attrs, int defStyle) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.view_sensor_state, this, true);

        this.titleText = (TextView) findViewById(R.id.view_sensor_state_title);
        this.readingText = (TextView) findViewById(R.id.view_sensor_state_reading);

        if (attrs != null) {
            TypedArray styles = getContext().obtainStyledAttributes(attrs, R.styleable.SensorStateView, defStyle, 0);
            String title = styles.getString(R.styleable.SensorStateView_sensorTitle);
            Drawable icon = styles.getDrawable(R.styleable.SensorStateView_sensorIcon);
            boolean wantsReading = styles.getBoolean(R.styleable.SensorStateView_wantsReading, true);

            setTitle(title);
            setIconDrawable(icon);
            setWantsReading(wantsReading);
        }
    }


    //region Properties

    public void setIconDrawable(Drawable drawable) {
        titleText.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null);
    }

    public Drawable getIconDrawable() {
        return titleText.getCompoundDrawablesRelative()[0];
    }

    public void setTitle(CharSequence title) {
        titleText.setText(title);
    }

    public CharSequence getTitle() {
        return titleText.getText();
    }

    public void setWantsReading(boolean wantsReading) {
        if (wantsReading)
            readingText.setVisibility(VISIBLE);
        else
            readingText.setVisibility(GONE);
    }

    public boolean getWantsReading() {
        return readingText.getVisibility() == VISIBLE;
    }

    public void setReading(CharSequence reading) {
        readingText.setText(reading);
    }

    public CharSequence getReading() {
        return readingText.getText();
    }

    public void displayCondition(Condition readingCondition) {
        readingText.setTextColor(getResources().getColor(readingCondition.colorRes));
    }

    public void displayReading(@Nullable SensorState reading, @Nullable UnitFormatter.Formatter formatter) {
        if (reading == null) {
            readingText.setText(R.string.missing_data_placeholder);
            displayCondition(Condition.UNKNOWN);
        } else {
            String formattedValue = reading.getFormattedValue(formatter);
            if (formattedValue != null)
                readingText.setText(formattedValue);
            else
                readingText.setText(R.string.missing_data_placeholder);

            displayCondition(reading.getCondition());
        }
    }

    //endregion
}
