package com.example.citialertsph;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.citialertsph.adapters.EmergencyListAdapter;
import com.example.citialertsph.models.EmergencyRequest;
import com.example.citialertsph.utils.ApiClient;
import com.example.citialertsph.utils.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class EmergencyListFragment extends Fragment implements EmergencyListAdapter.OnEmergencyActionListener {
    private static final String ARG_IS_USER_MODE = "is_user_mode";

    public static EmergencyListFragment newInstance(boolean isUserMode) {
        EmergencyListFragment fragment = new EmergencyListFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_USER_MODE, isUserMode);
        fragment.setArguments(args);
        return fragment;
    }

    private RecyclerView recyclerView;
    private TextView emptyView;
    private EmergencyListAdapter adapter;
    private ApiClient apiClient;
    private OnEmergencySelectedListener listener;
    private SessionManager sessionManager;
    private boolean isUserMode;

    public interface OnEmergencySelectedListener {
        void onEmergencySelected(EmergencyRequest request);
        void onEmergencyResponse(EmergencyRequest request);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_emergency_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.emergencyRecyclerView);
        emptyView = view.findViewById(R.id.emptyView);
        sessionManager = new SessionManager(requireContext());
        apiClient = new ApiClient();

        // Get user mode from arguments
        isUserMode = getArguments() != null && getArguments().getBoolean(ARG_IS_USER_MODE, true);
        adapter = new EmergencyListAdapter(this, isUserMode);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        fetchEmergencyRequests();
    }

    private void fetchEmergencyRequests() {
        String endpoint = isUserMode ?
            "get_emergency_requests.php?user_id=" + sessionManager.getUserId() :
            "get_emergency_requests.php";

        apiClient.getRequest(endpoint, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Failed to load emergency requests", Toast.LENGTH_SHORT).show()
                    );
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    return;
                }
                ResponseBody body = response.body();
                if (body == null) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Empty response from server", Toast.LENGTH_SHORT).show()
                        );
                    }
                    return;
                }
                String responseData = body.string();
                JsonObject jsonResponse = ApiClient.parseResponse(responseData);

                if (jsonResponse.has("requests")) {
                    JsonArray requestsArray = jsonResponse.getAsJsonArray("requests");
                    Type listType = new TypeToken<ArrayList<EmergencyRequest>>(){}.getType();
                    List<EmergencyRequest> requests = new Gson().fromJson(requestsArray, listType);

                    // Set current user ID for each request
                    for (EmergencyRequest request : requests) {
                        request.setCurrentUserId(sessionManager.getUserId());
                    }

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            adapter.updateData(requests);
                            updateEmptyView(requests.isEmpty());
                        });
                    }
                }
            }
        });
    }

    private void updateEmptyView(boolean isEmpty) {
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onRespond(EmergencyRequest request) {
        // For moderators, show the details dialog so they can confirm/respond there
        if (listener != null) {
            listener.onEmergencySelected(request);
        }
    }

    @Override
    public void onComplete(EmergencyRequest request) {
        // Use the same simple approach as emergency creation - delegate to EmergencyActivity
        if (getActivity() instanceof EmergencyActivity) {
            ((EmergencyActivity) getActivity()).completeEmergencyRequest(request);
        }
    }

    @Override
    public void onItemClick(EmergencyRequest request) {
        if (listener != null) {
            listener.onEmergencySelected(request);
        }
    }

    public void setOnEmergencySelectedListener(OnEmergencySelectedListener listener) {
        this.listener = listener;
    }

    // Call this method to refresh the list
    public void refreshList() {
        fetchEmergencyRequests();
    }
}
