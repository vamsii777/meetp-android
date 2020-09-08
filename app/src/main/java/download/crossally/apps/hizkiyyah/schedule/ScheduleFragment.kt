package download.crossally.apps.hizkiyyah.schedule

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import download.crossally.apps.hizkiyyah.R
import download.crossally.apps.hizkiyyah.bean.Schedule
import download.crossally.apps.hizkiyyah.firebase.DatabaseManager
import download.crossally.apps.hizkiyyah.firebase.DatabaseManager.OnDatabaseDataChanged
import download.crossally.apps.hizkiyyah.meeting.MeetingActivity
import download.crossally.apps.hizkiyyah.schedule.ScheduleFragment
import download.crossally.apps.hizkiyyah.utils.AppConstants
import download.crossally.apps.hizkiyyah.utils.SharedObjects
import download.crossally.apps.hizkiyyah.utils.SimpleDividerItemDecoration
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class ScheduleFragment : Fragment(), OnDatabaseDataChanged {
    @JvmField
    @BindView(R.id.llError)
    var llError: LinearLayout? = null

    @JvmField
    @BindView(R.id.rvEvents)
    var rvEvents: RecyclerView? = null

    @JvmField
    @BindView(R.id.imgAdd)
    var imgAdd: ImageView? = null

    @JvmField
    @BindView(R.id.txtError)
    var txtError: TextView? = null
    private var arrSchedule = ArrayList<Schedule>()
    var scheduleAdapter: ScheduleAdapter? = null
    var sharedObjects: SharedObjects? = null
    var databaseManager: DatabaseManager? = null
    var appPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_schedule, container, false)
        ButterKnife.bind(this, view)
        activity!!.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        sharedObjects = SharedObjects(activity!!)
        databaseManager = DatabaseManager(requireActivity())
        databaseManager!!.setDatabaseManagerListener(this)
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

    val data: Unit
        get() {
            if (sharedObjects?.userInfo() != null) {
                databaseManager!!.getScheduleByUser(sharedObjects?.userInfo()!!.id)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun setScheduleAdapter() {
        if (arrSchedule.size > 0) {
            Collections.sort(arrSchedule) { arg0, arg1 ->
                val format = SimpleDateFormat(
                        AppConstants.DateFormats.DATE_FORMAT_DASH)
                var compareResult = 0
                try {
                    val arg0Date = format.parse(arg0.date)
                    val arg1Date = format.parse(arg1.date)
                    compareResult = arg1Date.compareTo(arg0Date)
                    //                                            return (arg0Date.getTime() > arg1Date.getTime() ? 1 : -1);
                } catch (e: ParseException) {
                    e.printStackTrace()
                    //           compareResult = arg0.compareTo(arg1);
                }
                compareResult
            }
            scheduleAdapter = ScheduleAdapter(arrSchedule, activity)
            rvEvents!!.adapter = scheduleAdapter
            rvEvents!!.addItemDecoration(SimpleDividerItemDecoration(activity!!))
            scheduleAdapter!!.setOnItemClickListener(object : ScheduleAdapter.OnItemClickListener {
                override fun onItemClickListener(position: Int, bean: Schedule) {
                    startActivity(Intent(activity, ScheduleMeetingActivity::class.java)
                            .putExtra(AppConstants.INTENT_BEAN, bean))
                }

                override fun onDeleteClickListener(position: Int, bean: Schedule) {
                    databaseManager!!.deleteSchedule(bean)
                }

                override fun onStartClickListener(position: Int, bean: Schedule) {
                    AppConstants.MEETING_ID = bean.meeetingId
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkAppPermissions(appPermissions)) {
                            showMeetingShareDialog()
                        } else {
                            requestAppPermissions(appPermissions)
                        }
                    } else {
                        showMeetingShareDialog()
                    }
                }
            })
            rvEvents!!.visibility = View.VISIBLE
            llError!!.visibility = View.GONE
        } else {
            rvEvents!!.visibility = View.GONE
            llError!!.visibility = View.VISIBLE
        }
    }

    fun showMeetingShareDialog() {
        val dialogDate = Dialog(activity!!)
        dialogDate.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogDate.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogDate.setContentView(R.layout.dialog_meeting_share)
        dialogDate.setCancelable(true)
        val window = dialogDate.window
        val wlp = window!!.attributes
        wlp.gravity = Gravity.CENTER
        wlp.flags = wlp.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
        wlp.dimAmount = 0.8f
        window.attributes = wlp
        dialogDate.window!!.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val txtMeetingURL = dialogDate.findViewById<TextView>(R.id.txtMeetingURL)
        val imgCopy = dialogDate.findViewById<ImageView>(R.id.imgCopy)
        txtMeetingURL.text = AppConstants.MEETING_ID
        imgCopy.setOnClickListener {
            val myClipboard = activity!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val myClip: ClipData
            myClip = ClipData.newPlainText("text", txtMeetingURL.text.toString())
            myClipboard.setPrimaryClip(myClip)
            Toast.makeText(activity, "Link copied", Toast.LENGTH_SHORT).show()
        }
        txtMeetingURL.setOnClickListener {
            val myClipboard = activity!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val myClip: ClipData
            myClip = ClipData.newPlainText("text", txtMeetingURL.text.toString())
            myClipboard.setPrimaryClip(myClip)
            Toast.makeText(activity, "Link copied", Toast.LENGTH_SHORT).show()
        }
        val btnContinue = dialogDate.findViewById<Button>(R.id.btnContinue)
        btnContinue.setOnClickListener {
            dialogDate.dismiss()
            startActivity(Intent(activity, MeetingActivity::class.java))
        }
        if (!dialogDate.isShowing) {
            dialogDate.show()
        }
    }

    @OnClick(R.id.imgAdd)
    fun onClick(v: View) {
        when (v.id) {
            R.id.imgAdd -> startActivity(Intent(activity, ScheduleMeetingActivity::class.java))
            else -> {
            }
        }
    }

    override fun onDataChanged(url: String?, dataSnapshot: DataSnapshot?) {
        if (url.equals(AppConstants.Table.SCHEDULE, ignoreCase = true)) {
            if (this@ScheduleFragment.isVisible) {
                arrSchedule = ArrayList()
                arrSchedule.addAll(databaseManager!!.userSchedule)
                Log.e("getUserSchedule", arrSchedule.size.toString() + " s")
                setScheduleAdapter()
            }
        }
    }

    override fun onCancelled(error: DatabaseError?) {
        if (this@ScheduleFragment.isVisible) {
            arrSchedule = ArrayList()
            setScheduleAdapter()
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
//            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), PERMISSION_REQUEST_CODE);
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
    } /*
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_home, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                ((HomeActivity) getActivity()).openDrawer();
                return true;
            case R.id.action_notification:
//                Log.e("Notification","clicked");
                return true;
//            case R.id.action_logout:
//                ((MainActivity)getActivity()).removeAllPreferenceOnLogout();
//                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }*/

    companion object {
        private const val PERMISSION_REQUEST_CODE = 10001
        private const val SETTINGS_REQUEST_CODE = 10002
    }
}