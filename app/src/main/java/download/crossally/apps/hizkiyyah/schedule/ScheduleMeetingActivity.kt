package download.crossally.apps.hizkiyyah.schedule

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.app.ProgressDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import download.crossally.apps.hizkiyyah.R
import download.crossally.apps.hizkiyyah.bean.Schedule
import download.crossally.apps.hizkiyyah.firebase.DatabaseManager
import download.crossally.apps.hizkiyyah.firebase.DatabaseManager.OnScheduleListener
import download.crossally.apps.hizkiyyah.utils.AppConstants
import download.crossally.apps.hizkiyyah.utils.SharedObjects
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import it.sephiroth.android.library.numberpicker.NumberPicker
import java.util.*

class ScheduleMeetingActivity : AppCompatActivity() {
    @JvmField
    @BindView(R.id.toolbar)
    var toolbar: Toolbar? = null

    @JvmField
    @BindView(R.id.imgBack)
    var imgBack: ImageView? = null

    @JvmField
    @BindView(R.id.txtSave)
    var txtSave: TextView? = null

    @JvmField
    @BindView(R.id.numberPicker)
    var numberPicker: NumberPicker? = null

    @JvmField
    @BindView(R.id.inputLayoutTitle)
    var inputLayoutTitle: TextInputLayout? = null

    @JvmField
    @BindView(R.id.inputLayoutDate)
    var inputLayoutDate: TextInputLayout? = null

    @JvmField
    @BindView(R.id.inputLayoutStartTime)
    var inputLayoutStartTime: TextInputLayout? = null

    @JvmField
    @BindView(R.id.inputLayoutEndTime)
    var inputLayoutEndTime: TextInputLayout? = null

    @JvmField
    @BindView(R.id.edtTitle)
    var edtTitle: TextInputEditText? = null

    @JvmField
    @BindView(R.id.edtDate)
    var edtDate: TextInputEditText? = null

    @JvmField
    @BindView(R.id.edtStartTime)
    var edtStartTime: TextInputEditText? = null

