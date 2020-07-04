package network.oversee.buttin.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import network.oversee.buttin.MainActivity;
import network.oversee.buttin.R;
import network.oversee.buttin.profile.ProfileActivity;
import network.oversee.buttin.utils.AppConstants;
import network.oversee.buttin.utils.SharedObjects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SettingsFragment extends Fragment {

    SharedObjects sharedObjects;

    @BindView(R.id.llProfile) LinearLayout llProfile;
    @BindView(R.id.llRateUs) LinearLayout llRateUs;
    @BindView(R.id.llLogout) LinearLayout llLogout;

    public SettingsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        ButterKnife.bind(this, view);
        sharedObjects = new SharedObjects(getActivity());
        return view;
    }

    @OnClick({R.id.llProfile,R.id.llRateUs, R.id.llLogout})
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.llProfile:
                startActivity(new Intent(getActivity(), ProfileActivity.class));
                break;
            case R.id.llLogout:
                ((MainActivity) getActivity()).removeAllPreferenceOnLogout();
                break;
            case R.id.llRateUs:
                if (SharedObjects.isNetworkConnected(getActivity())) {
                    final String appPackageName = getActivity().getPackageName(); // getPackageName() from Context or Activity object
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                    } catch (android.content.ActivityNotFoundException anfe) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                    }
                } else {
                    AppConstants.showAlertDialog(getString(R.string.err_internet),getActivity());
                }
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case android.R.id.home: {
                ((MainActivity) getActivity()).onBackPressed();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
