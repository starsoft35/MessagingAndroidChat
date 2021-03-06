package handlers;

import android.content.Context;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.*;

import co.realtime.messagingandroidchat.MessageActivity;
import config.Config;
import domains.Channel;
import domains.Message;
import ibt.ortc.api.Ortc;
import ibt.ortc.extensibility.OnConnected;
import ibt.ortc.extensibility.OnDisconnected;
import ibt.ortc.extensibility.OnException;
import ibt.ortc.extensibility.OnMessage;
import ibt.ortc.extensibility.OnReconnected;
import ibt.ortc.extensibility.OnReconnecting;
import ibt.ortc.extensibility.OnSubscribed;
import ibt.ortc.extensibility.OnUnsubscribed;
import ibt.ortc.extensibility.OrtcClient;
import ibt.ortc.extensibility.OrtcFactory;
import interfaces.InterfaceRefresher;
import preferences.PreferencesManager;

public class OrtcHandler{

    public static OrtcHandler selfHandler;

	public static OrtcClient client = null;
	public Map<String, Channel> messages;
	public Boolean isConnected;
	public InterfaceRefresher rootView;
    public Context context;
	public InterfaceRefresher chatRoom;
	public InterfaceRefresher messagesView;

    final public static String ORTC_TAG = "ORTC";

	public static void subscribeChannel(String channel)
	{
		selfHandler.messages.put(channel, new Channel(channel));

		client.subscribeWithNotifications(channel, true, new OnMessage() {
            public void run(OrtcClient sender, String channel, String message) {
                selfHandler.handleMessage(message, channel);
            }
            ;
        });
	}


	public static void unsubscribeChannel(String channel)
	{
		client.unsubscribe(channel);
	}
	
	public static void prepareClient(Context context, InterfaceRefresher rootView){
		selfHandler = new OrtcHandler();
		selfHandler.context = context;
        selfHandler.rootView = rootView;
		selfHandler.messages = new HashMap<>();

		Ortc api = new Ortc();
		OrtcFactory factory;
		try {
			factory = api.loadOrtcFactory("IbtRealtimeSJ");
			client = factory.createClient();
            client.setConnectionMetadata(Config.METADATA);

            if(Config.CLUSTERURL != null){
                client.setClusterUrl(Config.CLUSTERURL);
            } else {
                client.setUrl(Config.URL);
            }

            client.setApplicationContext(selfHandler.context);
            client.setGoogleProjectId(Config.PROJECT_ID);

			client.onConnected = new OnConnected(){
				@Override
				public void run(OrtcClient sender) {
					selfHandler.isConnected = true;

					List<String> channels = PreferencesManager.getInstance(selfHandler.context).loadChannels();
					for (String channel : channels) {
						subscribeChannel(channel);
					}
                    selfHandler.rootView.refreshData(null);
				}
			};
			client.onDisconnected = new OnDisconnected(){
				@Override
				public void run(OrtcClient sender) {
					Log.i(ORTC_TAG, "Disconnected");

				}
			};
			client.onSubscribed = new OnSubscribed(){
				@Override
				public void run(OrtcClient sender, String channel) {
					Log.i(ORTC_TAG, "Subscribed to " + channel);
				}
			};
			client.onUnsubscribed = new OnUnsubscribed(){
				@Override
				public void run(OrtcClient sender, String channel) {
                    Log.i(ORTC_TAG, "Unsubscribed from " + channel);
				}
			};
			client.onException = new OnException(){
				@Override
				public void run(OrtcClient sender, Exception exc) {
					Log.e(ORTC_TAG, "Exception " + exc.toString());
				}
			};
			client.onReconnected = new OnReconnected(){
				@Override
				public void run(OrtcClient sender) {
                    Log.i(ORTC_TAG, "Reconnected");
				}
			};
			client.onReconnecting = new OnReconnecting(){
				@Override
				public void run(OrtcClient sender) {
			        Log.i(ORTC_TAG, "Reconnecting");
				}
			};

            client.connect(Config.APPKEY, Config.TOKEN);

		} catch (Exception e) {
            Log.e("Exception ",e.toString());
		}
	}

	public void sendMsg(Message msg, String channel) {
		OrtcHandler.client.send(channel, msg.toString());
	}
	
	public void handleMessage(String msg, String channel){
        SimpleDateFormat sdf = new SimpleDateFormat(Config.DATE_FORMAT);
		Message newMsg = null;
		if(msg.contains(":"))
		{
			newMsg = new Message(msg.substring(0, msg.indexOf(':')), msg.substring(msg.indexOf(':') + 1), sdf.format(new Date()));
		}
		else {
			newMsg = new Message("Unknown user", msg, sdf.format(new Date()));
		}
		
		Channel list = messages.get(channel);
		list.setUnRead(list.getUnRead() + 1);
		list.addMessage(newMsg);
		
		if(messagesView != null && ((MessageActivity)messagesView).getChannel().equals(channel))
			messagesView.refreshData(newMsg);
		
		if(chatRoom != null)
			chatRoom.refreshData(newMsg);

	}
	
}
