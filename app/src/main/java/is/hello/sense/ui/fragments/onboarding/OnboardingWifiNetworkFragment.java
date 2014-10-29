package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.hello.ble.protobuf.MorpheusBle;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.adapter.WifiNetworkAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.util.Analytics;

public class OnboardingWifiNetworkFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    @Inject HardwarePresenter hardwarePresenter;

    private WifiNetworkAdapter networkAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPresenter(hardwarePresenter);

        Analytics.event(Analytics.EVENT_ONBOARDING_SETUP_WIFI, null);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_wifi_networks, container, false);

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        View wifiView = inflater.inflate(R.layout.item_wifi_network, listView, false);
        wifiView.findViewById(R.id.item_wifi_network_locked).setVisibility(View.GONE);
        wifiView.findViewById(R.id.item_wifi_network_scanned).setVisibility(View.GONE);
        ((TextView) wifiView.findViewById(R.id.item_wifi_network_name)).setText(R.string.wifi_other_network);
        listView.addFooterView(wifiView, null, true);

        this.networkAdapter = new WifiNetworkAdapter(getActivity());
        listView.setAdapter(networkAdapter);

        this.swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.fragment_onboarding_wifi_networks_refresh_container);
        swipeRefreshLayout.setColorSchemeResources(R.color.sleep_light, R.color.sleep_intermediate, R.color.sleep_deep, R.color.sleep_awake);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Analytics.event(Analytics.EVENT_ONBOARDING_WIFI_SCAN, null);
            rescan(false);
        });

        Button helpButton = (Button) view.findViewById(R.id.fragment_onboarding_step_help);
        helpButton.setOnClickListener(v -> Toast.makeText(v.getContext().getApplicationContext(), "Hang in there...", Toast.LENGTH_SHORT).show());

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rescan(true);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        MorpheusBle.wifi_endpoint network = (MorpheusBle.wifi_endpoint) adapterView.getItemAtPosition(position);
        getOnboardingActivity().showSignIntoWifiNetwork(network);
    }


    public void rescan(boolean withSecondScan) {
        swipeRefreshLayout.setRefreshing(true);
        bindAndSubscribe(hardwarePresenter.scanForWifiNetworks(), firstResults -> {
            bindScanResults(firstResults);
            if (withSecondScan) {
                bindAndSubscribe(hardwarePresenter.scanForWifiNetworks(), secondResults -> {
                    Map<String, MorpheusBle.wifi_endpoint> combinedResults = new HashMap<>();
                    for (MorpheusBle.wifi_endpoint result : firstResults)
                        combinedResults.put(result.getSsid(), result);
                    for (MorpheusBle.wifi_endpoint result : secondResults)
                        combinedResults.put(result.getSsid(), result);
                    bindScanResults(combinedResults.values());

                    swipeRefreshLayout.setRefreshing(false);
                }, this::scanResultsUnavailable);
            } else {
                swipeRefreshLayout.setRefreshing(false);
            }
        }, this::scanResultsUnavailable);
    }

    public void bindScanResults(@NonNull Collection<MorpheusBle.wifi_endpoint> scanResults) {
        networkAdapter.clear();
        networkAdapter.addAll(scanResults);
    }

    public void scanResultsUnavailable(Throwable e) {
        swipeRefreshLayout.setRefreshing(false);
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }


    private OnboardingActivity getOnboardingActivity() {
        return (OnboardingActivity) getActivity();
    }
}
