package com.samcodes.notifications;

import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.R.dimen;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;
import android.view.Window;
import android.util.Log;
import com.samcodes.notifications.Common;
import org.haxe.extension.Extension;

public class PresenterReceiver extends BroadcastReceiver {

    private static NotificationManager notificationManager;
    private static NotificationCompat.Builder builder;
    public static final String NOTIFICATION_CHANNEL_ID = "10001";

	@Override
	public void onReceive(Context context, Intent intent) {
		if(context == null || intent == null) {
			Log.i(Common.TAG, "Received notification presentation broadcast with null context or intent");
			return;
		}
		String action = intent.getAction();
		if(action == null) {
			Log.i(Common.TAG, "Received notification presentation broadcast with null action");
			return;
		}
		presentNotification(context, action); // Everything should be for presenting local device notifications
        Log.i(Common.TAG, "presenter onReceive");
	}
	
	private static void presentNotification(Context context, String action) {
		SharedPreferences prefs = context.getSharedPreferences(action, Context.MODE_PRIVATE);
		if(prefs == null) {
			Log.i(Common.TAG, "Failed to read notification preference data");
			return;
		}
		
		int slot = prefs.getInt(Common.SLOT_TAG, -1);
		if(slot == -1) {
			Log.i(Common.TAG, "Failed to read notification slot id");
			return;
		}
		String titleText = prefs.getString(Common.TITLE_TEXT_TAG, "");
		String subtitleText = prefs.getString(Common.SUBTITLE_TEXT_TAG, "");
		String messageBodyText = prefs.getString(Common.MESSAGE_BODY_TEXT_TAG, "");
		String tickerText = prefs.getString(Common.TICKER_TEXT_TAG, "");
		Boolean ongoing = prefs.getBoolean(Common.ONGOING_TAG, false);
		Boolean incrementBadgeCount = prefs.getBoolean(Common.INCREMENT_BADGE_COUNT_TAG, false);
		String largeIconName = prefs.getString(Common.LARGE_ICON_NAME_TAG, "");
		String smallIconName = prefs.getString(Common.SMALL_ICON_NAME_TAG, "");
		String channelId = prefs.getString(Common.CHANNEL_ID_TAG, "pre_android_oreo_notifications");
		String channelName = prefs.getString(Common.CHANNEL_NAME_TAG, "Pre-Android Oreo Notifications");
		String channelDescription = prefs.getString(Common.CHANNEL_DESCRIPTION_TAG, "Notifications");
		int channelImportance = prefs.getInt(Common.CHANNEL_IMPORTANCE_TAG, NotificationManager.IMPORTANCE_DEFAULT);
		
		Common.erasePreference(context, slot);
		
		if(incrementBadgeCount) {
			Common.setApplicationIconBadgeNumber(context, Common.getApplicationIconBadgeNumber(context) + 1);
		}
		sendNotification(context, slot, titleText, subtitleText, messageBodyText, tickerText, ongoing, smallIconName, largeIconName, channelId, channelName, channelDescription, channelImportance);
	}
	
	// Actually send the local notification to the device
	private static void sendNotification(Context context, int slot, String titleText, String subtitleText, String messageBodyText, String tickerText, Boolean ongoing, String smallIconName, String largeIconName, String channelId, String channelName, String channelDescription, int channelImportance) {
		Context applicationContext = context.getApplicationContext();
		if(applicationContext == null) {
			Log.i(Common.TAG, "Failed to get application context");
			return;
		}
		
		// Get application icons
        Bitmap largeIcon = null;
        int largeIconId = 0;
		int smallIconId = R.drawable.ic_action_white;

		try {
			PackageManager pm = context.getPackageManager();
			if(pm != null) {
				ApplicationInfo ai = pm.getApplicationInfo(Common.getPackageName(), 0);
				if(ai != null) {
					largeIconId = ai.icon;
				}
			}
		} catch (NameNotFoundException e) {
			Log.i(Common.TAG, "Failed to get application icon, falling back to default");
			largeIconId = android.R.drawable.ic_dialog_info;
		}
        // Get large application icon
        largeIcon = BitmapFactory.decodeResource(applicationContext.getResources(), largeIconId);

		// Android 26+?
        if (largeIcon == null)
        {
            Drawable iconDr = null;
            try {
                iconDr = context.getPackageManager().getApplicationIcon(Common.getPackageName());
            } catch (PackageManager.NameNotFoundException e)
            {
                Log.i(Common.TAG, "Failed to get application icon, falling back to default");
            }
            if (iconDr != null)
            {
                largeIcon = getBitmapFromDrawable(iconDr);
            }
        }
        //
		
		// Scale it down if it's too big
		if(largeIcon != null && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			int width = android.R.dimen.notification_large_icon_width > 0 ? android.R.dimen.notification_large_icon_width : 150;
			int height = android.R.dimen.notification_large_icon_height > 0 ? android.R.dimen.notification_large_icon_height : 150;
			if(largeIcon.getWidth() > width || largeIcon.getHeight() > height) {
				largeIcon = Bitmap.createScaledBitmap(largeIcon, width, height, false);
			}
		}
		
		// Launch or open application on notification tap
		Intent intent = null;
		try {
			PackageManager pm = context.getPackageManager();
			if(pm != null) {
				String packageName = Common.getPackageName();
				intent = pm.getLaunchIntentForPackage(packageName);
				intent.addCategory(Intent.CATEGORY_LAUNCHER); // Should already be set, but just in case
			}
		} catch (Exception e) {
			Log.i(Common.TAG, "Failed to get application launch intent");
		}
		
		if(intent == null) {
			Log.i(Common.TAG, "Falling back to empty intent");
			intent = new Intent();
		}
		
		PendingIntent pendingIntent = PendingIntent.getActivity(applicationContext, slot, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder = new NotificationCompat.Builder(applicationContext);
		builder.setAutoCancel(true);
		builder.setContentTitle(titleText);
		builder.setSubText(subtitleText);
		builder.setContentText(messageBodyText);
		builder.setTicker(tickerText);

		if(largeIcon != null) {
			builder.setLargeIcon(largeIcon);
		}
		builder.setSmallIcon(smallIconId);
		builder.setContentIntent(pendingIntent);
		builder.setOngoing(ongoing);
		builder.setWhen(System.currentTimeMillis());
		builder.setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_LIGHTS);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			builder.setChannelId(channelId);
		}
		
		builder.build();
		
		notificationManager = ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
        {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "NOTIFICATION_CHANNEL_NAME", importance);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.GREEN);
            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(new long[]{500, 200});
            if(notificationManager != null) {
                builder.setChannelId(NOTIFICATION_CHANNEL_ID);
                Log.i(Common.TAG, "Set channel id to " + NOTIFICATION_CHANNEL_ID);
            }
            notificationManager.createNotificationChannel(notificationChannel);
        }

		if(notificationManager != null) {
			notificationManager.notify(slot, builder.build());
            Log.i(Common.TAG, "NotificationManager notify slot " + slot);
		}
	}

    @NonNull
    private static Bitmap getBitmapFromDrawable(@NonNull Drawable drawable) {
        final Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bmp;
    }
}
