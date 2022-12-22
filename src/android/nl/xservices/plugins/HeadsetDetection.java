package nl.xservices.plugins;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAssignedNumbers;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

public class HeadsetDetection extends CordovaPlugin {

  private static final String LOG_TAG = "HeadsetDetection";

  private static final String ACTION_DETECT = "detect";
  private static final String ACTION_EVENT = "registerRemoteEvents";
  protected static CordovaWebView mCachedWebView = null;

  BroadcastReceiver receiver;

  public HeadsetDetection() {
      this.receiver = null;
  }

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
      super.initialize(cordova, webView);
      Log.d(LOG_TAG, "Headset plugin initialized");
      mCachedWebView = webView;
      IntentFilter intentFilter = new IntentFilter();
      intentFilter.addCategory(BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY + "." + BluetoothAssignedNumbers.PLANTRONICS);
      intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
      Log.d(LOG_TAG, "Receiver started");

      this.receiver = new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
              Log.d(LOG_TAG, "onReceive: "+intent.getAction());
              if (intent.getAction().equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
                  Log.d(LOG_TAG, "BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED");

                  int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE,
                          BluetoothAdapter.STATE_DISCONNECTED);

                  Log.d(LOG_TAG, "state = "+state);

                  if (state == 2) {
                      Log.d(LOG_TAG, "Headset CONNECTED");
                      mCachedWebView.sendJavascript("cordova.require('cordova-plugin-headsetdetection.HeadsetDetection').remoteHeadsetAdded();");
                  } else if (state==1) {
                      Log.d(LOG_TAG, "Headset is connecting...");
                  } else {
                      Log.d(LOG_TAG, "Headset disconnected");
                      mCachedWebView.sendJavascript("cordova.require('cordova-plugin-headsetdetection.HeadsetDetection').remoteHeadsetRemoved();");
                  }


              }
          }
      };
      mCachedWebView.getContext().registerReceiver(this.receiver, intentFilter);
  }

  @Override
  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
    try {
      if (ACTION_DETECT.equals(action) || ACTION_EVENT.equals(action)) {
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, isHeadsetEnabled()));
        return true;
      } else {
        callbackContext.error("headsetdetection." + action + " is not a supported function. Did you mean '" + ACTION_DETECT + "'?");
        return false;
      }
    } catch (Exception e) {
      callbackContext.error(e.getMessage());
      return false;
    }
  }

  private boolean isHeadsetEnabled() {
    Log.d(LOG_TAG, "isHeadsetEnabled()");
    final AudioManager audioManager = (AudioManager) cordova.getActivity().getSystemService(Context.AUDIO_SERVICE);
    return audioManager.isWiredHeadsetOn() ||
        audioManager.isBluetoothA2dpOn() ||
        audioManager.isBluetoothScoOn();
  }

  public void onDestroy() {
      removeHeadsetListener();
  }

  public void onReset() {
      removeHeadsetListener();
  }

  private void removeHeadsetListener() {
      if (this.receiver != null) {
          try {
              mCachedWebView.getContext().unregisterReceiver(this.receiver);
              this.receiver = null;
          } catch (Exception e) {
              Log.e(LOG_TAG, "Error unregistering battery receiver: " + e.getMessage(), e);
          }
      }
  }
}
