package com.example.citialertsph.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.citialertsph.R;
import com.example.citialertsph.models.EvacuationCenter;
import java.util.List;

public class LocationsAdapter extends RecyclerView.Adapter<LocationsAdapter.LocationViewHolder> {

    private List<EvacuationCenter> locations;
    private Context context;
    private OnLocationClickListener listener;

    public interface OnLocationClickListener {
        void onLocationClick(EvacuationCenter location);
        void onDirectionsClick(EvacuationCenter location);
    }

    public LocationsAdapter(Context context, List<EvacuationCenter> locations) {
        this.context = context;
        this.locations = locations;
    }

    public void setOnLocationClickListener(OnLocationClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_location, parent, false);
        return new LocationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        EvacuationCenter location = locations.get(position);

        holder.tvName.setText(location.getName());
        holder.tvAddress.setText(location.getAddress());
        holder.tvCapacity.setText(location.getCapacityText());
        holder.tvContact.setText(location.getContactText());

        // Show/hide description
        if (location.hasDescription()) {
            holder.tvDescription.setVisibility(View.VISIBLE);
            holder.tvDescription.setText(location.getDescription());
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }

        // Set coordinates text
        holder.tvCoordinates.setText(String.format("%.6f, %.6f",
            location.getLatitude(), location.getLongitude()));

        // Click listeners
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLocationClick(location);
            }
        });

        holder.tvDirections.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDirectionsClick(location);
            }
        });
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    public void updateLocations(List<EvacuationCenter> newLocations) {
        this.locations = newLocations;
        notifyDataSetChanged();
    }

    static class LocationViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvName, tvAddress, tvDescription, tvCapacity, tvContact, tvCoordinates, tvDirections;

        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvName = itemView.findViewById(R.id.tvName);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvCapacity = itemView.findViewById(R.id.tvCapacity);
            tvContact = itemView.findViewById(R.id.tvContact);
            tvCoordinates = itemView.findViewById(R.id.tvCoordinates);
            tvDirections = itemView.findViewById(R.id.tvDirections);
        }
    }
}
