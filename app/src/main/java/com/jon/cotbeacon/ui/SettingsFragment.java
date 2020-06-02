package com.jon.cotbeacon.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.jon.cotbeacon.BuildConfig;
import com.jon.cotbeacon.R;
import com.jon.cotbeacon.enums.Protocol;
import com.jon.cotbeacon.service.GpsService;
import com.jon.cotbeacon.utils.GenerateInt;
import com.jon.cotbeacon.utils.Key;
import com.jon.cotbeacon.utils.Notify;
import com.jon.cotbeacon.utils.OutputPreset;
import com.jon.cotbeacon.utils.PresetSqlHelper;

import org.apache.commons.collections4.ListUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pub.devrel.easypermissions.EasyPermissions;

public class SettingsFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceChangeListener,
        EasyPermissions.PermissionCallbacks {
    private SharedPreferences prefs;
    private PresetSqlHelper sqlHelper;

    private static final int GPS_PERMISSION_CODE = GenerateInt.next();

    static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    private static final Map<String, String> PREFS_REQUIRING_VALIDATION = new HashMap<String, String>() {{
        put(Key.CALLSIGN, "Should only contain alphanumeric characters");
    }};

    private static final String[] SEEKBARS = new String[]{
            Key.STALE_TIMER,
            Key.TRANSMISSION_PERIOD
    };

    @Override
    public void onCreatePreferences(Bundle savedState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);

        for (final String key : PREFS_REQUIRING_VALIDATION.keySet()) {
            Preference pref = findPreference(key);
            if (pref != null) pref.setOnPreferenceChangeListener(this);
        }
        for (final String key : SEEKBARS) {
            SeekBarPreference seekbar = findPreference(key);
            seekbar.setMin(1); /* I can't set the minimum in the XML for whatever reason, so here it is */
        }
        setPresetPreferenceListeners();
        sqlHelper = new PresetSqlHelper(requireContext());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle state) {
        super.onActivityCreated(state);
        prefs = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        toggleProtocolSettingVisibility();
        requestGpsPermission();
        updatePresetEntries(Protocol.UDP, Key.UDP_PRESETS);
        updatePresetEntries(Protocol.TCP, Key.TCP_PRESETS);
        Protocol newProtocol = Protocol.fromPrefs(prefs);
        insertPresetAddressAndPort(newProtocol == Protocol.TCP ? Key.TCP_PRESETS : Key.UDP_PRESETS);
    }

    @Override
    public void onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        sqlHelper.close();
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        switch (key) {
            case Key.TRANSMISSION_PROTOCOL:
                toggleProtocolSettingVisibility();
                Protocol newProtocol = Protocol.fromPrefs(prefs);
                insertPresetAddressAndPort(newProtocol == Protocol.TCP ? Key.TCP_PRESETS : Key.UDP_PRESETS);
                break;
            case Key.TCP_PRESETS:
            case Key.UDP_PRESETS:
                insertPresetAddressAndPort(key);
                break;
            case Key.NEW_PRESET_ADDED:
                updatePresetEntries(Protocol.UDP, Key.UDP_PRESETS);
                updatePresetEntries(Protocol.TCP, Key.TCP_PRESETS);
                break;
        }
    }

    private void insertPresetAddressAndPort(String key) {
        EditTextPreference addressPref = findPreference(Key.DEST_ADDRESS);
        EditTextPreference portPref = findPreference(Key.DEST_PORT);
        ListPreference presetPref = findPreference(key);
        if (addressPref != null && portPref != null && presetPref != null) {
            OutputPreset preset = OutputPreset.fromString(presetPref.getValue());
            if (preset != null) {
                addressPref.setText(preset.address);
                portPref.setText(Integer.toString(preset.port));
            } else {
                presetPref.setValue(null);
                addressPref.setText(null);
                portPref.setText(null);
            }
        }
    }

    private void requestGpsPermission() {
        if (!EasyPermissions.hasPermissions(requireContext(), GpsService.GPS_PERMISSION)) {
            EasyPermissions.requestPermissions(this, getString(R.string.permissionRationale), GPS_PERMISSION_CODE, GpsService.GPS_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        if (requestCode == GPS_PERMISSION_CODE) {
            Notify.green(requireView(), "GPS Permission successfully granted");
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (requestCode == GPS_PERMISSION_CODE) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), GpsService.GPS_PERMISSION[0])) {
                /* Permission has been permanently denied, so show a toast and open Android settings */
                Notify.toast(requireContext(), getString(R.string.permissionBegging));
                Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
                startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri));
            } else {
                /* Permission has been temporarily denied, so we can re-ask within the app */
                Notify.orange(requireView(), getString(R.string.permissionRationale));
            }
        }
    }

    private void toggleProtocolSettingVisibility() {
        boolean showUdpSettings = Protocol.fromPrefs(prefs) == Protocol.UDP;
        findPreference(Key.UDP_PRESETS).setVisible(showUdpSettings);
        findPreference(Key.TCP_PRESETS).setVisible(!showUdpSettings);
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        final String str = (String) newValue;
        boolean result = true;
        if (Key.CALLSIGN.equals(pref.getKey()) && !str.matches("\\w*?")) {
            /* alphanumeric characters only */
            Notify.red(requireView(), "Invalid input: " + str + ". " + PREFS_REQUIRING_VALIDATION.get(pref.getKey()));
            return false;
        }
        return true;
    }

    private void setPresetPreferenceListeners() {
        Preference addPreference = findPreference(Key.ADD_NEW_PRESET);
        if (addPreference != null) {
            addPreference.setOnPreferenceClickListener(clickedPref -> {
                NewPresetDialogCreator.show(requireContext(), requireView(), prefs, sqlHelper);
                return true;
            });
        }
        Preference deletePreference = findPreference(Key.DELETE_PRESETS);
        if (deletePreference != null) {
            deletePreference.setOnPreferenceClickListener(clickedPref -> {
                deletePresetDialog();
                return true;
            });
        }
    }

    private void updatePresetEntries(Protocol protocol, String key) {
        List<OutputPreset> defaults = (protocol == Protocol.TCP) ? OutputPreset.tcpDefaults() : OutputPreset.udpDefaults();
        List<OutputPreset> presets = ListUtils.union(defaults, sqlHelper.getAllPresets(protocol));
        List<String> entries = OutputPreset.getAliases(presets);
        List<String> entryValues = new ArrayList<>();
        for (OutputPreset preset : presets) {
            entryValues.add(preset.toString());
        }
        ListPreference preference = findPreference(key);
        if (preference != null) {
            String previousValue = preference.getValue();
            preference.setEntries(Arrays.copyOf(entries.toArray(), entries.toArray().length, String[].class));
            preference.setEntryValues(Arrays.copyOf(entryValues.toArray(), entryValues.toArray().length, String[].class));
            preference.setValue(previousValue);
        }
    }

    private void deletePresetDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Presets")
                .setMessage("Clear all custom output presets? The built-in defaults will still remain.")
                .setPositiveButton(android.R.string.ok, (dialog, buttonId) -> {
                    resetPresetPreference(Key.UDP_PRESETS, OutputPreset.udpDefaults());
                    resetPresetPreference(Key.TCP_PRESETS, OutputPreset.tcpDefaults());
                    if (PresetSqlHelper.deleteDatabase()) {
                        Notify.green(requireView(), "Successfully deleted presets");
                    } else {
                        Notify.red(requireView(), "Failed to delete presets");
                    }
                }).setNegativeButton(android.R.string.cancel, (dialog, buttonId) -> dialog.dismiss())
                .show();
    }

    private void resetPresetPreference(String prefKey, List<OutputPreset> defaults) {
        List<String> entries = OutputPreset.getAliases(defaults);
        List<String> values = new ArrayList<>();
        for (OutputPreset preset : defaults) {
            values.add(preset.toString());
        }
        ListPreference preference = findPreference(prefKey);
        if (preference != null) {
            preference.setEntries(Arrays.copyOf(entries.toArray(), entries.size(), String[].class));
            preference.setEntryValues(Arrays.copyOf(values.toArray(), values.size(), String[].class));
            preference.setValueIndex(0);
        }
    }
}
