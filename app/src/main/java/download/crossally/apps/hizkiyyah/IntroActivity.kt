package download.crossally.apps.hizkiyyah

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.github.ajalt.timberkt.Timber
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.database.*
import download.crossally.apps.hizkiyyah.IntroActivity
import download.crossally.apps.hizkiyyah.RegisterActivity
import download.crossally.apps.hizkiyyah.bean.Intro
import download.crossally.apps.hizkiyyah.bean.MeetingAuth
import download.crossally.apps.hizkiyyah.bean.MeetingHistory
import download.crossally.apps.hizkiyyah.firebase.DatabaseManager
import download.crossally.apps.hizkiyyah.meeting.MeetingActivity
import download.crossally.apps.hizkiyyah.utils.AppConstants
import download.crossally.apps.hizkiyyah.utils.IntroPagerAdapter
import download.crossally.apps.hizkiyyah.utils.SharedObjects
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import me.relex.circleindicator.CircleIndicator
import java.util.*

class IntroActivity : AppCompatActivity() {
    var sharedObjects: SharedObjects? = null

    @JvmField
    @BindView(R.id.viewPager)
    var viewPager: ViewPager? = null

    @JvmField
    @BindView(R.id.circleIndicator)
    var circleIndicator: CircleIndicator? = null

    @JvmField
    @BindView(R.id.btnLogin)
    var btnLogin: Button? = null

    @JvmField
    @BindView(R.id.btnSignUp)
    var btnSignUp: Button? = null

    @JvmField
    @BindView(R.id.btnJoin)
    var btnJoin: Button? = null
    var introPagerAdapter: IntroPagerAdapter? = null
    val DELAY_MS: Long = 2000
    val PERIOD_MS: Long = 3500
    var arrSlider: ArrayList<Intro> = ArrayList<Intro>()
    var appPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    var databaseManager: DatabaseManager? = null
    private var databaseReferenceMeetingHistory: DatabaseReference? = null
    private var databaseReferenceMeetingId: DatabaseReference? = null
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        ButterKnife.bind(this)
        sharedObjects = SharedObjects(this@IntroActivity)
        databaseManager = DatabaseManager(this@IntroActivity)
        //        databaseManager.setDatabaseManagerListener(this);
        databaseReferenceMeetingHistory = FirebaseDatabase.getInstance().getReference(AppConstants.Table.MEETING_HISTORY)
        databaseReferenceMeetingId = FirebaseDatabase.getInstance().getReference(AppConstants.Table.MEETING_ID)
        //arrSlider.add(Intro(ContextCompat.getDrawable(this@IntroActivity, R.drawable.ic_slider_1), "Start a Meeting"))
        //arrSlider.add(Intro(ContextCompat.getDrawable(this@IntroActivity, R.drawable.ic_slider_2), "Schedule Your Meeting"))
        //arrSlider.add(Intro(ContextCompat.getDrawable(this@IntroActivity, R.drawable.ic_slider_3), "Message Your Team"))
        arrSlider.add(Intro(ContextCompat.getDrawable(this@IntroActivity, R.drawable.ic_meeting), "Welcome to Meetp!"))
        introPagerAdapter = IntroPagerAdapter(this@IntroActivity, arrSlider)
        viewPager!!.adapter = introPagerAdapter
        circleIndicator!!.setViewPager(viewPager)

