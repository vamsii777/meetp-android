package download.crossally.apps.hizkiyyah.meeting

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.facebook.react.modules.core.PermissionListener
import com.github.ajalt.timberkt.Timber
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.gson.Gson
import download.crossally.apps.hizkiyyah.IntroActivity
import download.crossally.apps.hizkiyyah.MainActivity
import download.crossally.apps.hizkiyyah.bean.MeetingHistory
import download.crossally.apps.hizkiyyah.bean.UserBean
import download.crossally.apps.hizkiyyah.firebase.DatabaseManager
import download.crossally.apps.hizkiyyah.utils.AppConstants
import download.crossally.apps.hizkiyyah.utils.SharedObjects
import org.jitsi.meet.sdk.*
import java.net.MalformedURLException
import java.net.URL


class NewMeetingActivity : FragmentActivity(), JitsiMeetActivityInterface {
    private var firebaseAuth: FirebaseAuth? = null
    private var view: JitsiMeetView? = null
    var sharedObjects: SharedObjects? = null
    var mDatabaseManager: DatabaseManager? = null
    var meetingHistory: MeetingHistory? = null
    var value: Long? = null
    private var databaseReferenceMeetingHistory: DatabaseReference? = null
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        JitsiMeetActivityDelegate.onActivityResult(this, requestCode, resultCode, data)
    }

    override fun onBackPressed() {
        JitsiMeetActivityDelegate.onBackPressed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = JitsiMeetView(this)
        firebaseAuth = FirebaseAuth.getInstance()
        sharedObjects = SharedObjects(this@NewMeetingActivity)
        mDatabaseManager = DatabaseManager(this@NewMeetingActivity)
        var userBean: UserBean? = null
        limekiln()
        if (sharedObjects?.userInfo() != null) {
            userBean = sharedObjects?.userInfo()
            meetingHistory = MeetingHistory()
            meetingHistory!!.id = mDatabaseManager!!.keyForMeetingHistory
            meetingHistory!!.userId = sharedObjects!!.userInfo()?.id
            meetingHistory!!.meeting_id = AppConstants.MEETING_ID
        }
        val jitsiMeetUserInfo = JitsiMeetUserInfo()
        if (userBean != null) {
            jitsiMeetUserInfo.displayName = userBean.name
            try {
                if (!TextUtils.isEmpty(userBean.profile_pic)) {
                    jitsiMeetUserInfo.avatar = URL(userBean.profile_pic)
                }
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            }
        } else {
            jitsiMeetUserInfo.displayName = AppConstants.NAME
        }
        var options: JitsiMeetConferenceOptions? = null
        try {
            options = JitsiMeetConferenceOptions.Builder()
                    .setServerURL(URL("https://meet.jit.si"))
                    .setRoom(AppConstants.MEETING_ID)
                    .setUserInfo(jitsiMeetUserInfo)
                    .setFeatureFlag("meeting-password.enabled", false)
                    .setFeatureFlag("lobby.enabled", false)
                    .setFeatureFlag("recording.enabled", false)
                    .setFeatureFlag("invite.enabled", false)
                    .setFeatureFlag("pip.enabled", false)
                    .setFeatureFlag("live-streaming.enabled", false)
                    .setFeatureFlag("conference-timer.enabled", true)
                    .setFeatureFlag("resolution", true)
                    .setFeatureFlag("call-integration.enabled", false)
                    .setVideoMuted(true)
                    .setAudioMuted(true)

                    .build()
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }
//        if (meetingHistory != null) {
//            meetingHistory!!.subject = options!!.subject
//        }
        view!!.join(options)
        view!!.listener = object : JitsiMeetViewListener {
            override fun onConferenceJoined(map: Map<String, Any>) {
                Log.e("Conference", " Joined: " + "")
                if (meetingHistory != null) {
                    meetingHistory!!.startTime = SharedObjects.getTodaysDate(AppConstants.DateFormats.DATETIME_FORMAT_24)
                    meetingHistory!!.endTime = ""
                    Log.e("meetingHistory", Gson().toJson(meetingHistory))
                    saveMeetingDetails()
                }
            }

            override fun onConferenceTerminated(map: Map<String, Any>) {
                Log.e("Conference", " terminated: " + "data")
                if (meetingHistory != null) {
                    if (TextUtils.isEmpty(meetingHistory!!.startTime)) {
                        meetingHistory!!.startTime = SharedObjects.getTodaysDate(AppConstants.DateFormats.DATETIME_FORMAT_24)
                    }
                    meetingHistory!!.endTime = SharedObjects.getTodaysDate(AppConstants.DateFormats.DATETIME_FORMAT_24)
                    updateMeetingDetails()
                }
                onBackPressed()
            }

            override fun onConferenceWillJoin(map: Map<String, Any>) {}
        }
        checkMeetingExists()
        limekiln()
        setContentView(view)

    }

    fun limekiln(){
        // Write a message to the database
        // Write a message to the database
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("NoLicenseMeetingID").child("meeting_duration")
        // Read from the database
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                value = dataSnapshot.getValue(Long::class.java)
                //value = value1?.toLong()
                Timber.tag("Data").e("exists$value")
                //Log.d("NewMeeting Duration", "Value is: $value")
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                //Log.w(TAG, "Failed to read value.", error.toException())
            }
        })
        if (firebaseAuth?.currentUser ==null){
            Handler().postDelayed(Runnable {
                view?.dispose()
                view = null
                val myIntent = Intent(this, IntroActivity::class.java)
                myIntent.putExtra("alertlimit2", true)
                startActivity(myIntent)
                finish()

            }, 4500000)
        } else {
            Handler().postDelayed(Runnable {
                view?.dispose()
                view = null
                val myIntent = Intent(this, MainActivity::class.java)
                myIntent.putExtra("alertlimit", true)
                startActivity(myIntent)
                finish()

            }, 4500000)
        }

        }

    var isMeetingExist = false
    private fun checkMeetingExists(): Boolean {
        isMeetingExist = false
        val query = databaseReferenceMeetingHistory?.orderByChild("meeting_id")?.equalTo(AppConstants.meetingCode)
        query?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {

                    Timber.tag("Meeting").e("exists")
                    for (postSnapshot in dataSnapshot.children) {
                        if (postSnapshot.getValue(MeetingHistory::class.java)!!.meeting_id == AppConstants.meetingCode) {
                            isMeetingExist = true
                            //limekiln()

                        }
                    }

                } else {
                    //limekiln()
                    isMeetingExist = false

                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                isMeetingExist = false
            }
        })
        return isMeetingExist
    }

    private fun saveMeetingDetails() {
        mDatabaseManager!!.addMeetingHistory(meetingHistory!!)
    }

    private fun updateMeetingDetails() {
        mDatabaseManager!!.updateMeetingHistory(meetingHistory!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        view?.dispose()
        view = null
        JitsiMeetActivityDelegate.onHostDestroy(this)
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        JitsiMeetActivityDelegate.onNewIntent(intent)
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        JitsiMeetActivityDelegate.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onResume() {
        super.onResume()
        JitsiMeetActivityDelegate.onHostResume(this)
    }

    override fun onStop() {
        super.onStop()
        JitsiMeetActivityDelegate.onHostPause(this)
    }

    override fun requestPermissions(strings: Array<String>, i: Int, permissionListener: PermissionListener) {}
}