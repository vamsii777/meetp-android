package network.oversee.buttin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import network.oversee.buttin.R;

import network.oversee.buttin.utils.SharedObjects;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;


public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 3000;
    @BindView(R.id.txtVersionName) TextView txtVersionName;
    SharedObjects sharedObjects ;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //Get Firebase auth instance
        firebaseAuth = FirebaseAuth.getInstance();

        sharedObjects = new SharedObjects(SplashActivity.this);

        ButterKnife.bind(this);

        txtVersionName.setText(getString(R.string.version,SharedObjects.getVersion(SplashActivity.this)));

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                Intent intentLogin;

                if (firebaseAuth.getCurrentUser() != null) {
                    if (checkIfEmailVerified()){
                        intentLogin = new Intent(SplashActivity.this, MainActivity.class);
                        intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intentLogin);
                        finish();
                    }else{
                        intentLogin = new Intent(SplashActivity.this, IntroActivity.class);
                        intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intentLogin);
                        finish();
                    }
                } else {
                    intentLogin = new Intent(SplashActivity.this, IntroActivity.class);
                    intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intentLogin);
                    finish();
                }
            }
        }, SPLASH_TIME_OUT);
    }

    private boolean checkIfEmailVerified() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user.isEmailVerified()) {
            // user is verified
            return true;
        } else {
            // email is not verified
            // NOTE: don't forget to log out the user.
            firebaseAuth.signOut();
            return false;
        }
    }
}
