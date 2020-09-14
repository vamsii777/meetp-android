package download.crossally.apps.hizkiyyah.explore;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.adapters.RecyclerSwipeAdapter;
import com.daimajia.swipe.implments.SwipeItemRecyclerMangerImpl;
import com.daimajia.swipe.util.Attributes;

import org.jetbrains.annotations.NotNull;

import download.crossally.apps.hizkiyyah.R;
import download.crossally.apps.hizkiyyah.bean.MeetingHistory;
import download.crossally.apps.hizkiyyah.bean.VideoList;
import download.crossally.apps.hizkiyyah.maxloghistory.MeetingHistoryAdapter;
import download.crossally.apps.hizkiyyah.utils.AppConstants;
import download.crossally.apps.hizkiyyah.utils.SharedObjects;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ExploreAdapter extends RecyclerView.Adapter<ExploreAdapter.ViewHolder> {

    ArrayList<VideoList> list;
    Context context;
    ExploreAdapter.OnItemClickListener onItemClickListener;

    public ExploreAdapter(@NotNull ArrayList<VideoList> arrExplore, @NotNull Context context) {
        this.list = arrExplore;
        this.context = context;
    }


    public void setOnItemClickListener(ExploreAdapter.OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }


    @Override
    public ExploreAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_videolist, parent, false);
        ExploreAdapter.ViewHolder holder = new ExploreAdapter.ViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(final ExploreAdapter.ViewHolder holder, final int position) {

        final VideoList bean = list.get(position);

        if (!TextUtils.isEmpty(bean.getTitle())) {
            holder.txtName.setText(bean.getTitle());
        }else{
            holder.txtName.setText("");
        }

        if (!TextUtils.isEmpty(bean.getDescription())) {
            holder.txtDesc.setText(bean.getDescription());
        }else{
            holder.txtDesc.setText("");
        }


        holder.cardV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClickListener(position, list.get(position));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


    public interface OnItemClickListener {
        void onItemClickListener(int position, VideoList bean);
        void onDeleteClickListener(int position, VideoList bean);
        void onJoinClickListener(int position, VideoList bean);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.txtName) TextView txtName;
        @BindView(R.id.txtDesc) TextView txtDesc;
        @BindView(R.id.itemVimg) AppCompatImageView itemVimgg;
        @BindView(R.id.cardv) TextView cardV;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}



