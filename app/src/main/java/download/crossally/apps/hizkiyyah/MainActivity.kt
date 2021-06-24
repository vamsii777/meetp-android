package download.crossally.apps.hizkiyyah

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import butterknife.BindView
import butterknife.ButterKnife
import com.github.javiersantos.appupdater.AppUpdaterUtils
import com.github.javiersantos.appupdater.enums.AppUpdaterError
import com.github.javiersantos.appupdater.objects.Update
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import download.crossally.apps.hizkiyyah.IntroActivity
import download.crossally.apps.hizkiyyah.explore.ExploreFragment
import download.crossally.apps.hizkiyyah.firebase.DatabaseManager
import download.crossally.apps.hizkiyyah.firebase.DatabaseManager.OnUserListener
import download.crossally.apps.hizkiyyah.home.HomeFragment
import download.crossally.apps.hizkiyyah.maxloghistory.MeetingHistoryFragment
import download.crossally.apps.hizkiyyah.schedule.ScheduleFragment
import download.crossally.apps.hizkiyyah.settings.SettingsFragment
import download.crossally.apps.hizkiyyah.utils.AppConstants
import download.crossally.apps.hizkiyyah.utils.SharedObjects
import io.github.inflationx.viewpump.ViewPumpContextWrapper


class MainActivity : AppCompatActivity(){
    private var firebaseAuth: FirebaseAuth? = null
    private var authListener: AuthStateListener? = null
    var doubleBackToExitPressedOnce = false
    var databaseReferenceUser: DatabaseReference? = null
    var databaseManager: DatabaseManager? = null
    var sharedObjects: SharedObjects? = null

