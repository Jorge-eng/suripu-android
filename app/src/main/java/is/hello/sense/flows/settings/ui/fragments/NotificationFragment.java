package is.hello.sense.flows.settings.ui.fragments;


import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import is.hello.sense.R;
import is.hello.sense.flows.settings.ui.views.NotificationView;
import is.hello.sense.mvp.presenters.PresenterFragment;

public class NotificationFragment extends PresenterFragment<NotificationView> {
    @Override
    public void initializePresenterView() {
        presenterView = new NotificationView(getActivity());
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu,
                                    final MenuInflater inflater) {
        inflater.inflate(R.menu.save, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.save) {
            Toast.makeText(getActivity(), "Pressed Save", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
