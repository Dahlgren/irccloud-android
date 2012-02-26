package org.vatvit.irccloudandroid;

import org.vatvit.irccloud.Channel;
import org.vatvit.irccloud.Client;
import org.vatvit.irccloud.Server;
import org.vatvit.irccloud.events.ServersListener;

import android.app.ExpandableListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

public class ServerActivity extends ExpandableListActivity {
	private static final String TAG = "IRCCloudServerActivity";
	private ServerItemAdapter adapter;
	private Server server = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "Server activity oppened");
		super.onCreate(savedInstanceState);

		IRCCloudApplication app = ((IRCCloudApplication) getApplicationContext());
		Client client = app.getClient();
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
		Log.d(TAG, "Server found " + server);
		setTitle(server.getName() + " - " + server.getNick() + "@"
				+ server.getHostname());

		final Handler refresh = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				adapter.notifyDataSetChanged();
			}
		};

		adapter = new ServerItemAdapter();
		setListAdapter(adapter);

		client.addServerListener(new ServersListener() {

			@Override
			public void connectedToServer(Server server) {
				Log.d(TAG, "Server "+server.toString());
				refresh.sendEmptyMessage(0);
				

			}

			@Override
			public void disconnectedFromServer(Server server) {
				Log.d(TAG, "Server disconnect "+server.toString());
				refresh.sendEmptyMessage(0);
			}

			@Override
			public void update() {
				Log.d(TAG, "Server update");
				refresh.sendEmptyMessage(0);
			}

		});
		
		this.getExpandableListView().expandGroup(0);
		this.getExpandableListView().expandGroup(1);
	}

	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		if (groupPosition == 0) {
			Channel channel = server.getChannels().get(childPosition);
			Log.d(TAG, "Channel selected " + channel.toString());
			Intent channelIntent = new Intent(v.getContext(),
					ChannelActivity.class);
			channelIntent.putExtra("cid", server.getCid());
			channelIntent.putExtra("chan", channel.getName());
			startActivity(channelIntent);
		}
		return false;
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.server_menu, menu);
		return true;
	}

	public class ServerItemAdapter extends BaseExpandableListAdapter {
		public Object getChild(int groupPosition, int childPosition) {
			if (groupPosition == 0) {
				return server.getChannels().get(childPosition);
			} else {
				return server.getPrivates().get(childPosition);
			}
		}

		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		public int getChildrenCount(int groupPosition) {
			if (groupPosition == 0) {
				return server.getChannels().size();
			} else {
				return server.getPrivates().size();
			}
		}

		public TextView getGenericView() {
			// Layout parameters for the ExpandableListView
			AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, 64);

			TextView textView = new TextView(ServerActivity.this);
			textView.setLayoutParams(lp);
			// Center the text vertically
			textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
			// Set the text starting position
			textView.setPadding(52, 0, 0, 0);
			return textView;
		}

		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			LayoutInflater mInflater = getLayoutInflater();
			View row = null;
			if (groupPosition == 0) {
				if (null == convertView) {
					row = mInflater.inflate(R.layout.channel_item, null);
				} else {
					row = convertView;
				}
				Channel channel = server.getChannels().get(childPosition);
				if (channel != null) {
					TextView name = (TextView) row
							.findViewById(R.id.channelName);
					TextView topic = (TextView) row.findViewById(R.id.topic);
					name.setText(channel.getName());
					if (channel.getTopic() != null
							&& !channel.getTopic().equalsIgnoreCase("null")) {
						topic.setText(channel.getTopic() + " - by "
								+ channel.getTopicAuthor());
					} else {
						topic.setText("");
					}
				}
			} else {
				row = getGenericView();
				((TextView) row).setText(getChild(groupPosition, childPosition)
						.toString());
			}

			return row;
		}

		public Object getGroup(int groupPosition) {
			if (groupPosition == 0) {
				return getResources().getString(R.string.channels);
			} else {
				return getResources().getString(R.string.private_chats);
			}
		}

		public int getGroupCount() {
			return 2;
		}

		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			TextView textView = getGenericView();
			textView.setText(getGroup(groupPosition).toString());
			return textView;
		}

		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

		public boolean hasStableIds() {
			return true;
		}

	}

}
