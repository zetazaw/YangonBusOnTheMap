package net.konyan.yangonbusonthemap;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cocoahero.android.geojson.Feature;

import org.json.JSONException;

import java.util.List;

/**
 * Created by zeta on 1/16/17.
 */

public class BusRecyclerAdapter extends RecyclerView.Adapter<BusRecyclerAdapter.BusHolder>{

    Context context;
    List<Feature> featureList;
    BusSelectListener listener;

    public BusRecyclerAdapter(Context context, List<Feature> featureList, BusSelectListener listener){
        this.context = context;
        this.featureList = featureList;
        this.listener = listener;
    }

    @Override
    public BusHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new BusHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bus, parent, false));
    }

    @Override
    public void onBindViewHolder(BusHolder holder, int position) {
        final Feature feature = featureList.get(position);
        try {
            String bus = feature.getProperties().getString("service_name");
            String busColor = feature.getProperties().getString("color").trim();
            holder.itemView.setBackgroundColor(Color.parseColor(busColor));
            holder.tvBusName.setText(bus);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onBusClick(feature);
            }
        });
    }

    @Override
    public int getItemCount() {
        return featureList == null ? 0 : featureList.size();
    }

    class BusHolder extends RecyclerView.ViewHolder{
        TextView tvBusName;
        public BusHolder(View itemView) {
            super(itemView);
            tvBusName = (TextView) itemView.findViewById(R.id.tv_item_bus_name);
        }
    }
}
