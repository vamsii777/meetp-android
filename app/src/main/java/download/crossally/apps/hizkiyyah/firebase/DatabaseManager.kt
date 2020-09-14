package download.crossally.apps.hizkiyyah.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.database.*
import download.crossally.apps.hizkiyyah.bean.MeetingHistory
import download.crossally.apps.hizkiyyah.bean.Schedule
import download.crossally.apps.hizkiyyah.bean.UserBean
import download.crossally.apps.hizkiyyah.bean.VideoList
import download.crossally.apps.hizkiyyah.utils.AppConstants
import download.crossally.apps.hizkiyyah.utils.SharedObjects
import timber.log.Timber
import java.util.*

/**
 * The common Database manager is used for storing signin info and other meeting parser classed with list of firebase database
 * references
 */
class DatabaseManager(var context: Context) {
    private val mDatabase: FirebaseDatabase
    var databaseUsers: DatabaseReference
    var databaseMeetingHistory: DatabaseReference
    var databaseExplore: DatabaseReference
    var databaseSchedule: DatabaseReference
    private var mDatabaseListener: OnDatabaseDataChanged? = null
    private var onUserAddedListener: OnUserAddedListener? = null
    var onUserListener: OnUserListener? = null
    private var onUserPasswordListener: OnUserPasswordListener? = null
    var onScheduleListener: OnScheduleListener? = null
    private val onMeetingHistoryListener: OnMeetingHistoryListener? = null
    private val onUserDeleteListener: OnUserDeleteListener? = null
    var userMeetingHistory = ArrayList<MeetingHistory>()
    var userExplore = ArrayList<VideoList>()
    var explore = ArrayList<VideoList>()
    var userSchedule = ArrayList<Schedule>()
    var users = ArrayList<UserBean>()
    var sharedObjects: SharedObjects
    var currentUser: UserBean? = null
    fun initUsers() {
        databaseUsers.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                try {
                    users = ArrayList<UserBean>()
                    for (postSnapshot in dataSnapshot.children) {
                        val customer = postSnapshot.getValue(UserBean::class.java)
                        if (customer != null) {
                            users.add(customer)
                        }
                    }
                    if (mDatabaseListener != null) {
                        mDatabaseListener!!.onDataChanged(AppConstants.Table.USERS, dataSnapshot)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d(TAG, databaseError.message)
                if (mDatabaseListener != null) {
                    mDatabaseListener!!.onCancelled(databaseError)
                }
            }
        })
    }

    fun addUser(bean: UserBean) {
//        String id = databaseUsers.push().getKey();
//        bean.setId(id);
        databaseUsers.child(bean.id).setValue(bean).addOnSuccessListener {
            if (onUserAddedListener != null) {
                onUserAddedListener!!.onSuccess()
            }
        }.addOnFailureListener {
            if (onUserAddedListener != null) {
                onUserAddedListener!!.onFail()
            }
        }
    }

    fun updateUser(bean: UserBean) {
        val db = databaseUsers.child(bean.id)
        db.setValue(bean).addOnSuccessListener {
            if (onUserAddedListener != null) {
                onUserAddedListener!!.onSuccess()
            }
        }.addOnFailureListener {
            if (onUserAddedListener != null) {
                onUserAddedListener!!.onFail()
            }
        }
    }

    fun getUser(id: String) {
        val query = databaseUsers.orderByChild("id").equalTo(id)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (postSnapshot in dataSnapshot.children) {
                        if (postSnapshot.getValue(UserBean::class.java)!!.id == id) {
                            currentUser = postSnapshot.getValue(UserBean::class.java)
                        }
                    }
                }
                if (onUserListener != null) {
                    onUserListener!!.onUserFound()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                if (onUserListener != null) {
                    onUserListener!!.onUserNotFound()
                }
            }
        })
    }

    fun updateUserPassword(bean: UserBean) {
        val db = databaseUsers.child(bean.id)
        db.setValue(bean).addOnSuccessListener {
            if (onUserPasswordListener != null) {
                onUserPasswordListener!!.onPasswordUpdateSuccess()
            }
        }.addOnFailureListener {
            if (onUserPasswordListener != null) {
                onUserPasswordListener!!.onPasswordUpdateFail()
            }
        }
    }

    fun deleteUser(bean: UserBean) {
        databaseUsers.child(bean.id).removeValue { databaseError, databaseReference ->
            if (databaseError != null) {
                onUserDeleteListener?.onUserDeleteFail()
            } else {
                onUserDeleteListener?.onUserDeleteSuccess()
            }
        }
    }

    fun getScheduleByUser(id: String) {
        val query = databaseSchedule.orderByChild("userId").equalTo(id)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                userSchedule = ArrayList<Schedule>()
                if (dataSnapshot.exists()) {
                    for (postSnapshot in dataSnapshot.children) {
                        if (postSnapshot.getValue(Schedule::class.java)!!.userId == id) {
                            val products = postSnapshot.getValue(Schedule::class.java)
                            if (products != null) {
                                userSchedule.add(products)
                            }
                        }
                    }
                }
                if (mDatabaseListener != null) {
                    mDatabaseListener!!.onDataChanged(AppConstants.Table.SCHEDULE, dataSnapshot)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                if (mDatabaseListener != null) {
                    mDatabaseListener!!.onCancelled(databaseError)
                }
            }
        })
    }

    fun addSchedule(bean: Schedule) {
        val id = databaseSchedule.push().key
        bean.id = id
        databaseSchedule.child(bean.id).setValue(bean).addOnSuccessListener {
            if (onScheduleListener != null) {
                onScheduleListener!!.onAddSuccess()
            }
        }.addOnFailureListener {
            if (onScheduleListener != null) {
                onScheduleListener!!.onAddFail()
            }
        }
    }

    fun updateSchedule(bean: Schedule) {
        val db = databaseSchedule.child(bean.id)
        db.setValue(bean).addOnSuccessListener {
            if (onScheduleListener != null) {
                onScheduleListener!!.onUpdateSuccess()
            }
        }.addOnFailureListener {
            if (onScheduleListener != null) {
                onScheduleListener!!.onUpdateFail()
            }
        }
    }

    fun deleteSchedule(bean: Schedule) {
        databaseSchedule.child(bean.id).removeValue { databaseError, databaseReference ->
            if (databaseError != null) {
                if (onScheduleListener != null) {
                    onScheduleListener!!.onDeleteFail()
                }
            } else {
                if (onScheduleListener != null) {
                    onScheduleListener!!.onDeleteSuccess()
                }
            }
        }
    }

    fun addMeetingHistory(bean: MeetingHistory) {
        databaseMeetingHistory.child(bean.id).setValue(bean).addOnSuccessListener { aVoid: Void? -> onMeetingHistoryListener?.onAddSuccess() }.addOnFailureListener { onMeetingHistoryListener?.onAddFail() }
    }

    val keyForMeetingHistory: String?
        get() = databaseMeetingHistory.push().key

    fun updateMeetingHistory(bean: MeetingHistory) {
        val db = databaseMeetingHistory.child(bean.id)
        db.setValue(bean).addOnSuccessListener { onMeetingHistoryListener?.onUpdateSuccess() }.addOnFailureListener { onMeetingHistoryListener?.onUpdateFail() }
    }

    fun getMeetingHistoryByUser(userId: String) {
        val query = databaseMeetingHistory.orderByChild("userId").equalTo(userId)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                userMeetingHistory = ArrayList<MeetingHistory>()
                if (dataSnapshot.exists()) {
                    for (postSnapshot in dataSnapshot.children) {
                        if (postSnapshot.getValue(MeetingHistory::class.java)!!.userId == userId) {
                            val meetingHistory = postSnapshot.getValue(MeetingHistory::class.java)
                            if (meetingHistory != null) {
                                userMeetingHistory.add(meetingHistory)
                            }
                        }
                    }
                }
                if (mDatabaseListener != null) {
                    mDatabaseListener!!.onDataChanged(AppConstants.Table.MEETING_HISTORY, dataSnapshot)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                if (mDatabaseListener != null) {
                    mDatabaseListener!!.onCancelled(databaseError)
                }
            }
        })
    }

    fun getVideoList() {
        val query = databaseExplore.orderByChild("timestamp")
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                userExplore = ArrayList<VideoList>()
                if (dataSnapshot.exists()) {
                    for (postSnapshot in dataSnapshot.children) {
                        val explore = postSnapshot.getValue(VideoList::class.java)
                            if (explore != null) {
                                userExplore.add(explore)
                                Timber.d(explore.toString())
                            }
                    }
                }
                if (mDatabaseListener != null) {
                    mDatabaseListener!!.onDataChanged(AppConstants.Table.VIDEO_LIST, dataSnapshot)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                if (mDatabaseListener != null) {
                    mDatabaseListener!!.onCancelled(databaseError)
                }
            }
        })
    }

    fun deleteMeetingHistory(bean: MeetingHistory) {
        databaseMeetingHistory.child(bean.id).removeValue { databaseError, databaseReference ->
            if (databaseError != null) {
                onMeetingHistoryListener?.onDeleteFail()
            } else {
                onMeetingHistoryListener?.onDeleteSuccess()
            }
        }
    }

    interface OnUserListener {
        fun onUserFound()
        fun onUserNotFound()
    }

    interface OnDatabaseDataChanged {
        fun onDataChanged(url: String?, dataSnapshot: DataSnapshot?)
        fun onCancelled(error: DatabaseError?)
    }

    interface OnUserAddedListener {
        fun onSuccess()
        fun onFail()
    }

    interface OnUserDeleteListener {
        fun onUserDeleteSuccess()
        fun onUserDeleteFail()
    }

    fun setOnUserAddedListener(listener: OnUserAddedListener?) {
        onUserAddedListener = listener
    }

    fun setDatabaseManagerListener(listener: OnDatabaseDataChanged?) {
        mDatabaseListener = listener
    }

    interface OnUserPasswordListener {
        fun onPasswordUpdateSuccess()
        fun onPasswordUpdateFail()
    }

    interface OnScheduleListener {
        fun onAddSuccess()
        fun onUpdateSuccess()
        fun onDeleteSuccess()
        fun onAddFail()
        fun onUpdateFail()
        fun onDeleteFail()
    }

    interface OnMeetingHistoryListener {
        fun onAddSuccess()
        fun onUpdateSuccess()
        fun onDeleteSuccess()
        fun onAddFail()
        fun onUpdateFail()
        fun onDeleteFail()
    }

    fun setOnUserPasswordListener(onUserPasswordListener: OnUserPasswordListener?) {
        this.onUserPasswordListener = onUserPasswordListener
    }

    companion object {
        private val TAG = DatabaseManager::class.java.simpleName
    }

    init {
        sharedObjects = SharedObjects(context)
        mDatabase = FirebaseDatabase.getInstance()
        databaseUsers = mDatabase.getReference(AppConstants.Table.USERS)
        databaseUsers.keepSynced(true)
        databaseMeetingHistory = mDatabase.getReference(AppConstants.Table.MEETING_HISTORY)
        databaseMeetingHistory.keepSynced(true)
        databaseExplore = mDatabase.getReference(AppConstants.Table.VIDEO_LIST)
        databaseExplore.keepSynced(true)
        databaseSchedule = mDatabase.getReference(AppConstants.Table.SCHEDULE)
        databaseSchedule.keepSynced(true)
    }
}