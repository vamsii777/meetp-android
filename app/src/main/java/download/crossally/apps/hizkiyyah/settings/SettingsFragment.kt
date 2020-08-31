package download.crossally.apps.hizkiyyah.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import download.crossally.apps.hizkiyyah.MainActivity
import download.crossally.apps.hizkiyyah.R
import download.crossally.apps.hizkiyyah.profile.ProfileActivity
import download.crossally.apps.hizkiyyah.utils.AppConstants
import download.crossally.apps.hizkiyyah.utils.SharedObjects

class SettingsFragment : Fragment() {
    var sharedObjects: SharedObjects? = null

    @JvmField
    @BindView(R.id.llProfile)
    var llProfile: LinearLayout? = null

    @JvmField
    @BindView(R.id.llRateUs)
    var llRateUs: LinearLayout? = null

    @JvmField
    @BindView(R.id.llLogout)
    var llLogout: LinearLayout? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        ButterKnife.bind(this, view)
        sharedObjects = SharedObjects(activity)
        return view
    }

    @OnClick(R.id.llProfile, R.id.llRateUs, R.id.llLogout)
    fun onClick(v: View) {
        when (v.id) {
            R.id.llProfile -> startActivity(Intent(activity, ProfileActivity::class.java))
            R.id.llLogout -> (activity as MainActivity?)!!.removeAllPreferenceOnLogout()
            R.id.llRateUs -> if (SharedObjects.isNetworkConnected(activity)) {
                val appPackageName = activity!!.packageName // getPackageName() from Context or Activity object
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
                } catch (anfe: ActivityNotFoundException) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
                }
            } else {
                AppConstants.showAlertDialog(getString(R.string.err_internet), activity)
            }
            else -> {
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            android.R.id.home -> {
                (activity as MainActivity?)!!.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}