    @JvmField
    @BindView(R.id.navigation)
    var navigation: BottomNavigationView? = null
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)
        val showLimit = this.intent.extras?.getBoolean("alertlimit")
        if (showLimit == true) {
            MaterialAlertDialogBuilder(this)
                    .setTitle("Limit Exceeded")
                    .setMessage("Your free meeting has expired.")
                    .setPositiveButton("GOT IT") { dialogInterface, i ->
                        this.intent.extras?.remove("alertlimit")
                    }
                    .setCancelable(false)
                    .show()
        }


        sharedObjects = SharedObjects(this@MainActivity)

        //get firebase auth instance
        firebaseAuth = FirebaseAuth.getInstance()
        databaseManager = DatabaseManager(this@MainActivity)
        databaseManager!!.onUserListener = object : OnUserListener {
            override fun onUserFound() {
                sharedObjects!!.setPreference(AppConstants.USER_INFO, Gson().toJson(databaseManager!!.currentUser))
                updateFragments()
            }

            override fun onUserNotFound() {
                removeAllPreferenceOnLogout()
            }
        }
        databaseReferenceUser = FirebaseDatabase.getInstance().getReference(AppConstants.Table.USERS)
        authListener = AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                Log.e("user", "null")
                // user auth state is changed - user is null
                // launch login activity
                onLogout()
            } else {
                databaseManager!!.getUser(user.uid)
            }
        }
        firebaseAuth!!.addAuthStateListener(authListener!!)
        loadFragment(HomeFragment())
        navigation!!.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
    }

    override fun onStart() {
        super.onStart()
        if (SharedObjects.isNetworkConnected(this@MainActivity)) {
            val appUpdaterUtils = AppUpdaterUtils(this)
                    .withListener(object : AppUpdaterUtils.UpdateListener {
                        override fun onSuccess(update: Update, isUpdateAvailable: Boolean) {
                            if (isUpdateAvailable) {
                                launchUpdateDialog(update.latestVersion)
                            }
                        }

                        override fun onFailed(error: AppUpdaterError) {}
                    })
            appUpdaterUtils.start()
        }
        val content: CoordinatorLayout = findViewById(R.id.container)
        content.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }

    private fun launchUpdateDialog(onlineVersion: String) {
        try {
            val materialAlertDialogBuilder = MaterialAlertDialogBuilder(this@MainActivity)
            materialAlertDialogBuilder.setMessage("Update " + onlineVersion + " is available to download. Downloading the latest update you will get the latest features," +
                    "improvements and bug fixes of " + getString(R.string.app_name))
            materialAlertDialogBuilder.setCancelable(false).setPositiveButton(resources.getString(R.string.update_now)) { dialog, which ->
                dialog.dismiss()
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
            }
            materialAlertDialogBuilder.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateFragments() {
        val fragment = supportFragmentManager.findFragmentById(R.id.flContent)
        if (fragment is HomeFragment) {
            fragment.setUserData()
        }
    }

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { menuItem ->
        var bundle: Bundle
        val fragment: Fragment?
        val fragmentClass: Class<*>
        when (menuItem.itemId) {
            R.id.nav_home -> {
                fragmentClass = HomeFragment::class.java
                try {
                    fragment = fragmentClass.newInstance() as Fragment
                    loadFragment(fragment, menuItem)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return@OnNavigationItemSelectedListener true
            }
            R.id.nav_settings -> {
                fragmentClass = SettingsFragment::class.java
                try {
                    fragment = fragmentClass.newInstance() as Fragment
                    loadFragment(fragment, menuItem)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return@OnNavigationItemSelectedListener true
            }
//            R.id.nav_explore -> {
//                fragmentClass = ExploreFragment::class.java
//                try {
//                    fragment = fragmentClass.newInstance() as Fragment
//                    loadFragment(fragment, menuItem)
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//                return@OnNavigationItemSelectedListener true
//            }
//            R.id.nav_meeting_history -> {
//                fragmentClass = MeetingHistoryFragment::class.java
//                try {
//                    fragment = fragmentClass.newInstance() as Fragment
//                    loadFragment(fragment, menuItem)
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//                return@OnNavigationItemSelectedListener true
//            }
            R.id.nav_schedule -> {
                fragmentClass = ScheduleFragment::class.java
                try {
                    fragment = fragmentClass.newInstance() as Fragment
                    loadFragment(fragment, menuItem)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    fun selectMenuItem(menu: String) {
        if (menu == getString(R.string.menu_home)) {
            navigation!!.menu.findItem(R.id.nav_home).isChecked = true
        } else if (menu == getString(R.string.meeting_history)) {
            navigation!!.menu.findItem(R.id.nav_meeting_history).isChecked = true
        } else if (menu == getString(R.string.schedule)) {
            navigation!!.menu.findItem(R.id.nav_schedule).isChecked = true
        } else if (menu == getString(R.string.menu_settings)) {
            navigation!!.menu.findItem(R.id.nav_settings).isChecked = true
        } else {
            navigation!!.menu.findItem(R.id.nav_home).isChecked = true
            navigation!!.menu.findItem(R.id.nav_meeting_history).isChecked = false
            navigation!!.menu.findItem(R.id.nav_schedule).isChecked = false
            navigation!!.menu.findItem(R.id.nav_settings).isChecked = false
        }
    }

    override fun onResume() {
        super.onResume()
        doubleBackToExitPressedOnce = false
    }

    override fun onBackPressed() {
        Handler().postDelayed({
            val f = supportFragmentManager.findFragmentById(R.id.flContent)
            if (f is HomeFragment) {
                selectMenuItem(getString(R.string.menu_home))
            } else if (f is SettingsFragment) {
                selectMenuItem(getString(R.string.menu_settings))
            } else if (f is ScheduleFragment) {
                selectMenuItem(getString(R.string.schedule))
            } else if (f is MeetingHistoryFragment) {
                selectMenuItem(getString(R.string.meeting_history))
            }
        }, 200)
        if (supportFragmentManager.backStackEntryCount == 1) {
            if (doubleBackToExitPressedOnce) {
                finish()
                System.exit(0)
                return
            }
            doubleBackToExitPressedOnce = true
            Toast.makeText(this, getString(R.string.exit), Toast.LENGTH_SHORT).show()
            Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
        } else {
            super.onBackPressed()
        }
    }

    private fun loadFragment(fragment: Fragment) {
        val backStateName = fragment.javaClass.name
        val manager = supportFragmentManager
        val fragmentPopped = manager.popBackStackImmediate(backStateName, 0)
        if (!fragmentPopped && manager.findFragmentByTag(backStateName) == null) { //fragment not in back stack, create it.
            val ft = manager.beginTransaction()
            ft.replace(R.id.flContent, fragment, backStateName)
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            ft.addToBackStack(backStateName)
            ft.commit()
        }
    }

    private fun loadFragment(fragment: Fragment, menuItem: MenuItem) {
        val backStateName = fragment.javaClass.getName()
        val manager = supportFragmentManager
        val fragmentPopped = manager.popBackStackImmediate(backStateName, 0)
        if (!fragmentPopped && manager.findFragmentByTag(backStateName) == null) { //fragment not in back stack, create it.
            val ft = manager.beginTransaction()
            ft.replace(R.id.flContent, fragment!!, backStateName)
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            ft.addToBackStack(backStateName)
            ft.commit()
            menuItem.isChecked = true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        for (fragment in supportFragmentManager.fragments) {
            fragment.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun onLogout() {
        sharedObjects!!.removeSinglePreference(AppConstants.USER_INFO)
        val intent = Intent(this@MainActivity, IntroActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    fun removeAllPreferenceOnLogout() {
        try {
            firebaseAuth!!.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}