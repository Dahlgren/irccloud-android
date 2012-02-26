package org.vatvit.irccloudandroid;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.vatvit.irccloud.Channel;
import org.vatvit.irccloud.Client;
import org.vatvit.irccloud.Message;
import org.vatvit.irccloud.Server;
import org.vatvit.irccloud.events.ChannelListener;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ChannelActivity extends Activity {
	private static final String TAG = "IRCCloudChannelActivity";
	private Server server;
	private Channel channel;
	private MessageAdapter adapter;
	private ChannelListener chanListener;
	private Client client;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.channel);
		IRCCloudApplication app = ((IRCCloudApplication) getApplicationContext());
		client = app.getClient();
		Bundle extras = getIntent().getExtras();

		long cid = extras.getLong("cid");
		for (Server serv : client.getServers()) {
			if (serv.getCid() == cid) {
				server = serv;
				break;
			}
		}
		if (server == null) {
			Toast.makeText(app.getApplicationContext(),
					"Server was not found. This should not happen",
					Toast.LENGTH_SHORT).show();
			return;
		}
		Log.d(TAG, "Server found from channel "+server);
		
		String chanName = extras.getString("chan");
		for (Channel chan : server.getChannels()) {
			if (chan.getName().equalsIgnoreCase(chanName)) {
				channel = chan;
				break;
			}
		}
		if (channel == null) {
			Toast.makeText(app.getApplicationContext(),
					"Channel was not found. This should not happen",
					Toast.LENGTH_SHORT).show();
			return;
		}
		Log.d(TAG, "Channel found "+channel);
		

		setTitle(channel.getName()+(channel.getTopic()!=null && !channel.getTopic().equalsIgnoreCase("null")?" - "+channel.getTopic()+" - by "+channel.getTopicAuthor():""));
	
		
		ListView messages = (ListView) findViewById(R.id.messages);
		adapter = new MessageAdapter(this, R.layout.message_item,
				channel.getMessages());
		messages.setAdapter(adapter);


		final Handler hRefresh = new Handler(){

			@Override
			public void handleMessage(android.os.Message msg) {
				adapter.notifyDataSetChanged();
			}
			
		};

		
		chanListener = new ChannelListener(){
			@Override
			public void newMessage(Message message) {
				//Log.d(TAG, "New message "+message.getMsg()+" message count is "+channel.getMessages().size());
				hRefresh.sendEmptyMessage(0);
			}
		};
		
		
		final Button mSendButton = (Button) findViewById(R.id.button_send);
		final EditText message = (EditText) findViewById(R.id.send_message);
		
		mSendButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Send a message using content of the edit text widget
				EditText view = (EditText) findViewById(R.id.send_message);
				String message = view.getText().toString();
				view.setText("");
				channel.sendMessage(message);
			}
		});

		message.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN && 
					keyCode == KeyEvent.KEYCODE_ENTER) {
					mSendButton.performClick();
					return true;
				}
				return false;
			}
		});
	}
	
	public void onStop() {
		super.onStop();
		if(channel != null) {
			channel.removeChannelListener(chanListener);
			Log.d(TAG, "Removed listener. Channel has now "+channel.getChannelListeners().size()+" listeners");
		}
	}
	
	public void onResume() {
		super.onResume();
		if(channel != null) {
			if(channel.getChannelListeners().indexOf(chanListener) == -1) {
				channel.addChannelListener(chanListener);
				Log.d(TAG, "Added listener back");
			}
		}
	}
	
	public class MessageAdapter extends ArrayAdapter<Message> {
		private ArrayList<Message> messages;

		public MessageAdapter(Context context, int textViewResourceId,
				ArrayList<Message> messages) {
			super(context, textViewResourceId, messages);
			this.messages = messages;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater mInflater = getLayoutInflater();
			View row;

			if (null == convertView) {
				row = mInflater.inflate(R.layout.message_item, null);
			} else {
				row = convertView;
			}
			Message message = messages.get(position);
			if (message != null) {
				TextView by = (TextView) row.findViewById(R.id.by);
				TextView time = (TextView) row.findViewById(R.id.time);
				TextView msg = (TextView) row.findViewById(R.id.message);
				if(by != null) by.setText("<"+message.getFrom()+">");
				if(msg != null) msg.setText(message.getMsg());
				Date date = message.getDateTime();
				String hour_min = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
				if(time != null) time.setText(hour_min);
			}
			return row;
		}
	}

}
