package com.android.settings.paranoid;

import android.content.Context;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import com.android.settings.hardware.DisplayColor;
import com.android.settings.hardware.DisplayGamma;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.util.ArrayList;

import org.cyanogenmod.hardware.AdaptiveBacklight;
import org.cyanogenmod.hardware.TapToWake;

public class DisplaySettingsAdvanced extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, OnPreferenceClickListener {
    private static final String TAG = "DisplaySettingsAdvanced";

    private static final String KEY_ADVANCED_DISPLAY_SETTINGS = "advanced_display_settings";
    private static final String KEY_SCREEN_COLOR_SETTINGS = "screencolor_settings";
    private static final String KEY_ADAPTIVE_BACKLIGHT = "adaptive_backlight";
    private static final String KEY_TAP_TO_WAKE = "double_tap_wake_gesture";
    private static final String KEY_WAKE_WHEN_PLUGGED_OR_UNPLUGGED = "wake_when_plugged_or_unplugged";
    private static final String KEY_SCREEN_GESTURE_SETTINGS = "touch_screen_gesture_settings";
    private static final String KEY_ADVANCED_SETTINGS = "oppo_advanced_settings";
    private static final String KEY_DISPLAY_CALIBRATION_CATEGORY = "display_calibration_category";
    private static final String KEY_DISPLAY_COLOR = "color_calibration";
    private static final String KEY_DISPLAY_GAMMA = "gamma_tuning";

    private PreferenceScreen mScreenColorSettings;

    private CheckBoxPreference mAdaptiveBacklight;
    private CheckBoxPreference mTapToWake;
    private CheckBoxPreference mWakeWhenPluggedOrUnplugged;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.display_settings_advanced);

        mScreenColorSettings = (PreferenceScreen) findPreference(KEY_SCREEN_COLOR_SETTINGS);
        if (!isPostProcessingSupported()) {
            getPreferenceScreen().removePreference(mScreenColorSettings);
        }

        mAdaptiveBacklight = (CheckBoxPreference) findPreference(KEY_ADAPTIVE_BACKLIGHT);
        if (!isAdaptiveBacklightSupported()) {
            getPreferenceScreen().removePreference(mAdaptiveBacklight);
            mAdaptiveBacklight = null;
        }

        mTapToWake = (CheckBoxPreference) findPreference(KEY_TAP_TO_WAKE);
        if (!isTapToWakeSupported()) {
            getPreferenceScreen().removePreference(mTapToWake);
            mTapToWake = null;
        }

        mWakeWhenPluggedOrUnplugged =
                (CheckBoxPreference) findPreference(KEY_WAKE_WHEN_PLUGGED_OR_UNPLUGGED);

        final PreferenceGroup calibrationCategory =
                (PreferenceGroup) findPreference(KEY_DISPLAY_CALIBRATION_CATEGORY);

        if (!DisplayColor.isSupported() && !DisplayGamma.isSupported()) {
			            getPreferenceScreen().removePreference(calibrationCategory);
        } else {
            if (!DisplayColor.isSupported()) {
                calibrationCategory.removePreference(findPreference(KEY_DISPLAY_COLOR));
            }
            if (!DisplayGamma.isSupported()) {
                calibrationCategory.removePreference(findPreference(KEY_DISPLAY_GAMMA));
            }
        }

        Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(getActivity(),
                getPreferenceScreen(), KEY_ADVANCED_DISPLAY_SETTINGS);

        //Gesture Settings
        Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(getActivity(),
                getPreferenceScreen(), KEY_SCREEN_GESTURE_SETTINGS);

        Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(getActivity(),
                getPreferenceScreen(), KEY_ADVANCED_SETTINGS);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mAdaptiveBacklight != null) {
            mAdaptiveBacklight.setChecked(AdaptiveBacklight.isEnabled());
        }

        if (mTapToWake != null) {
            mTapToWake.setChecked(TapToWake.isEnabled());
        }

        final ContentResolver resolver = getContentResolver();

        // Default value for wake-on-plug behavior from config.xml
        boolean wakeUpWhenPluggedOrUnpluggedConfig = getResources().getBoolean(
                com.android.internal.R.bool.config_unplugTurnsOnScreen);

        mWakeWhenPluggedOrUnplugged.setChecked(Settings.Global.getInt(resolver,
                Settings.Global.WAKE_WHEN_PLUGGED_OR_UNPLUGGED,
                (wakeUpWhenPluggedOrUnpluggedConfig ? 1 : 0)) == 1);
    }

    private boolean isPostProcessingSupported() {
        boolean ret = true;
        final PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo("com.qualcomm.display", PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
            ret = false;
        }
        return ret;
    }

    private static boolean isAdaptiveBacklightSupported() {
        try {
            return AdaptiveBacklight.isSupported();
        } catch (NoClassDefFoundError e) {
            // Hardware abstraction framework not installed
            return false;
        }
    }

    private static boolean isTapToWakeSupported() {
        try {
            return TapToWake.isSupported();
        } catch (NoClassDefFoundError e) {
            // Hardware abstraction framework not installed
            return false;
        }
    }

    /**
     * Restore the properties associated with this preference on boot
     * @param ctx A valid context
     */
    public static void restore(Context ctx) {
        if (isAdaptiveBacklightSupported()) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
            final boolean enabled = prefs.getBoolean(KEY_ADAPTIVE_BACKLIGHT, true);
            if (!AdaptiveBacklight.setEnabled(enabled)) {
                Log.e(TAG, "Failed to restore adaptive backlight settings.");
            } else {
                Log.d(TAG, "Adaptive backlight settings restored.");
            }
        }
        if (isTapToWakeSupported()) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
            final boolean enabled = prefs.getBoolean(KEY_TAP_TO_WAKE, true);
            if (!TapToWake.setEnabled(enabled)) {
                Log.e(TAG, "Failed to restore tap-to-wake settings.");
            } else {
                Log.d(TAG, "Tap-to-wake settings restored.");
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mAdaptiveBacklight) {
            return AdaptiveBacklight.setEnabled(mAdaptiveBacklight.isChecked());
        } else if (preference == mTapToWake) {
            return TapToWake.setEnabled(mTapToWake.isChecked());
        } else if (preference == mWakeWhenPluggedOrUnplugged) {
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.WAKE_WHEN_PLUGGED_OR_UNPLUGGED,
                    mWakeWhenPluggedOrUnplugged.isChecked() ? 1 : 0);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

}
