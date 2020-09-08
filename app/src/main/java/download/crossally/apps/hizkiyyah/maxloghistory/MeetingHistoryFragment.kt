package download.crossally.apps.hizkiyyah.maxloghistory

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import download.crossally.apps.hizkiyyah.R
import download.crossally.apps.hizkiyyah.bean.MeetingHistory
import download.crossally.apps.hizkiyyah.firebase.DatabaseManager
import download.crossally.apps.hizkiyyah.firebase.DatabaseManager.OnDatabaseDataChanged
import download.crossally.apps.hizkiyyah.maxloghistory.MeetingHistoryFragment
import download.crossally.apps.hizkiyyah.meeting.MeetingActivity
import download.crossally.apps.hizkiyyah.utils.AppConstants
import download.crossally.apps.hizkiyyah.utils.SharedObjects
import download.crossally.apps.hizkiyyah.utils.SimpleDividerItemDecoration
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class MeetingHistoryFragment : Fragment(), OnDatabaseDataChanged {
    @JvmField
    @BindView(R.id.llError)
    var llError: LinearLayout? = null

    @JvmField
    @BindView(R.id.rvHistory)
    var rvHistory: RecyclerView? = null
    private var adView: AdView? = null

    @JvmField
    @BindView(R.id.txtError)
    var txtError: TextView? = null
    var databaseManager: DatabaseManager? = null
    private var arrMeetingHistory = ArrayList<MeetingHistory>()
    var meetingHistoryAdapter: MeetingHistoryAdapter? = null
    var sharedObjects: SharedObjects? = null
    var appPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_meeting_history, container, false)
        ButterKnife.bind(this, view)
        activity!!.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        sharedObjects = SharedObjects(activity!!)
        databaseManager = DatabaseManager(activity!!)
        databaseManager!!.setDatabaseManagerListener(this)
        adView = view.findViewById(R.id.adView)
        bindAdvtView()
        data
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkAppPermissions(appPermissions)) {
                requestAppPermissions(appPermissions)
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
    }

    private fun bindAdvtView() {
        if (SharedObjects.isNetworkConnected(Objects.requireNonNull(activity!!))) {
            val adRequest = AdRequest.Builder() //                      .addTestDevice("23F1C653C3AF44D748738885C1F91FDA")
                    .build()
            adView!!.adListener = object : AdListener() {
                override fun onAdLoaded() {}
                override fun onAdClosed() {
//                Toast.makeText(getApplicationContext(), "Ad is closed!", Toast.LENGTH_SHORT).show();
                }

                override fun onAdFailedToLoad(errorCode: Int) {
                    adView!!.visibility = View.GONE
                    //                Toast.makeText(getApplicationContext(), "Ad failed to load! error code: " + errorCode, Toast.LENGTH_SHORT).show();
                }

                override fun onAdLeftApplication() {
//                Toast.makeText(getApplicationContext(), "Ad left application!", Toast.LENGTH_SHORT).show();
                }

                override fun onAdOpened() {
                    super.onAdOpened()
                }
            }
            adView!!.loadAd(adRequest)
            adView!!.visibility = View.VISIBLE
        } else {
            adView!!.visibility = View.GONE
        }
    }

    val data: Unit
        get() {
            if (sharedObjects?.userInfo() != null) {
                databaseManager!!.getMeetingHistoryByUser(sharedObjects?.userInfo()!!.id)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun setMeetingHistoryAdapter() {
        if (arrMeetingHistory.size > 0) {
            Collections.sort(arrMeetingHistory) { arg0, arg1 ->
                val format = SimpleDateFormat(
                        AppConstants.DateFormats.DATETIME_FORMAT_24)
                var compareResult = 0
                try {
                    val arg0Date = format.parse(arg0.startTime)
                    val arg1Date = format.parse(arg1.startTime)
                    compareResult = arg1Date.compareTo(arg0Date)
                } catch (e: ParseException) {
                    e.printStackTrace()
                }
                compareResult
            }
            meetingHistoryAdapter = MeetingHistoryAdapter(arrMeetingHistory, activity)
            rvHistory!!.adapter = meetingHistoryAdapter
            rvHistory!!.addItemDecoration(SimpleDividerItemDecoration(activity!!))
            meetingHistoryAdapter!!.setOnItemClickListener(object : MeetingHistoryAdapter.OnItemClickListener {
                override fun onItemClickListener(position: Int, bean: MeetingHistory) {}
                override fun onDeleteClickListener(position: Int, bean: MeetingHistory) {
                    databaseManager!!.deleteMeetingHistory(bean)
                }

                override fun onJoinClickListener(position: Int, bean: MeetingHistory) {
                    AppConstants.MEETING_ID = bean.meeting_id
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkAppPermissions(appPermissions)) {
                            startActivity(Intent(activity, MeetingActivity::class.java))
                        } else {
                            requestAppPermissions(appPermissions)
                        }
                    } else {
                        startActivity(Intent(activity, MeetingActivity::class.java))
                    }
                }
            })
            rvHistory!!.visibility = View.VISIBLE
            llError!!.visibility = View.GONE
        } else {
            rvHistory!!.visibility = View.GONE
            llError!!.visibility = View.VISIBLE
        }
    }

    override fun onDataChanged(url: String?, dataSnapshot: DataSnapshot?) {
        if (url.equals(AppConstants.Table.MEETING_HISTORY, ignoreCase = true)) {
            if (this@MeetingHistoryFragment.isVisible) {
                arrMeetingHistory = ArrayList()
                arrMeetingHistory.addAll(databaseManager!!.userMeetingHistory)
                setMeetingHistoryAdapter()
            }
        }
    }

    override fun onCancelled(error: DatabaseError?) {
        if (this@MeetingHistoryFragment.isVisible) {
            arrMeetingHistory = ArrayList()
            setMeetingHistoryAdapter()
        }
    }

    fun checkAppPermissions(appPermissions: Array<String>): Boolean {
        //check which permissions are granted
        val listPermissionsNeeded: MutableList<String> = ArrayList()
        for (perm in appPermissions) {
            if (ContextCompat.checkSelfPermission(activity!!, perm) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(perm)
            }
        }

        //Ask for non granted permissions
        return if (!listPermissionsNeeded.isEmpty()) {
            false
        } else true
        // App has all permissions
    }

    private fun requestAppPermissions(appPermissions: Array<String>) {
        ActivityCompat.requestPermissions(activity!!, appPermissions, PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                val permissionResults = HashMap<String, Int>()
                var deniedCount = 0
                var i = 0
                while (i < grantResults.size) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        permissionResults[permissions[i]] = grantResults[i]
                        deniedCount++
                    }
                    i++
                }
                if (deniedCount == 0) {
                    Log.e("Permissions", "All permissions are granted!")
                    //invoke ur method
                } else {
                    //some permissions are denied
                    for ((permName, permResult) in permissionResults) {
                        //permission is denied and never asked is not checked
                        if (ActivityCompat.shouldShowRequestPermissionRationale(activity!!, permName)) {
                            val materialAlertDialogBuilder = MaterialAlertDialogBuilder(activity!!)
                            materialAlertDialogBuilder.setMessage(getString(R.string.permission_msg))
                            materialAlertDialogBuilder.setCancelable(false)
                                    .setNegativeButton(getString(R.string.no)) { dialog, which -> dialog.cancel() }
                                    .setPositiveButton(getString(R.string.yes_grant_permission)) { dialog, id ->
                                        dialog.cancel()
                                        if (!checkAppPermissions(appPermissions)) {
                                            requestAppPermissions(appPermissions)
                                        }
                                    }
                            materialAlertDialogBuilder.show()
                            break
                        } else { //permission is denied and never asked is checked
                            val materialAlertDialogBuilder = MaterialAlertDialogBuilder(activity!!)
                            materialAlertDialogBuilder.setMessage(getString(R.string.permission_msg_never_checked))
                            materialAlertDialogBuilder.setCancelable(false)
                                    .setNegativeButton(getString(R.string.no)) { dialog, which -> dialog.cancel() }
                                    .setPositiveButton(getString(R.string.go_to_settings)) { dialog, id ->
                                        dialog.cancel()
                                        openSettings()
                                    }
                            materialAlertDialogBuilder.show()
                            break
                        }
                    }
                }
            }
        }
    }

    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", activity!!.packageName, null)
        intent.data = uri
        startActivityForResult(intent, SETTINGS_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            SETTINGS_REQUEST_CODE -> {
                Log.e("Settings", "onActivityResult!")
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        if (checkAppPermissions(appPermissions)) {
                        } else {
                            requestAppPermissions(appPermissions)
                        }
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 10001
        private const val SETTINGS_REQUEST_CODE = 10002
    }
}