    @JvmField
    @BindView(R.id.edtEndTime)
    var edtEndTime: TextInputEditText? = null
    var sharedObjects: SharedObjects? = null
    var mDatabaseManager: DatabaseManager? = null
    private var databaseReferenceSchedule: DatabaseReference? = null
    private var calendar: Calendar? = null
    var scheduleBean: Schedule? = null
    var meetingCode = ""
    private var progressDialog: ProgressDialog? = null
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_meeting)
        ButterKnife.bind(this)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
            toolbar!!.setNavigationOnClickListener { onBackPressed() }
        }
        sharedObjects = SharedObjects(this@ScheduleMeetingActivity)
        calendar = Calendar.getInstance()
        mDatabaseManager = DatabaseManager(this@ScheduleMeetingActivity)
        databaseReferenceSchedule = FirebaseDatabase.getInstance().getReference(AppConstants.Table.SCHEDULE)
        mDatabaseManager!!.onScheduleListener = object : OnScheduleListener {
            override fun onAddSuccess() {
                AppConstants.showSnackBar("Schedule added successfully", imgBack)
                showSuccessDialog()
                dismissProgressDialog()
            }

            override fun onUpdateSuccess() {
                AppConstants.showSnackBar("Schedule updated successfully", imgBack)
                dismissProgressDialog()
                showSuccessDialog()
            }

            override fun onDeleteSuccess() {}
            override fun onAddFail() {
                dismissProgressDialog()
                AppConstants.showSnackBar("Could not add schedule", imgBack)
            }

            override fun onUpdateFail() {
                dismissProgressDialog()
                AppConstants.showSnackBar("Could not update schedule", imgBack)
            }

            override fun onDeleteFail() {}
        }
        setEdtListeners()
        if (intent.hasExtra(AppConstants.INTENT_BEAN)) {
            scheduleBean = intent.getSerializableExtra(AppConstants.INTENT_BEAN) as Schedule?
            setData()
        } else {
            meetingCode = AppConstants.getMeetingCode()
        }
        numberPicker!!.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS //disable input via keyboard
    }

    private fun setData() {
        meetingCode = scheduleBean!!.meeetingId
        edtTitle!!.setText(scheduleBean!!.title)
        edtDate!!.setText(scheduleBean!!.date)
        numberPicker!!.progress = scheduleBean!!.duration.toInt()
        val startTime = SharedObjects.convertDateFormat(scheduleBean!!.startTime, AppConstants.DateFormats.TIME_FORMAT_24
                , AppConstants.DateFormats.TIME_FORMAT_12)
        edtStartTime!!.setText(startTime)
    }

    fun showSuccessDialog() {
        val dialogDate = Dialog(this@ScheduleMeetingActivity)
        dialogDate.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogDate.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogDate.setContentView(R.layout.dialog_meeting_info)
        dialogDate.setCancelable(true)
        val window = dialogDate.window
        val wlp = window!!.attributes
        wlp.gravity = Gravity.CENTER
        wlp.flags = wlp.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
        wlp.dimAmount = 0.8f
        window.attributes = wlp
        dialogDate.window!!.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val txtName = dialogDate.findViewById<TextView>(R.id.txtName)
        val txtDate = dialogDate.findViewById<TextView>(R.id.txtDate)
        val txtTime = dialogDate.findViewById<TextView>(R.id.txtTime)
        if (!TextUtils.isEmpty(scheduleBean!!.title)) {
            txtName.text = scheduleBean!!.title
        }
        if (!TextUtils.isEmpty(scheduleBean!!.date)) {
            txtDate.text = scheduleBean!!.date
        }
        if (!TextUtils.isEmpty(scheduleBean!!.startTime) && !TextUtils.isEmpty(scheduleBean!!.duration)) {
            val startTime = SharedObjects.convertDateFormat(scheduleBean!!.startTime, AppConstants.DateFormats.TIME_FORMAT_24
                    , AppConstants.DateFormats.TIME_FORMAT_12)
            txtTime.text = "Starts at " + startTime + " (" + scheduleBean!!.duration + " Mins)"
        }
        val btnCalendar = dialogDate.findViewById<Button>(R.id.btnCalendar)
        val btnCancel = dialogDate.findViewById<Button>(R.id.btnCancel)
        btnCalendar.setOnClickListener {
            dialogDate.dismiss()
            addMeetingToCalendar()
        }
        btnCancel.setOnClickListener {
            dialogDate.dismiss()
            onBackPressed()
        }
        if (!dialogDate.isShowing) {
            dialogDate.show()
        }
    }

    private fun setEdtListeners() {
        edtTitle!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                inputLayoutTitle!!.isErrorEnabled = false
                inputLayoutTitle!!.error = ""
            }

            override fun afterTextChanged(editable: Editable) {}
        })
    }

    @OnClick(R.id.imgBack, R.id.txtSave, R.id.edtDate, R.id.edtStartTime, R.id.edtEndTime)
    fun onClick(v: View) {
        val mcurrentTime = Calendar.getInstance()
        val hour = mcurrentTime[Calendar.HOUR_OF_DAY]
        val minute = mcurrentTime[Calendar.MINUTE]
        val mTimePicker: TimePickerDialog
        when (v.id) {
            R.id.imgBack -> onBackPressed()
            R.id.txtSave -> {
                txtSave!!.isEnabled = false
                Handler().postDelayed({ txtSave!!.isEnabled = true }, 1500)
                validateAndSaveMeeting()
            }
            R.id.edtDate -> {
                val datePickerDialog = DatePickerDialog(this@ScheduleMeetingActivity,
                        OnDateSetListener { view, year, monthOfYear, dayOfMonth -> edtDate!!.setText(SharedObjects.pad(dayOfMonth) + "-" + SharedObjects.pad(monthOfYear + 1) + "-" + year) }, calendar!![Calendar.YEAR], calendar!![Calendar.MONTH], calendar!![Calendar.DAY_OF_MONTH])
                datePickerDialog.setCancelable(false)
                datePickerDialog.datePicker.minDate = Date().time
                datePickerDialog.show()
            }
            R.id.edtStartTime -> {
                mTimePicker = TimePickerDialog(this@ScheduleMeetingActivity, TimePickerDialog.OnTimeSetListener { timePicker, selectedHour, selectedMinute ->
                    edtStartTime!!.setText(SharedObjects.convertDateFormat(SharedObjects.pad(selectedHour) + ":" + SharedObjects.pad(selectedMinute),
                            AppConstants.DateFormats.TIME_FORMAT_24,
                            AppConstants.DateFormats.TIME_FORMAT_12))
                }, hour, minute, false)
                mTimePicker.show()
            }
            R.id.edtEndTime -> {
                mTimePicker = TimePickerDialog(this@ScheduleMeetingActivity, TimePickerDialog.OnTimeSetListener { timePicker, selectedHour, selectedMinute ->
                    edtEndTime!!.setText(SharedObjects.convertDateFormat(SharedObjects.pad(selectedHour) + ":" + SharedObjects.pad(selectedMinute),
                            AppConstants.DateFormats.TIME_FORMAT_24,
                            AppConstants.DateFormats.TIME_FORMAT_12))
                }, hour, minute, false)
                mTimePicker.show()
            }
            else -> {
            }
        }
    }

    private fun validateAndSaveMeeting() {
        SharedObjects.hideKeyboard(txtSave!!, this@ScheduleMeetingActivity)
        if (TextUtils.isEmpty(edtTitle!!.text.toString().trim { it <= ' ' })) {
            inputLayoutTitle!!.isErrorEnabled = true
            inputLayoutTitle!!.error = getString(R.string.err_title)
        } else if (TextUtils.isEmpty(edtDate!!.text.toString().trim { it <= ' ' })) {
            AppConstants.showSnackBar(getString(R.string.select_date), edtDate)
        } else if (TextUtils.isEmpty(edtStartTime!!.text.toString().trim { it <= ' ' })) {
            AppConstants.showSnackBar(getString(R.string.select_start_time), edtStartTime)
        } else if (numberPicker!!.progress == 0) {
            AppConstants.showSnackBar(getString(R.string.select_duration), numberPicker)
        } else {
            showProgressDialog()

//            Schedule schedule = new Schedule();
            if (intent.hasExtra(AppConstants.INTENT_BEAN)) {
//                schedule = scheduleBean;
            } else {
                scheduleBean = Schedule()
                scheduleBean!!.userId = sharedObjects?.userInfo()!!.id
                scheduleBean!!.meeetingId = meetingCode
            }
            scheduleBean!!.duration = numberPicker!!.progress.toString() + ""
            scheduleBean!!.title = edtTitle!!.text.toString().trim { it <= ' ' }
            scheduleBean!!.date = edtDate!!.text.toString().trim { it <= ' ' }
            scheduleBean!!.startTime = SharedObjects.convertDateFormat(edtStartTime!!.text.toString().trim { it <= ' ' },
                    AppConstants.DateFormats.TIME_FORMAT_12, AppConstants.DateFormats.TIME_FORMAT_24)
            if (intent.hasExtra(AppConstants.INTENT_BEAN)) {
                mDatabaseManager!!.updateSchedule(scheduleBean!!)
            } else {
                mDatabaseManager!!.addSchedule(scheduleBean!!)
            }
        }
    }

    private fun addMeetingToCalendar() {
        var date = ""
        var startTime = ""
        date = edtDate!!.text.toString().trim { it <= ' ' }
        val splitDate = date.split("-".toRegex()).toTypedArray()
        startTime = SharedObjects!!.convertDateFormat(edtStartTime!!.text.toString().trim { it <= ' ' },
                AppConstants.DateFormats.TIME_FORMAT_12, AppConstants.DateFormats.TIME_FORMAT_24)!!
        val splitStartTime = startTime.split(":".toRegex()).toTypedArray()
        val calStartTime = Calendar.getInstance()
        calStartTime[splitDate[2].toInt(), splitDate[1].toInt() - 1, splitDate[0].toInt(), splitStartTime[0].toInt()] = splitStartTime[1].toInt()

        //open intent
        val i = Intent(Intent.ACTION_EDIT)
        i.type = "vnd.android.cursor.item/event"
        i.putExtra("beginTime", calStartTime.timeInMillis)
        //                i.putExtra("allDay", true);
//                i.putExtra("rule", "FREQ=YEARLY");
        calStartTime.add(Calendar.MINUTE, numberPicker!!.progress)
        i.putExtra("endTime", calStartTime.timeInMillis)
        i.putExtra("title", resources.getString(R.string.app_name) + "-" + edtTitle!!.text.toString().trim { it <= ' ' })
        startActivity(i)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    private fun showProgressDialog() {
        progressDialog = ProgressDialog(this@ScheduleMeetingActivity)
        progressDialog!!.max = 100
        progressDialog!!.setCancelable(false)
        progressDialog!!.setMessage(getString(R.string.please_wait))
        progressDialog!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        if (!this@ScheduleMeetingActivity.isFinishing) {
            progressDialog!!.show()
        }
    }

    fun dismissProgressDialog() {
        if (progressDialog!!.isShowing) progressDialog!!.dismiss()
    }
}