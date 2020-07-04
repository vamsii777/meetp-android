package network.oversee.buttin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import network.oversee.buttin.R;

import network.oversee.buttin.firebase.DatabaseManager;
import network.oversee.buttin.utils.AppConstants;
import network.oversee.buttin.utils.SharedObjects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;

public class EmailVerificationActivity extends AppCompatActivity {

    @BindView(R.id.btnVerify) Button btnVerify;

    @BindView(R.id.txtEmail) TextView txtEmail;

    SharedObjects sharedObjects;

    private FirebaseAuth firebaseAuth;

    DatabaseManager databaseManager ;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);

        ButterKnife.bind(this);

        //Get Firebase auth instance
        firebaseAuth = FirebaseAuth.getInstance();

        sharedObjects = new SharedObjects(EmailVerificationActivity.this);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);


        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        databaseManager = new DatabaseManager(EmailVerificationActivity.this);

        verifyUser();
    }

    public void verifyUser(){

        final FirebaseUser user = firebaseAuth.getCurrentUser();
        txtEmail.setText(user.getEmail());

        user.sendEmailVerification()
                .addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(EmailVerificationActivity.this,
                                    "Verification email sent to " + user.getEmail(),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e("sendEmailVerification "," " + task.getException());
                            AppConstants.showAlertDialog(task.getException().getMessage(), EmailVerificationActivity.this);
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @OnClick({R.id.btnVerify})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnVerify:
                if (firebaseAuth != null && firebaseAuth.getCurrentUser() != null){
                    firebaseAuth.getCurrentUser().reload();

                    SharedObjects.hideKeyboard(btnVerify, EmailVerificationActivity.this);
                    if (SharedObjects.isNetworkConnected(EmailVerificationActivity.this)) {
                        if (firebaseAuth.getCurrentUser() != null && firebaseAuth.getCurrentUser().isEmailVerified()){

                            Intent intent = new Intent(EmailVerificationActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivity(intent);
                            overridePendingTransition(0,0);
                            finish();

                        }else{
                            AppConstants.showAlertDialog("Please verify your email address and try again.", EmailVerificationActivity.this);
                        }
                    } else {
                        AppConstants.showAlertDialog(getString(R.string.err_internet), EmailVerificationActivity.this);
                    }
                }
                break;
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }


}
