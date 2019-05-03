package com.skein.cordova.plugins;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class GameTurn {

  public static final String TAG = "EBTurn";

  public int data=0;
  public int turnCounter;

  public GameTurn() {
  }

  // This is the byte array we will write out to the TBMP API.
  public byte[] persist() {
    JSONObject retVal = new JSONObject();

    try {
      retVal.put("data", "1");
      retVal.put("turnCounter", turnCounter);

    } catch (JSONException e) {
      Log.e("Turn", "There was an issue writing JSON!", e);
    }

    String st = retVal.toString();

    Log.d(TAG, "==== PERSISTING\n" + st);

    return st.getBytes(Charset.forName("UTF-8"));
  }

  // Creates a new instance of SkeletonTurn.
  static public GameTurn unpersist(byte[] byteArray) {

    if (byteArray == null) {
      Log.d(TAG, "Empty array---possible bug.");
      return new GameTurn();
    }

    String st = null;
    try {
      st = new String(byteArray, "UTF-8");
    } catch (UnsupportedEncodingException e1) {
      e1.printStackTrace();
      return null;
    }

    Log.d(TAG, "====UNPERSIST \n" + st);

    GameTurn retVal = new GameTurn();

    try {
      JSONObject obj = new JSONObject(st);

      if (obj.has("data")) {
        retVal.data = obj.getInt("data");
      }
      if (obj.has("turnCounter")) {
        retVal.turnCounter = obj.getInt("turnCounter");
      }

    } catch (JSONException e) {
      Log.e("SkeletonTurn", "There was an issue parsing JSON!", e);
    }

    return retVal;
  }
}
