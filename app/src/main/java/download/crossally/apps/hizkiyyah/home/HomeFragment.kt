package download.crossally.apps.hizkiyyah.home

import android.Manifest
import android.annotation.SuppressLint
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
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
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
import com.github.ajalt.timberkt.Timber
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.mikhaellopez.circularimageview.CircularImageView
import com.squareup.picasso.Picasso
import download.crossally.apps.hizkiyyah.R
import download.crossally.apps.hizkiyyah.bean.MeetingAuth
import download.crossally.apps.hizkiyyah.bean.MeetingHistory
import download.crossally.apps.hizkiyyah.bean.UserBean
import download.crossally.apps.hizkiyyah.firebase.DatabaseManager
import download.crossally.apps.hizkiyyah.firebase.DatabaseManager.OnDatabaseDataChanged
import download.crossally.apps.hizkiyyah.maxloghistory.MeetingHistoryAdapter
import download.crossally.apps.hizkiyyah.meeting.MeetingActivity
import download.crossally.apps.hizkiyyah.meeting.NewMeetingActivity
import download.crossally.apps.hizkiyyah.profile.ProfileActivity
import download.crossally.apps.hizkiyyah.schedule.ScheduleMeetingActivity
import download.crossally.apps.hizkiyyah.utils.AppConstants
import download.crossally.apps.hizkiyyah.utils.SharedObjects
import download.crossally.apps.hizkiyyah.utils.SimpleDividerItemDecoration
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment(), OnDatabaseDataChanged{
    private var sharedObjects: SharedObjects? = null

    @JvmField
    @BindView(R.id.txtUserName)
    var txtUserName: TextView? = null

    @JvmField
    @BindView(R.id.imgUser)
    var imgUser: CircularImageView? = null

    @JvmField
    @BindView(R.id.llJoin)
    var llJoin: LinearLayout? = null

    @JvmField
    @BindView(R.id.llSchedule)
    var llSchedule: LinearLayout? = null

//    @JvmField
//    @BindView(R.id.llNewMeeting)
//    var llNewMeeting: LinearLayout? = null
    var userBean: UserBean? = null
    private var arrMeetingHistory = ArrayList<MeetingHistory>()
    var meetingHistoryAdapter: MeetingHistoryAdapter? = null
    var databaseManager: DatabaseManager? = null
    private var databaseReferenceMeetingHistory: DatabaseReference? = null
    private var databaseReferenceMeetingId: DatabaseReference? = null

    @JvmField
    @BindView(R.id.llError)
    var llError: LinearLayout? = null

    @JvmField
    @BindView(R.id.rvHistory)
    var rvHistory: RecyclerView? = null

    @JvmField
    @BindView(R.id.txtError)
    var txtError: TextView? = null
    var appPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    private var adView: AdView? = null
    var mInterstitialAd: InterstitialAd? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    private final var TAG = "HomeFrag"
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        ButterKnife.bind(this, view)
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        sharedObjects = SharedObjects(requireActivity())
        databaseManager = DatabaseManager(requireActivity())
        databaseManager!!.setDatabaseManagerListener(this)
        setUserData()
        databaseReferenceMeetingHistory = FirebaseDatabase.getInstance().getReference(AppConstants.Table.MEETING_HISTORY)
        databaseReferenceMeetingId = FirebaseDatabase.getInstance().getReference(AppConstants.Table.MEETING_ID)
        rvHistory!!.isNestedScrollingEnabled = false
        adView = view.findViewById(R.id.adView)
        // set the ad unit ID

        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(requireContext(),"ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, adError?.message)
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d(TAG, "Ad was loaded.".toString())
                mInterstitialAd = interstitialAd
            }
        })


        bindAdvtView()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkAppPermissions(appPermissions)) {
                requestAppPermissions(appPermissions)
            }
        }
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireActivity());
        loadInterstitial()
        return view
    }

    private fun loadInterstitial() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(requireActivity())
        } else {
            Log.d("TAG", "The interstitial ad wasn't ready yet.")
        }
    }

    private fun bindAdvtView() {
        if (SharedObjects.isNetworkConnected(Objects.requireNonNull(requireActivity()))) {
            val adRequest = AdRequest.Builder() //                      .addTestDevice("23F1C653C3AF44D748738885C1F91FDA")
                    .build()
            adView!!.adListener = object : AdListener() {
                override fun onAdLoaded() {

                }
                override fun onAdClosed() {
//                Toast.makeText(getApplicationContext(), "Ad is closed!", Toast.LENGTH_SHORT).show();
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    adView!!.visibility = View.GONE
                    //                Toast.makeText(getApplicationContext(), "Ad failed to load! error code: " + errorCode, Toast.LENGTH_SHORT).show();
                }

                fun onAdLeftApplication() {
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

    var isMeetingExist = false
    private fun checkMeetingExists(meeting_id: String): Boolean {
        isMeetingExist = false
        val query = databaseReferenceMeetingHistory!!.orderByChild("meeting_id").equalTo(meeting_id)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {

                    Timber.tag("Meeting").e("exists")
                    for (postSnapshot in dataSnapshot.children) {
                        if (postSnapshot.getValue(MeetingHistory::class.java)!!.meeting_id == meeting_id) {
                            isMeetingExist = true

                        }
                    }

                } else {
                    isMeetingExist = false

                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                isMeetingExist = false
            }
        })
        return isMeetingExist
    }


    override fun onResume() {
        super.onResume()
    }

    @SuppressLint("SetTextI18n")
    fun setUserData() {
        Picasso.get().load(FirebaseAuth.getInstance().currentUser?.photoUrl)
            .into(imgUser)

        userBean = sharedObjects!!.userInfo()
        if (userBean != null) {

            if (!TextUtils.isEmpty(userBean?.profile_pic)) {
                Picasso.get().load(userBean?.profile_pic)
                        .error(R.drawable.avatar).into(imgUser)
            } else {
                imgUser!!.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.avatar))
            }
            if (!TextUtils.isEmpty(userBean?.name)) {
                txtUserName!!.text = "Hi, " + FirebaseAuth.getInstance().currentUser?.displayName
            } else {
                txtUserName!!.text = "Hi, " + FirebaseAuth.getInstance().currentUser?.displayName
            }
            databaseManager!!.getMeetingHistoryByUser(sharedObjects?.userInfo()?.id!!) // meetingid by userid
        } else {
            txtUserName!!.text = "Hi, " + FirebaseAuth.getInstance().currentUser?.displayName
            imgUser!!.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.avatar))
        }
        imgUser!!.setOnClickListener() {
            startActivity(Intent(activity, ProfileActivity::class.java))
        }
    }

    override fun onDataChanged(url: String?, dataSnapshot: DataSnapshot?) {
        if (url.equals(AppConstants.Table.MEETING_HISTORY, ignoreCase = true)) {
            if (this@HomeFragment.isVisible) {
                arrMeetingHistory = ArrayList()
                if (databaseManager!!.userMeetingHistory.size > 0) {
                    for (i in databaseManager!!.userMeetingHistory.indices) {
                        val bean = databaseManager!!.userMeetingHistory[i]
                        if (!TextUtils.isEmpty(bean.startTime)) {
                            val date = SharedObjects.convertDateFormat(bean.startTime, AppConstants.DateFormats.DATETIME_FORMAT_24, AppConstants.DateFormats.DATE_FORMAT_DD_MMM_YYYY)
                            if (date.equals(SharedObjects.getTodaysDate(AppConstants.DateFormats.DATE_FORMAT_DD_MMM_YYYY), ignoreCase = true)) {
                                arrMeetingHistory.add(bean)
                            }
                        }
                    }
                }
                setMeetingHistoryAdapter()
            }
        }
    }

    override fun onCancelled(error: DatabaseError?) {
        arrMeetingHistory = ArrayList()
        setMeetingHistoryAdapter()
    }

    private fun setMeetingHistoryAdapter() = if (arrMeetingHistory.size > 0) {
        arrMeetingHistory.sortWith(Comparator { arg0, arg1 ->
            @SuppressLint("SimpleDateFormat") val format = SimpleDateFormat(
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
        })
        meetingHistoryAdapter = MeetingHistoryAdapter(arrMeetingHistory, activity)
        rvHistory!!.adapter = meetingHistoryAdapter
        rvHistory!!.addItemDecoration(SimpleDividerItemDecoration(requireActivity()))
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

    @OnClick(R.id.llJoin, R.id.llSchedule)
    fun onClick(v: View) {
        when (v.id) {
            R.id.llJoin ->
            {
                loadInterstitial()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkAppPermissions(appPermissions)) {
                        showMeetingCodeDialog()
                    } else {
                        requestAppPermissions(appPermissions)
                    }
                } else {
                    showMeetingCodeDialog()
                }
            }
            R.id.llSchedule -> startActivity(Intent(activity, ScheduleMeetingActivity::class.java))
            else -> {
            }
        }
    }

    fun showMeetingShareDialog() {
        val dialogDate = Dialog(requireActivity())
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
            val myClipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val myClip: ClipData = ClipData.newPlainText("text", txtMeetingURL.text.toString())
            myClipboard.setPrimaryClip(myClip)
            Toast.makeText(activity, "Link copied", Toast.LENGTH_SHORT).show()
        }
        txtMeetingURL.setOnClickListener {
            val myClipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val myClip: ClipData = ClipData.newPlainText("text", txtMeetingURL.text.toString())
            myClipboard.setPrimaryClip(myClip)
            Toast.makeText(activity, "Link copied", Toast.LENGTH_SHORT).show()
        }
        val btnContinue = dialogDate.findViewById<Button>(R.id.btnContinue)
        btnContinue.setOnClickListener {
            dialogDate.dismiss()
            startActivity(Intent(activity, NewMeetingActivity::class.java))
        }
        if (!dialogDate.isShowing) {
            dialogDate.show()
        }
    }

    var inputLayoutCode: TextInputLayout? = null
    var inputLayoutName: TextInputLayout? = null
    var edtCode: TextInputEditText? = null
    var edtName: TextInputEditText? = null
    private val mAuth: FirebaseAuth? = null
    private var mFirebaseAnalytics: FirebaseAnalytics? = null
    fun showMeetingCodeDialog() {
        val dialogDate = Dialog(requireActivity())
        dialogDate.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogDate.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogDate.setContentView(R.layout.dialog_meeting_code)
        dialogDate.setCancelable(true)
        val window = dialogDate.window
        val wlp = window!!.attributes
        wlp.gravity = Gravity.CENTER
        wlp.flags = wlp.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
        wlp.dimAmount = 0.8f
        window.attributes = wlp
        dialogDate.window!!.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        inputLayoutCode = dialogDate.findViewById(R.id.inputLayoutCode)
        inputLayoutName = dialogDate.findViewById(R.id.inputLayoutName)
        edtCode = dialogDate.findViewById(R.id.edtCode)
        edtName = dialogDate.findViewById(R.id.edtName)
        edtName!!.setText(sharedObjects!!.userInfo()?.name)
        edtCode!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                inputLayoutCode!!.isErrorEnabled = false
                inputLayoutCode!!.error = ""
                if (charSequence.length >= 16) {
                    checkMeetingExists(charSequence.toString())
                } else {
                    isMeetingExist = false
                }
            }

            override fun afterTextChanged(editable: Editable) {

            }
        })
        edtName!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                inputLayoutName!!.isErrorEnabled = false
                inputLayoutName!!.error = ""
            }

            override fun afterTextChanged(editable: Editable) {}
        })
        //Log.e((AppConstants.USER_INFO))
        val btnAdd = dialogDate.findViewById<Button>(R.id.btnAdd)
        val btnCancel = dialogDate.findViewById<Button>(R.id.btnCancel)
        btnAdd.setOnClickListener(View.OnClickListener {
            AppConstants.MEETING_ID = edtCode!!.getText().toString().trim { it <= ' ' }
            AppConstants.NAME = edtName!!.getText().toString().trim { it <= ' ' }
            dialogDate.dismiss()
            startActivity(Intent(activity, MeetingActivity::class.java))
            val params = Bundle()
            params.putString("meeting_id", edtCode!!.getText().toString())
            mFirebaseAnalytics!!.logEvent("meetings_data", params)

            if (TextUtils.isEmpty(edtCode!!.getText().toString().trim { it <= ' ' })) {
                inputLayoutCode!!.isErrorEnabled = true
                inputLayoutCode!!.error = getString(R.string.errMeetingCode)
                return@OnClickListener
            }
            if (edtCode!!.getText().toString().length < 8) {
                inputLayoutCode!!.isErrorEnabled = true
                inputLayoutCode!!.setError(getString(R.string.errMeetingCodeInValid))
                return@OnClickListener
            }
            if (isMeetingExist) {
                //checkMeetingExists(edtCode!!.getText().toString())
                //Toast.makeText(requireActivity(), "MeetingExists!", Toast.LENGTH_SHORT).show();
                //AppConstants.showAlertDialog(resources.getString(R.string.meeting_not_exist), activity)
                return@OnClickListener
            }
            /*if (isMeetingExist) {
                checkMeetingExists(edtCode!!.getText().toString())
                //Toast.makeText(requireActivity(),"Error! Unauthorized meeting not allowed",Toast.LENGTH_SHORT).show();
                //AppConstants.showAlertDialog(resources.getString(R.string.meeting_not_exist), activity)
                return@OnClickListener
            }*/
            if (TextUtils.isEmpty(edtName!!.getText().toString().trim { it <= ' ' })) {
                inputLayoutName!!.isErrorEnabled = true
                inputLayoutName!!.setError(getString(R.string.err_name))
                return@OnClickListener
            }


        })
        btnCancel.setOnClickListener { dialogDate.dismiss() }
        if (!dialogDate.isShowing) {
            dialogDate.show()
        }
    }

    fun nonLicenseUser(){
        val dialogDate = Dialog(requireActivity())
        val query = databaseReferenceMeetingHistory!!.orderByChild("meeting_id").equalTo(edtCode!!.getText().toString())
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    Timber.tag("NonLMeeting").e("exists")
                    AppConstants.MEETING_ID = edtCode!!.getText().toString().trim { it <= ' ' }
                    AppConstants.NAME = edtName!!.getText().toString().trim { it <= ' ' }
                    dialogDate.dismiss()
                    startActivity(Intent(activity, NewMeetingActivity::class.java))
                    val params = Bundle()
                    params.putString("meeting_id", edtCode!!.getText().toString())
                    mFirebaseAnalytics!!.logEvent("meetings_data", params)
                } else {
                    inputLayoutCode!!.isErrorEnabled = true
                    inputLayoutCode!!.setError("MeetingID doesn't exist")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        })
    }
    fun checkAppPermissions(appPermissions: Array<String>): Boolean {
        //check which permissions are granted
        val listPermissionsNeeded: MutableList<String> = ArrayList()
        for (perm in appPermissions) {
            if (ContextCompat.checkSelfPermission(requireActivity(), perm) != PackageManager.PERMISSION_GRANTED) {
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
        ActivityCompat.requestPermissions(requireActivity(), appPermissions, PERMISSION_REQUEST_CODE)
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
                    Timber.tag("Permissions").e("All permissions are granted!")
                    //invoke ur method
                } else {
                    //some permissions are denied
                    for ((permName, permResult) in permissionResults) {
                        //permission is denied and never asked is not checked
                        if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permName)) {
                            val materialAlertDialogBuilder = MaterialAlertDialogBuilder(requireActivity())
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
                            val materialAlertDialogBuilder = MaterialAlertDialogBuilder(requireActivity())
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
        val uri = Uri.fromParts("package", requireActivity().packageName, null)
        intent.data = uri
        startActivityForResult(intent, SETTINGS_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            SETTINGS_REQUEST_CODE -> {
                Timber.tag("Settings").e("onActivityResult!")
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 10001
        private const val SETTINGS_REQUEST_CODE = 10002
    }
}