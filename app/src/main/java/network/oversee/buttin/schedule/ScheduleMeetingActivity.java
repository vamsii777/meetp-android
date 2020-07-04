package network.oversee.buttin.schedule;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import network.oversee.buttin.R;
import network.oversee.buttin.bean.Schedule;
import network.oversee.buttin.firebase.DatabaseManager;
import network.oversee.buttin.utils.AppConstants;
import network.oversee.buttin.utils.SharedObjects;

import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;
import it.sephiroth.android.library.numberpicker.NumberPicker;

public class ScheduleMeetingActivity extends AppCompatActivity {

    @BindView(R.id.toolbar) Toolbar toolbar;

    @BindView(R.id.imgBack) ImageView imgBack;

    @BindView(R.id.txtSave) TextView txtSave;
    @BindView(R.id.numberPicker)
    NumberPicker numberPicker;

    @BindView(R.id.inputLayoutTitle) TextInputLayout inputLayoutTitle;
    @BindView(R.id.inputLayoutDate) TextInputLayout inputLayoutDate;
    @BindView(R.id.inputLayoutStartTime) TextInputLayout inputLayoutStartTime;
    @BindView(R.id.inputLayoutEndTime) TextInputLayout inputLayoutEndTime;

    @BindView(R.id.edtTitle) TextInputEditText edtTitle;
    @BindView(R.id.edtDate) TextInputEditText edtDate;
    @BindView(R.id.edtStartTime) TextInputEditText edtStartTime;
    @BindView(R.id.edtEndTime) TextInputEditText edtEndTime;

    SharedObjects sharedObjects ;

    DatabaseManager mDatabaseManager;
    private DatabaseReference databaseReferenceSchedule;

    private Calendar calendar;

