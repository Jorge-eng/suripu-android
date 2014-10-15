package is.hello.sense.ui.activities;

import android.os.Bundle;

import com.google.common.collect.Lists;

import is.hello.sense.R;
import is.hello.sense.graph.presenters.SensorHistoryPresenter;
import is.hello.sense.ui.widget.LineGraphView;

public class SensorHistoryActivity extends SenseActivity {
    public static final String EXTRA_SENSOR = SensorHistoryActivity.class.getName() + ".EXTRA_SENSOR";
    public static final String EXTRA_MODE = SensorHistoryActivity.class.getName() + ".EXTRA_MODE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_history);
    }


    public String getSensor() {
        return getIntent().getStringExtra(EXTRA_SENSOR);
    }

    public int getMode() {
        return getIntent().getIntExtra(EXTRA_MODE, SensorHistoryPresenter.MODE_WEEK);
    }
}
