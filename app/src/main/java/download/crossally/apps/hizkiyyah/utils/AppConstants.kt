package download.crossally.apps.hizkiyyah.utils

import android.content.Context
import android.util.Patterns
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import download.crossally.apps.hizkiyyah.R
import download.crossally.apps.hizkiyyah.utils.SharedObjects.Companion.getTodaysDate
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object AppConstants {
    const val USER_INFO = "user_info"
    const val INTENT_BEAN = "BeanData"
    const val INTENT_ID = "ID"
    var NAME = "NAME"
    var MEETING_ID = "MEETING_ID"
    const val IMAGE_DIRECTORY_NAME = "Meetp"
    const val Storage_Path = "users/images/"
    @JvmStatic
    fun checkDateisFuture(selectedDate: String): Boolean {
        val myFormat = SimpleDateFormat(DateFormats.DATE_FORMAT_DASH)
        try {
            val date1 = myFormat.parse(selectedDate)
            val date2 = myFormat.parse(getTodaysDate(DateFormats.DATE_FORMAT_DASH))
            if (date2.before(date1)) {
                return true
            } else if (date2 == date1) {
                return true
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return false
    }

    fun showSnackBar(message: String?, view: View?) {
        val snackBar = Snackbar.make(view!!, message!!, Snackbar.LENGTH_SHORT)
        snackBar.show()
    }

    fun showAlertDialog(Msg: String?, context: Context) {
        val materialAlertDialogBuilder = MaterialAlertDialogBuilder(context)
        materialAlertDialogBuilder.setMessage(Msg)
        materialAlertDialogBuilder.setCancelable(false).setPositiveButton(context.resources.getString(R.string.ok)) { dialog, which -> dialog.dismiss() }
        materialAlertDialogBuilder.show()
    }

    fun isValidEmail(target: CharSequence?): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(target).matches()
    }

    val meetingCode: String
        get() {
            var meetingCode = ""
            val randomString = getRandomString(9)
            val size = 3
            var start = 0
            while (start < randomString.length) {
                meetingCode += randomString.substring(start, Math.min(randomString.length, start + size)) + "-"
                start += size
            }
            return meetingCode.substring(0, meetingCode.length - 1)
        }
    private const val ALLOWED_CHARACTERS = "qwertyuiopasdfghjklzxcvbnm"
    private fun getRandomString(sizeOfRandomString: Int): String {
        val random = Random()
        val sb = StringBuilder(sizeOfRandomString)
        for (i in 0 until sizeOfRandomString) sb.append(ALLOWED_CHARACTERS[random.nextInt(ALLOWED_CHARACTERS.length)])
        return sb.toString()
    }

    object Table {
        var USERS = "Users"
        var MEETING_HISTORY = "MeetingHistory"
        var VIDEO_LIST = "videoList"
        var MEETING_ID = "MeetingId"
        var SCHEDULE = "Schedule"
    }

    object DateFormats {
        @JvmField
        var DATE_FORMAT_DD_MMM_YYYY = "dd MMM yyyy"
        var DATE_FORMAT_DASH = "dd-MM-yyyy"
        @JvmField
        var TIME_FORMAT_12 = "hh:mm a"
        @JvmField
        var TIME_FORMAT_24 = "HH:mm"
        @JvmField
        var DATETIME_FORMAT_24 = "dd-MM-yyyy HH:mm:ss"
        var DATETIME_FORMAT_12 = "dd-MM-yyyy hh:mm a"
    }
}