        //enable if need auto slider
        /*val handler = Handler()
        val update = Runnable {
            if (currentPage == NUM_PAGES) {
                currentPage = 0
            }
            viewPager!!.setCurrentItem(currentPage++, true)
        }
        val swipeTimer = Timer()
        swipeTimer.schedule(object : TimerTask() {
            override fun run() {
                handler.post(update)
            }
        }, DELAY_MS, PERIOD_MS)*/
    }

    @OnClick(R.id.btnLogin, R.id.btnSignUp, R.id.btnJoin)
    fun onClick(view: View) {
        when (view.id) {
            R.id.btnLogin -> if (SharedObjects.isNetworkConnected(this@IntroActivity)) {
                startActivity(Intent(this@IntroActivity, LoginActivity::class.java))
            } else {
                AppConstants.showAlertDialog(getString(R.string.err_internet), this@IntroActivity)
            }
            R.id.btnSignUp -> if (SharedObjects.isNetworkConnected(this@IntroActivity)) {
                startActivity(Intent(this@IntroActivity, RegisterActivity::class.java))
            } else {
                AppConstants.showAlertDialog(getString(R.string.err_internet), this@IntroActivity)
            }
            R.id.btnJoin -> if (SharedObjects.isNetworkConnected(this@IntroActivity)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkAppPermissions(appPermissions)) {
                        showMeetingCodeDialog()
                    } else {
                        requestAppPermissions(appPermissions)
                    }
                } else {
                    showMeetingCodeDialog()
                }
            } else {
                AppConstants.showAlertDialog(getString(R.string.err_internet), this@IntroActivity)
            }
        }
    }

    var isMeetingExist = false
    private fun checkMeetingExists(meeting_id: String): Boolean {
        isMeetingExist = false
        val query = databaseReferenceMeetingHistory!!.orderByChild("meeting_id").equalTo(meeting_id)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.e("Meeting", "exists")
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

    var inputLayoutCode: TextInputLayout? = null
    var inputLayoutName: TextInputLayout? = null
    var edtCode: TextInputEditText? = null
    var edtName: TextInputEditText? = null
    fun showMeetingCodeDialog() {
        val dialogDate = Dialog(this@IntroActivity)
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
        edtCode!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                inputLayoutCode!!.setErrorEnabled(false)
                inputLayoutCode!!.setError("")
                if (charSequence.length >= 16) {
                    checkMeetingExists(charSequence.toString())
                } else {
                    isMeetingExist = false
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })
        edtName!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                inputLayoutName!!.setErrorEnabled(false)
                inputLayoutName!!.setError("")
            }

            override fun afterTextChanged(editable: Editable) {}
        })
        val btnAdd = dialogDate.findViewById<Button>(R.id.btnAdd)
        val btnCancel = dialogDate.findViewById<Button>(R.id.btnCancel)
        btnAdd.setOnClickListener(View.OnClickListener {
            val query = databaseReferenceMeetingId!!.orderByChild("meeting_id").equalTo(edtCode!!.getText().toString())
            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Timber.tag("Meeting").e("exists")
                        for (postSnapshot in dataSnapshot.children) {
                            if (postSnapshot.getValue(MeetingAuth::class.java)!!.meeting_id == edtCode!!.getText().toString()) {
                                //isMeetingExist = true
                                AppConstants.MEETING_ID = edtCode!!.getText().toString().trim { it <= ' ' }
                                AppConstants.NAME = edtName!!.getText().toString().trim { it <= ' ' }
                                dialogDate.dismiss()
                                startActivity(Intent(this@IntroActivity, MeetingActivity::class.java))

                            }
                        }

                    } else {
                        //isMeetingExist = false
                        Toast.makeText(this@IntroActivity,"Error! Unauthorized meeting not allowed", Toast.LENGTH_SHORT).show();

                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    isMeetingExist = false
                }
            })
            if (TextUtils.isEmpty(edtCode!!.getText().toString().trim { it <= ' ' })) {
                inputLayoutCode!!.isErrorEnabled = true
                inputLayoutCode!!.setError(getString(R.string.errMeetingCode))
                return@OnClickListener
            }
            if (edtCode!!.getText().toString().length < 8) {
                inputLayoutCode!!.isErrorEnabled = true
                inputLayoutCode!!.setError(getString(R.string.errMeetingCodeInValid))
                return@OnClickListener
            }
           /* if (!isMeetingExist) {
                AppConstants.showAlertDialog(resources.getString(R.string.meeting_not_exist), this@IntroActivity)
                return@OnClickListener
            }*/
            if (TextUtils.isEmpty(edtName!!.getText().toString().trim { it <= ' ' })) {
                inputLayoutName!!.isErrorEnabled = true
                inputLayoutName!!.setError(getString(R.string.err_name))
                return@OnClickListener
            }
            /*AppConstants.MEETING_ID = edtCode!!.getText().toString().trim { it <= ' ' }
            AppConstants.NAME = edtName!!.getText().toString().trim { it <= ' ' }
            dialogDate.dismiss()
            startActivity(Intent(this@IntroActivity, MeetingActivity::class.java))*/
        })
        btnCancel.setOnClickListener { dialogDate.dismiss() }
        if (!dialogDate.isShowing) {
            dialogDate.show()
        }
    }

    fun checkAppPermissions(appPermissions: Array<String>): Boolean {
        //check which permissions are granted
        val listPermissionsNeeded: MutableList<String> = ArrayList()
        for (perm in appPermissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
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
        ActivityCompat.requestPermissions(this, appPermissions, PERMISSION_REQUEST_CODE)
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
                    showMeetingCodeDialog()
                } else {
                    //some permissions are denied
                    for ((permName, permResult) in permissionResults) {
                        //permission is denied and never asked is not checked
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permName)) {
                            val materialAlertDialogBuilder = MaterialAlertDialogBuilder(this@IntroActivity)
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
                            val materialAlertDialogBuilder = MaterialAlertDialogBuilder(this@IntroActivity)
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
        val uri = Uri.fromParts("package", this@IntroActivity.packageName, null)
        intent.data = uri
        startActivityForResult(intent, SETTINGS_REQUEST_CODE)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            SETTINGS_REQUEST_CODE -> {
                Log.e("Settings", "onActivityResult!")
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        if (checkAppPermissions(appPermissions)) {
                            showMeetingCodeDialog()
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
        private var currentPage = 0
        private const val NUM_PAGES = 4
        private const val PERMISSION_REQUEST_CODE = 10001
        private const val SETTINGS_REQUEST_CODE = 10002
    }
}