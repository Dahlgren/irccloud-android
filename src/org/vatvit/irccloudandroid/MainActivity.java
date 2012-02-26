package org.vatvit.irccloudandroid;

import org.vatvit.irccloud.Client;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final String PREF_EMAIL = "email";
	private static final String PREF_PASSWORD = "password";
	private static final String TAG = "IRCCloudMainActivity";
	private static final int ONGOING_NOTIFICATION_ID = 100;
	private Client client;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final IRCCloudApplication app = ((IRCCloudApplication) getApplicationContext());
		client = app.getClient();

		setContentView(R.layout.main);

		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		final Button loginButton = (Button) findViewById(R.id.loginButton);
		final EditText emailField = (EditText) findViewById(R.id.emailField);
		final EditText passwordField = (EditText) findViewById(R.id.passwordField);

		String email = pref.getString(PREF_EMAIL, "");
		String password = pref.getString(PREF_PASSWORD, "");

		loginButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(final View v) {
				if (client.isLoggedIn()) {
					showServers();
				} else {
					client.getServers().clear();
					(new Thread() {
						@Override
						public void run() {
							Editor e = pref.edit();
							String email = emailField.getText().toString().trim();
							String password = passwordField.getText().toString().trim();
							e.putString(PREF_EMAIL, email);

							if (client.login(email, password)) {
								Log.d(TAG, "Login successful.");
								e.putString(PREF_PASSWORD, password);
								showOngoingNotification(email);
								showServers();
							} else {
								Log.d(TAG, "Login failed.");
								e.putString("password", "");
								Toast.makeText(app.getApplicationContext(),
										"Login failed", Toast.LENGTH_SHORT).show();
							}
							e.commit();
						}
					}).run();
				}

			}
		});

		emailField.setText(email);
		passwordField.setText(password);

		if (!(email.equals("") || password.equals(""))) {
			loginButton.performClick();
		}
	}

	private void showServers() {
		Intent serversIntent = new Intent(this.getBaseContext(), ServersActivity.class);
		startActivity(serversIntent);
	}

	private void showOngoingNotification(String email) {
		String notifyText = String.format(getResources().getString(R.string.ongoing), email);
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		int icon = R.drawable.ic_stat_ongoing;
		CharSequence tickerText = notifyText;
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		Context context = getApplicationContext();
		CharSequence contentTitle = getResources().getString(R.string.app_name);
		CharSequence contentText = notifyText;
		Intent notificationIntent = new Intent(this, ServersActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(
				this,
				0,
				notificationIntent,
				PendingIntent.FLAG_UPDATE_CURRENT | Notification.FLAG_AUTO_CANCEL
		);

		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);


		mNotificationManager.notify(ONGOING_NOTIFICATION_ID, notification);
	}

}