    Schedule scheduleBean ;
    String meetingCode = "";
    private ProgressDialog progressDialog;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_meeting);

        ButterKnife.bind(this);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onBackPressed();
                }
            });
        }

        sharedObjects = new SharedObjects(ScheduleMeetingActivity.this);
        calendar = Calendar.getInstance();

        mDatabaseManager = new DatabaseManager(ScheduleMeetingActivity.this);
        databaseReferenceSchedule = FirebaseDatabase.getInstance().getReference(AppConstants.Table.SCHEDULE);

        mDatabaseManager.setOnScheduleListener(new DatabaseManager.OnScheduleListener() {
            @Override
            public void onAddSuccess() {
                AppConstants.showSnackBar("Schedule added successfully",imgBack);
                showSuccessDialog();
                dismissProgressDialog();
            }

            @Override
            public void onUpdateSuccess() {
                AppConstants.showSnackBar("Schedule updated successfully",imgBack);
                dismissProgressDialog();
                showSuccessDialog();
            }

            @Override
            public void onDeleteSuccess() {
            }

            @Override
            public void onAddFail() {
                dismissProgressDialog();
                AppConstants.showSnackBar("Could not add schedule",imgBack);
            }

            @Override
            public void onUpdateFail() {
                dismissProgressDialog();
                AppConstants.showSnackBar("Could not update schedule",imgBack);
            }

            @Override
            public void onDeleteFail() {
            }
        });
        setEdtListeners();

        if (getIntent().hasExtra(AppConstants.INTENT_BEAN)){
            scheduleBean = (Schedule) getIntent().getSerializableExtra(AppConstants.INTENT_BEAN);
            setData();
        }else{
            meetingCode = AppConstants.getMeetingCode();
        }

        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); //disable input via keyboard
    }

    private void setData() {

        meetingCode = scheduleBean.getMeeetingId();

        edtTitle.setText(scheduleBean.getTitle());
        edtDate.setText(scheduleBean.getDate());

        numberPicker.setProgress(Integer.parseInt(scheduleBean.getDuration()));

        String startTime = SharedObjects.convertDateFormat(scheduleBean.getStartTime(),AppConstants.DateFormats.TIME_FORMAT_24
                ,AppConstants.DateFormats.TIME_FORMAT_12);

        edtStartTime.setText(startTime);
    }

    public void showSuccessDialog() {
        final Dialog dialogDate = new Dialog(ScheduleMeetingActivity.this);
        dialogDate.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogDate.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialogDate.setContentView(R.layout.dialog_meeting_info);
        dialogDate.setCancelable(true);

        Window window = dialogDate.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.CENTER;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
        wlp.dimAmount = 0.8f;
        window.setAttributes(wlp);
        dialogDate.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        TextView txtName = dialogDate.findViewById(R.id.txtName);
        TextView txtDate = dialogDate.findViewById(R.id.txtDate);
        TextView txtTime = dialogDate.findViewById(R.id.txtTime);

        if (!TextUtils.isEmpty(scheduleBean.getTitle())) {
            txtName.setText(scheduleBean.getTitle());
        }

        if (!TextUtils.isEmpty(scheduleBean.getDate())) {
            txtDate.setText(scheduleBean.getDate());
        }

        if (!TextUtils.isEmpty(scheduleBean.getStartTime()) && !TextUtils.isEmpty(scheduleBean.getDuration())) {
            String startTime = SharedObjects.convertDateFormat(scheduleBean.getStartTime(),AppConstants.DateFormats.TIME_FORMAT_24
                    ,AppConstants.DateFormats.TIME_FORMAT_12);

            txtTime.setText("Starts at " + startTime + " (" + scheduleBean.getDuration() + " Mins)");
        }

        Button btnCalendar = dialogDate.findViewById(R.id.btnCalendar);
        Button btnCancel = dialogDate.findViewById(R.id.btnCancel);

        btnCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogDate.dismiss();
                addMeetingToCalendar();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogDate.dismiss();
                onBackPressed();
            }
        });

        if (!dialogDate.isShowing()) {
            dialogDate.show();
        }
    }

    private void setEdtListeners() {
        edtTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                inputLayoutTitle.setErrorEnabled(false);
                inputLayoutTitle.setError("");
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

    }

    @OnClick({R.id.imgBack, R.id.txtSave, R.id.edtDate, R.id.edtStartTime, R.id.edtEndTime})
    public void onClick(View v) {

        Calendar mcurrentTime = Calendar.getInstance();
        int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
        int minute = mcurrentTime.get(Calendar.MINUTE);
        TimePickerDialog mTimePicker;

        switch (v.getId()) {
            default:
                break;
            case R.id.imgBack:
                onBackPressed();
                break;
            case R.id.txtSave:

                txtSave.setEnabled(false);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        txtSave.setEnabled(true);
                    }
                }, 1500);

                validateAndSaveMeeting();
                break;
            case R.id.edtDate:
                DatePickerDialog datePickerDialog = new DatePickerDialog(ScheduleMeetingActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                edtDate.setText(SharedObjects.pad(dayOfMonth) + "-" + SharedObjects.pad((monthOfYear + 1)) + "-" + year);
                            }
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

                datePickerDialog.setCancelable(false);
                datePickerDialog.getDatePicker().setMinDate(new Date().getTime());
                datePickerDialog.show();
                break;
            case R.id.edtStartTime:
                mTimePicker = new TimePickerDialog(ScheduleMeetingActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        edtStartTime.setText(SharedObjects.convertDateFormat(SharedObjects.pad(selectedHour) + ":" + SharedObjects.pad(selectedMinute),
                                AppConstants.DateFormats.TIME_FORMAT_24,
                                AppConstants.DateFormats.TIME_FORMAT_12));
                    }
                }, hour, minute, false);
                mTimePicker.show();
                break;
                case R.id.edtEndTime:
                    mTimePicker = new TimePickerDialog(ScheduleMeetingActivity.this, new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                            edtEndTime.setText(SharedObjects.convertDateFormat(SharedObjects.pad(selectedHour) + ":" + SharedObjects.pad(selectedMinute),
                                    AppConstants.DateFormats.TIME_FORMAT_24,
                                    AppConstants.DateFormats.TIME_FORMAT_12));
                        }
                    }, hour, minute, false);

                    mTimePicker.show();
                break;
        }
    }

    private void validateAndSaveMeeting() {

        SharedObjects.hideKeyboard(txtSave, ScheduleMeetingActivity.this);

        if (TextUtils.isEmpty(edtTitle.getText().toString().trim())){
            inputLayoutTitle.setErrorEnabled(true);
            inputLayoutTitle.setError(getString(R.string.err_title));
        }else if (TextUtils.isEmpty(edtDate.getText().toString().trim())){
            AppConstants.showSnackBar(getString(R.string.select_date), edtDate);
        }else if (TextUtils.isEmpty(edtStartTime.getText().toString().trim())){
            AppConstants.showSnackBar(getString(R.string.select_start_time), edtStartTime);
        }else if (numberPicker.getProgress() == 0){
            AppConstants.showSnackBar(getString(R.string.select_duration), numberPicker);
        }else {

            showProgressDialog();

//            Schedule schedule = new Schedule();
            if (getIntent().hasExtra(AppConstants.INTENT_BEAN)){
//                schedule = scheduleBean;
            }else{
                scheduleBean = new Schedule();
                scheduleBean.setUserId(sharedObjects.getUserInfo().getId());
                scheduleBean.setMeeetingId(meetingCode);
            }

            scheduleBean.setDuration(numberPicker.getProgress()+"");

            scheduleBean.setTitle(edtTitle.getText().toString().trim());
            scheduleBean.setDate(edtDate.getText().toString().trim());
            scheduleBean.setStartTime(SharedObjects.convertDateFormat(edtStartTime.getText().toString().trim(),
                    AppConstants.DateFormats.TIME_FORMAT_12,AppConstants.DateFormats.TIME_FORMAT_24));

            if (getIntent().hasExtra(AppConstants.INTENT_BEAN)) {
                mDatabaseManager.updateSchedule(scheduleBean);
            }else{
                mDatabaseManager.addSchedule(scheduleBean);
            }
        }
    }

    private void addMeetingToCalendar() {

        String date = "", startTime = "";

        date = edtDate.getText().toString().trim();
        String [] splitDate = date.split("-");

        startTime = SharedObjects.convertDateFormat(edtStartTime.getText().toString().trim(),
                AppConstants.DateFormats.TIME_FORMAT_12,AppConstants.DateFormats.TIME_FORMAT_24);
        String [] splitStartTime = startTime.split(":");

        Calendar calStartTime = Calendar.getInstance();
        calStartTime.set(Integer.parseInt(splitDate[2]), Integer.parseInt(splitDate[1]) - 1, Integer.parseInt(splitDate[0]),
                Integer.parseInt(splitStartTime[0]), Integer.parseInt(splitStartTime[1]));

        //open intent
        Intent i = new Intent(Intent.ACTION_EDIT);
        i.setType("vnd.android.cursor.item/event");
        i.putExtra("beginTime", calStartTime.getTimeInMillis());
//                i.putExtra("allDay", true);
//                i.putExtra("rule", "FREQ=YEARLY");
        calStartTime.add(Calendar.MINUTE, numberPicker.getProgress());

        i.putExtra("endTime", calStartTime.getTimeInMillis());
        i.putExtra("title", getResources().getString(R.string.app_name) + "-" + edtTitle.getText().toString().trim());
        startActivity(i);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void showProgressDialog() {
        progressDialog = new ProgressDialog(ScheduleMeetingActivity.this);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.please_wait));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        if (!ScheduleMeetingActivity.this.isFinishing()) {
            progressDialog.show();
        }
    }

    public void dismissProgressDialog() {
        if (progressDialog.isShowing())
            progressDialog.dismiss();
    }
}
