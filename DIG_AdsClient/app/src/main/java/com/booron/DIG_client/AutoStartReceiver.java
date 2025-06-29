package com.booron.DIG_AdsClient; // Убедитесь, что здесь ваш ТОЧНЫЙ package name

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AutoStartReceiver extends BroadcastReceiver {

    private static final String TAG = "AutoStartReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Проверяем, что событие, которое мы поймали, это BOOT_COMPLETED
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Received BOOT_COMPLETED. Starting MainActivity.");

            // Создаем Intent для запуска MainActivity
            Intent i = new Intent(context, MainActivity.class);

            // Обязательно добавьте этот флаг для запуска Activity из BroadcastReceiver
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(i);
        }
    }
}