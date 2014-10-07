package is.hello.sense.ui.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Interval;

import is.hello.sense.R;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.fragments.TimelineFragment;
import is.hello.sense.ui.widget.FragmentPageView;

public class HomeActivity extends InjectionActivity {
    private FragmentPageView<TimelineFragment> viewPager;
    private TimelineAdapter timelineAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // noinspection unchecked
        this.viewPager = (FragmentPageView<TimelineFragment>) findViewById(R.id.activity_home_view_pager);
        viewPager.setFragmentManager(getSupportFragmentManager());

        this.timelineAdapter = new TimelineAdapter();
        viewPager.setAdapter(timelineAdapter);
        viewPager.setCurrentFragment(TimelineFragment.newInstance(DateTime.now()).setViewPagerTouchListener(viewPager.TOUCH_LISTENER));
    }


    private class TimelineAdapter implements FragmentPageView.Adapter<TimelineFragment> {
        private boolean isToday(@NonNull DateTime dateTime) {
            Interval today = new Interval(dateTime.withTimeAtStartOfDay(), Days.ONE);
            return today.contains(dateTime);
        }


        @Override
        public boolean hasFragmentBeforeFragment(@NonNull TimelineFragment fragment) {
            return true;
        }

        @Override
        public TimelineFragment getFragmentBeforeFragment(@NonNull TimelineFragment fragment) {
            return TimelineFragment.newInstance(fragment.getDateTime().minusDays(1)).setViewPagerTouchListener(viewPager.TOUCH_LISTENER);
        }


        @Override
        public boolean hasFragmentAfterFragment(@NonNull TimelineFragment fragment) {
            return isToday(fragment.getDateTime());
        }

        @Override
        public TimelineFragment getFragmentAfterFragment(@NonNull TimelineFragment fragment) {
            return TimelineFragment.newInstance(fragment.getDateTime().plusDays(1)).setViewPagerTouchListener(viewPager.TOUCH_LISTENER);
        }
    }
}
