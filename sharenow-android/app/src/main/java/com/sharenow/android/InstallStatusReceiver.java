package com.sharenow.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.widget.Toast;

public class InstallStatusReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
        if (status == PackageInstaller.STATUS_SUCCESS) {
            Toast.makeText(context, "نصب برنامه با موفقیت انجام شد.", Toast.LENGTH_LONG).show();
        } else if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirmation = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirmation != null) {
                confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(confirmation);
            } else {
                Toast.makeText(context, "برای تکمیل نصب، پیام سیستم را تأیید کنید.", Toast.LENGTH_LONG).show();
            }
        } else {
            String detail = message == null ? "کد خطا: " + status : message;
            Toast.makeText(context, "نصب انجام نشد: " + detail, Toast.LENGTH_LONG).show();
        }
    }
}
