package com.radio.codec2talkie.storage.log;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.storage.station.StationItemAdapter;
import com.radio.codec2talkie.storage.position.PositionItemViewModel;
import com.radio.codec2talkie.storage.station.StationItemViewModel;

import java.util.List;

public class LogItemFragment extends Fragment implements MenuProvider {
    /** @noinspection unused*/
    private static final String TAG = LogItemFragment.class.getSimpleName();

    private static final String ARG_DATA = "arg_data";

    private String _stationName;
    private LogItemViewModel _logItemViewModel;
    private PositionItemViewModel _positionItemViewModel;
    private StationItemViewModel _stationItemViewModel;

    private LiveData<List<LogItem>> _logItemLiveData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            _stationName = getArguments().getString(ARG_DATA);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_log_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requireActivity().addMenuProvider(this, getViewLifecycleOwner());

        // view models
        _logItemViewModel = new ViewModelProvider(this).get(LogItemViewModel.class);
        _positionItemViewModel = new ViewModelProvider(this).get(PositionItemViewModel.class);
        _stationItemViewModel = new ViewModelProvider(this).get(StationItemViewModel.class);

        // log items
        RecyclerView logItemRecyclerView = view.findViewById(R.id.log_item_recyclerview);
        logItemRecyclerView.setHasFixedSize(true);

        // log lines list adapter
        final LogItemAdapter adapter = new LogItemAdapter(new LogItemAdapter.LogItemDiff(), requireActivity(), _stationName == null);
        logItemRecyclerView.setAdapter(adapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireActivity());
        linearLayoutManager.setReverseLayout(true);
        logItemRecyclerView.setLayoutManager(linearLayoutManager);
        logItemRecyclerView.addItemDecoration(new DividerItemDecoration(logItemRecyclerView.getContext(), DividerItemDecoration.VERTICAL));

        // stations
        RecyclerView stationsRecyclerView = view.findViewById(R.id.log_item_group_recyclerview);
        stationsRecyclerView.setHasFixedSize(true);

        // stations adapter
        final StationItemAdapter stationsAdapter = getStationItemAdapter(adapter);
        stationsRecyclerView.setAdapter(stationsAdapter);
        LinearLayoutManager linearLayoutManagerStations = new LinearLayoutManager(requireActivity());
        stationsRecyclerView.setLayoutManager(linearLayoutManagerStations);
        stationsRecyclerView.addItemDecoration(new DividerItemDecoration(stationsRecyclerView.getContext(), DividerItemDecoration.VERTICAL));

        _stationItemViewModel.getAllStationItems(false).observe(requireActivity(), stationsAdapter::submitList);

        // launch with filter if station name is provided
        if (_stationName == null) {
            stationsRecyclerView.setVisibility(View.GONE);
            view.findViewById(R.id.log_item_textview).setVisibility(View.GONE);
            view.findViewById(R.id.log_item_group_textview).setVisibility(View.GONE);
            _logItemLiveData = _logItemViewModel.getAllData();
            _logItemLiveData.observe(requireActivity(), adapter::submitList);
        } else {
            _logItemLiveData = _logItemViewModel.getData(_stationName);
            _logItemLiveData.observe(requireActivity(), adapter::submitList);
        }

        // register live scroll
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                int msgCount = adapter.getItemCount();
                int lastVisiblePosition = linearLayoutManager.findLastCompletelyVisibleItemPosition();
                //Log.i(TAG, " " + positionStart + " " + itemCount + " " + lastVisiblePosition + " " + msgCount);
                if (lastVisiblePosition == RecyclerView.NO_POSITION || (positionStart == msgCount - itemCount && lastVisiblePosition == positionStart - 1)) {
                    logItemRecyclerView.scrollToPosition(msgCount - 1);
                }
            }
        });
    }

    @NonNull
    private StationItemAdapter getStationItemAdapter(LogItemAdapter adapter) {
        final StationItemAdapter stationsAdapter = new StationItemAdapter(new StationItemAdapter.StationItemDiff());
        stationsAdapter.setClickListener(v -> {
            TextView itemView = v.findViewById(R.id.log_view_group_item_title);
            _logItemLiveData.removeObservers(this);
            _stationName = itemView.getText().toString();
            _logItemLiveData = _logItemViewModel.getData(_stationName);
            _logItemLiveData.observe(requireActivity(), adapter::submitList);
        });
        return stationsAdapter;
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.log_view_menu, menu);
    }

    @Override
    public void onPrepareMenu(@NonNull Menu menu) {
        if (_stationName != null) {
            menu.findItem(R.id.log_view_menu_stations).setVisible(false);
        }
    }

    @Override
    public boolean onMenuItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.log_view_menu_clear_all) {
            deleteLogItems(-1);
            return true;
        }  else if (itemId == R.id.log_view_menu_clear_1h) {
            deleteLogItems(1);
            return true;
        }  else if (itemId == R.id.log_view_menu_clear_12h) {
            deleteLogItems(12);
            return true;
        }  else if (itemId == R.id.log_view_menu_clear_1d) {
            deleteLogItems(24);
            return true;
        }  else if (itemId == R.id.log_view_menu_clear_7d) {
            deleteLogItems(24*7);
            return true;
        } else if (itemId == R.id.log_view_menu_stations) {
            restartLogItemFragment(getString(R.string.log_view_station_history));
            return true;
        }
        return false;
    }

    public static LogItemFragment newInstance(String data) {
        LogItemFragment fragment = new LogItemFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DATA, data);
        fragment.setArguments(args);
        return fragment;
    }

    private void restartLogItemFragment(String newData) {
        LogItemFragment newFragment = newInstance(newData);
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentMain, newFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void deleteLogItems(int hours) {
        DialogInterface.OnClickListener deleteAllDialogClickListener = (dialog, which) -> {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                _logItemViewModel.deleteLogItems(_stationName, hours);
                _positionItemViewModel.deletePositionItems(_stationName, hours);
                _stationItemViewModel.deleteStationItems(_stationName, hours);
            }
        };
        String alertMessage = getString(R.string.log_item_activity_delete_all_title);
        if (hours != -1) {
            alertMessage = String.format(getString(R.string.log_item_activity_delete_hours_title), hours);
        }
        if (_stationName != null) {
            alertMessage = getString(R.string.log_item_activity_delete_group_title);
            alertMessage = String.format(alertMessage, _stationName);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setMessage(alertMessage)
                .setPositiveButton(getString(R.string.yes), deleteAllDialogClickListener)
                .setNegativeButton(getString(R.string.no), deleteAllDialogClickListener).show();
    }
}
