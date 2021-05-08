package download.crossally.apps.hizkiyyah.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import download.crossally.apps.hizkiyyah.R
import download.crossally.apps.hizkiyyah.bean.Intro
import java.util.*

class IntroPagerAdapter(var context: Context, var arrSlider: ArrayList<Intro>) : PagerAdapter() {
    var inflater: LayoutInflater? = null
    override fun getCount(): Int {
        return arrSlider.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object` as LinearLayout
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val imgPreview: ImageView
        var txtContent: TextView
        val txtTitle: TextView
        var txtTitle1: TextView
        inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val itemview = inflater!!.inflate(R.layout.itemview_intro, container, false)
        imgPreview = itemview.findViewById(R.id.imgPreview)
        txtTitle = itemview.findViewById(R.id.txtTitle)
        imgPreview.setImageDrawable(arrSlider[position].img)
        txtTitle.text = arrSlider[position].title

        //add item.xml to viewpager
        (container as ViewPager).addView(itemview)
        return itemview
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        // Remove viewpager_item.xml from ViewPager
        (container as ViewPager).removeView(`object` as LinearLayout)
    } /*@Override
    public float getPageWidth(int position) {
        return .20f;   //it is used for set page widht of view pager
    }*/
}