package download.crossally.apps.hizkiyyah.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
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

    private val ACTIVITY_CALLBACK = 1
    private var reviewInfo: ReviewInfo? = null
    private lateinit var reviewManager: ReviewManager

    @JvmField
    @BindView(R.id.llLogout)
    var llLogout: LinearLayout? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        ButterKnife.bind(this, view)
        sharedObjects = SharedObjects(requireActivity())
        //Create the ReviewManager instance
        reviewManager = ReviewManagerFactory.create(requireContext())
        val requestFlow = reviewManager.requestReviewFlow()
        requestFlow.addOnCompleteListener { request ->
            if (request.isSuccessful) {
                //Received ReviewInfo object
                reviewInfo = request.result
            } else {
                //Problem in receiving object
                reviewInfo = null
            }
        }
        return view
    }

    @OnClick(R.id.llProfile, R.id.llPolicy, R.id.llRateUs, R.id.llLogout)
    fun onClick(v: View) {
        when (v.id) {
            R.id.llProfile -> startActivity(Intent(activity, ProfileActivity::class.java))
            R.id.llLogout -> (activity as MainActivity?)!!.removeAllPreferenceOnLogout()
            R.id.llPolicy -> if (SharedObjects.isNetworkConnected(requireActivity())) {
            val url = "http://legal.oversee.network/applet/meetp/"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            } else {
                AppConstants.showAlertDialog(getString(R.string.err_internet), requireActivity())
            }
            R.id.llRateUs ->
                if (SharedObjects.isNetworkConnected(requireActivity())) {
                    val appPackageName = requireActivity().packageName // getPackageName() from Context or Activity object
//                try {
//                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
//                } catch (anfe: ActivityNotFoundException) {
//                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
//                }
                    startActivityForResult(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")), ACTIVITY_CALLBACK)
                } else {
                    AppConstants.showAlertDialog(getString(R.string.err_internet), requireActivity())
                }
            else -> {
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ACTIVITY_CALLBACK && resultCode == Activity.RESULT_OK) {
            Handler().postDelayed({
                reviewInfo?.let {
                    val flow = reviewManager.launchReviewFlow(requireActivity(), it)
                    flow.addOnSuccessListener {
                        //Showing toast is only for testing purpose, this shouldn't be implemented
                        //in production app.
                        Toast.makeText(
                                requireContext(),
                                "Thanks for the feedback!",
                                Toast.LENGTH_LONG
                        ).show()
                    }
                    flow.addOnFailureListener {
                        //Showing toast is only for testing purpose, this shouldn't be implemented
                        //in production app.
                        Toast.makeText(requireContext(), "${it.message}", Toast.LENGTH_LONG).show()
                    }
                    flow.addOnCompleteListener {
                        //Showing toast is only for testing purpose, this shouldn't be implemented
                        //in production app.
                        //Toast.makeText(requireContext(), "Thanks!", Toast.LENGTH_LONG).show()
                    }
                }
            }, 3000)
        }
        super.onActivityResult(requestCode, resultCode, data)

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