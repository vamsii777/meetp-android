package download.crossally.apps.hizkiyyah.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import com.bugsnag.android.Bugsnag
import com.facebook.stetho.Stetho
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.smartlook.sdk.smartlook.Smartlook
import download.crossally.apps.hizkiyyah.R
import download.crossally.apps.hizkiyyah.bean.UserBean
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class SharedObjects : MultiDexApplication {
    var sharedPreference: SharedPreferences? = null
    var editor: SharedPreferences.Editor? = null

    constructor() {}

    @SuppressLint("CommitPrefEdits")
    constructor(context: Context) {
        Companion.context = context
        initializeStetho()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        sharedPreference = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        editor = sharedPreference!!.edit()
        ViewPump.init(ViewPump.builder()
                .addInterceptor(CalligraphyInterceptor(
                        CalligraphyConfig.Builder()
                                .setDefaultFontPath("fonts/montserrat_regular.ttf")
                                .setFontAttrId(R.attr.fontPath)
                                .build()))
                .build())
    }

    override fun onCreate() {
        instance = SharedObjects(this)
        super.onCreate()
        context = applicationContext
        Bugsnag.start(this)
        Smartlook.setupAndStartRecording("94ea4378c7929993fe016a701ff175c3de7b81c1");
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
    val gson = GsonBuilder().create();
    val userInfo: UserBean get() = gson.fromJson(getPreference(AppConstants.USER_INFO), UserBean::class.java)


    fun initializeStetho() {
        val initializerBuilder = Stetho.newInitializerBuilder(context)
        initializerBuilder.enableWebKitInspector(Stetho.defaultInspectorModulesProvider(context))
        initializerBuilder.enableDumpapp(Stetho.defaultDumperPluginsProvider(context))
        val initializer = initializerBuilder.build()
        Stetho.initialize(initializer)
    }

    @SuppressLint("CommitPrefEdits")
    fun setPreference(key: String?, value: String?) {
        editor = sharedPreference!!.edit()
        editor!!.putString(key, value)
        editor!!.commit()
    }

    fun getPreference(key: String?): String? {
        return try {
            sharedPreference!!.getString(key, "")
        } catch (exception: Exception) {
            ""
        }
    }

    fun removeSinglePreference(pref: String?) {
        if (sharedPreference!!.contains(pref)) {
            editor = sharedPreference!!.edit()
            editor!!.remove(pref)
            editor!!.commit()
        }
    }

    fun clear() {
        editor = sharedPreference!!.edit()
        editor!!.clear()
        editor!!.commit()
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        var context: Context? = null
        var PRIVATE_MODE = 0
        var PREF_NAME = "Meetp"

        @SuppressLint("StaticFieldLeak")
        var instance: SharedObjects? = null
            private set

        @JvmName("getContext1")
        fun getContext(): Context? {
            return instance
            // or return instance.getApplicationContext();
        }

        fun isNetworkConnected(context: Context): Boolean {
            val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.activeNetworkInfo != null
        }

        fun hideKeyboard(view: View, c: Context) {
            val inputMethodManager = c.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }

        @JvmStatic
        fun pad(c: Int): String {
            return if (c >= 10) c.toString() else "0$c"
        }

        fun getVersion(context: Context): String {
            var versionName = ""
            try {
                versionName = context.packageManager
                        .getPackageInfo(context.packageName, 0).versionName
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
            return versionName
        }

        val deviceVersion: String
            get() = Build.VERSION.RELEASE
        val deviceName: String
            get() {
                val manufacturer = Build.MANUFACTURER
                val model = Build.MODEL
                return if (model.startsWith(manufacturer)) {
                    capitalize(model)
                } else {
                    capitalize(manufacturer) + " " + model
                }
            }

        private fun capitalize(s: String?): String {
            if (s == null || s.length == 0) {
                return ""
            }
            val first = s[0]
            return if (Character.isUpperCase(first)) {
                s
            } else {
                Character.toUpperCase(first).toString() + s.substring(1)
            }
        }

        @JvmStatic
        fun getTodaysDate(dateFormat: String?): String {
            val c = Calendar.getInstance()
            @SuppressLint("SimpleDateFormat") val df = SimpleDateFormat(dateFormat)
            return df.format(c.time)
        }

        @JvmStatic
        fun convertDateFormat(dateString: String?, originalDateFormat: String?, outputDateFormat: String?): String? {
            var finalDate: String? = null
            var simpleDateFormat = SimpleDateFormat(originalDateFormat)
            try {
                val date = simpleDateFormat.parse(dateString)
                simpleDateFormat = SimpleDateFormat(outputDateFormat)
                finalDate = simpleDateFormat.format(date)
            } catch (e: ParseException) {
                e.printStackTrace()
            }
            return finalDate
        }
    }
}