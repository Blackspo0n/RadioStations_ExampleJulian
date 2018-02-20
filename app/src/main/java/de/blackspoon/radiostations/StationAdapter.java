package de.blackspoon.radiostations;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import de.blackspoon.radiostations.task.DownloadImageTask;

/**
 * Created by mario on 11.11.17.
 */

public class StationAdapter extends RecyclerView.Adapter<StationAdapter.StationViewHolder> {

    private List<StationInfo> stationList;
    private View.OnClickListener OnClickListener;
    private Context context;
    // Provide a suitable constructor (depends on the kind of dataset)
    protected StationAdapter(Context context, List<StationInfo> sI) {
        stationList = sI;
        this.context = context;

    }
    @Override
    public StationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        View v = inflater.inflate(R.layout.cardview, parent, false);

        v.setOnClickListener(OnClickListener);
        return new StationViewHolder(v);
    }

    @Override
    public void onBindViewHolder(StationViewHolder holder, final int position) {
        StationInfo si = stationList.get(position);

        holder.vName.setText(si.name);
        new DownloadImageTask(holder.vStationImage).execute(si.stationImageURL.toString());

        holder.vStationImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StationInfo item = stationList.get(position);
                Toast.makeText(context, item.name, Toast.LENGTH_LONG).show();

                // Wenn der Player noch läuft wird der aktualle Buffervorgang angehalten
                ((StationActivity)context).stopPlayer();
                ((StationActivity)context).startMediaPlayer(item);
            }
        });

        holder.vOverflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popup = new PopupMenu(context, view);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.menu_station_entry, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        StationInfo item = stationList.get(position);

                        switch(menuItem.getItemId()) {
                            case R.id.stationParameter:

                                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                builder.setMessage(item.toString()).setTitle("Stations Parameter");
                                AlertDialog dialog = builder.create();

                                dialog.show();
                                break;
                            case R.id.stationWebsite:
                                Intent intent = new Intent((StationActivity)context, StreamWebsite.class);
                                intent.putExtra("station",item);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                                Toast.makeText(context, "Öffne " + item.stationWebsite.toString(), Toast.LENGTH_SHORT).show();

                                context.startActivity(intent);
                                break;
                        }
                        // 1. Instantiate an AlertDialog.Builder with its constructor

                        return true;
                    }
                });
                popup.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return stationList.size();
    }

    class StationViewHolder extends RecyclerView.ViewHolder {
        TextView vName;
        ImageView vStationImage;
        ImageView vOverflow;

        StationViewHolder(View itemView) {
            super(itemView);

            vName = itemView.findViewById(R.id.title);
            vStationImage = itemView.findViewById(R.id.thumbnail);
            vOverflow = itemView.findViewById(R.id.overflow);
        }
    }
}
