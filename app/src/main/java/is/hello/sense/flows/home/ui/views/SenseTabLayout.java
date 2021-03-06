package is.hello.sense.flows.home.ui.views;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

import is.hello.sense.flows.home.util.HomeFragmentPagerAdapter;

public class SenseTabLayout<T extends SenseTabLayout.Listener> extends TabLayout
        implements TabLayout.OnTabSelectedListener {

    @Nullable
    protected T listener = null;

    public SenseTabLayout(final Context context) {
        this(context, null, 0);
    }

    public SenseTabLayout(final Context context,
                          final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SenseTabLayout(final Context context,
                          final AttributeSet attrs,
                          final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setupWithViewPager(@Nullable final ViewPager viewPager,
                                   final boolean autoRefresh) {
        super.setupWithViewPager(viewPager, autoRefresh);
        setUpInternal(viewPager);
    }

    /**
     * Called during {@link SenseTabLayout#setUpInternal(ViewPager)} for each tab to be updated
     * @param position of tab
     * @param adapter used to populate tabs
     * @param item at position provided by adapter
     * @return {@link View} which will be used as custom view for tab at position
     */
    protected View createTabCustomView(final int position,
                                       @NonNull final HomeFragmentPagerAdapter adapter,
                                       @NonNull final HomeFragmentPagerAdapter.HomeItem item) {
        return createTabFor(item.normalIconRes, item.activeIconRes);
    }

    private void setUpInternal(@Nullable final ViewPager viewPager) {
        clearOnTabSelectedListeners();

        if (viewPager !=null && viewPager.getAdapter() instanceof HomeFragmentPagerAdapter) {
            final HomeFragmentPagerAdapter adapter = (HomeFragmentPagerAdapter) viewPager.getAdapter();
            final HomeFragmentPagerAdapter.HomeItem[] items = adapter.getHomeItems();
            final int tabCount = getTabCount();
            if (items.length != tabCount) {
                throw new AssertionError(String.format("Tab count mismatch expected %s actual %s", items.length, tabCount));
            }
            for (int position = 0; position < tabCount; position++) {
                final HomeFragmentPagerAdapter.HomeItem item = items[position];
                Tab tab = getTabAt(position);
                if (tab == null) {
                    tab = newTab();
                    addTab(tab, position);
                }
                tab.setCustomView(createTabCustomView(position, adapter, item));
            }
        }

        addOnTabSelectedListener(this);

        final TabLayout.Tab tab = getTabAt(getSelectedTabPosition());
        if (tab != null) {
            setTabActive(tab, true);
            tab.select();
        }
    }

    //region TabSelectedListener
    @Override
    public void onTabSelected(final Tab tab) {
        if (tab == null) {
            return;
        }
        tabChanged(tab.getPosition());
        setTabActive(tab, true);
    }

    @Override
    public void onTabUnselected(final Tab tab) {
        setTabActive(tab, false);
    }

    @Override
    public void onTabReselected(final Tab tab) {
        scrollUp(tab.getPosition());

    }
    //endregion

    private void scrollUp(final int position) {
        if (this.listener != null) {
            this.listener.scrollUp(position);
        }
    }

    private void tabChanged(final int fragmentPosition) {
        if (this.listener != null) {
            this.listener.tabChanged(fragmentPosition);
        }
    }

    private SenseTabView createTabFor(@DrawableRes final int normal,
                                      @DrawableRes final int active) {
        return new SenseTabView(getContext())
                .setDrawables(normal, active)
                .setActive(false);
    }

    public final void setListener(@Nullable final T listener) {
        this.listener = listener;
    }

    private void setTabActive(@Nullable final Tab tab,
                              final boolean active) {
        if (tab == null) {
            return;
        }
        final View view = tab.getCustomView();
        if (view instanceof SenseTabView) {
            ((SenseTabView) view).setActive(active);
        }
    }

    public interface Listener {
        void scrollUp(int fragmentPosition);

        void tabChanged(int fragmentPosition);
    }
}
