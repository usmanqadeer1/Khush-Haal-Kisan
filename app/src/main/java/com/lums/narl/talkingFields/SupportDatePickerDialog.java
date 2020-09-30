package com.lums.narl.talkingFields;


import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.StyleRes;
import android.support.v7.view.ContextThemeWrapper;

/**
 * Support {@link DatePickerDialog} for working around Samsung 5 {@link java.util.IllegalFormatConversionException} bug.
 * <p>
 * > Fatal Exception: java.util.IllegalFormatConversionException: %d can't format java.lang.String arguments
 * <p>
 * Created by Tobias Schürg
 * Based on http://stackoverflow.com/a/31855744/570168
 */

public class SupportDatePickerDialog extends DatePickerDialog {

    @RequiresApi(api = Build.VERSION_CODES.N)
    public SupportDatePickerDialog(@NonNull Context context) {
        super(fixContext(context));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public SupportDatePickerDialog(@NonNull Context context, @StyleRes int themeResId) {
        super(fixContext(context), themeResId);
    }

    public SupportDatePickerDialog(@NonNull Context context, @Nullable OnDateSetListener listener, int year, int month, int dayOfMonth) {
        super(fixContext(context), listener, year, month, dayOfMonth);
    }

    public SupportDatePickerDialog(@NonNull Context context, @StyleRes int themeResId, @Nullable OnDateSetListener listener, int year, int monthOfYear, int dayOfMonth) {
        super(fixContext(context), themeResId, listener, year, monthOfYear, dayOfMonth);
    }

    /**
     * Wraps the {@link Context} to use the holo theme to avoid stupid bug on Samsung devices.
     */
    private static Context fixContext(Context context) {
        if (isBrokenSamsungDevice()) {
            return new ContextThemeWrapper(context, android.R.style.Theme_Holo_Light_Dialog);
        } else {
            return context;
        }
    }

    /**
     * Affected devices:
     * - Samsung 5.0
     * - Samsung 5.1
     *
     * @return true if device is affected by this bug.
     */
    private static boolean isBrokenSamsungDevice() {
        return (Build.MANUFACTURER.equalsIgnoreCase("samsung")
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1);
    }

}