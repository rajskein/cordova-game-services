package com.skein.cordova.plugins;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class GameTurn {

  public static final String TAG = "EBTurn";

  public JSONObject data;
  public int turnCounter;
  public String cPlayer;
  public JSONArray p_1;
  public JSONArray p_2;


  public GameTurn() {
  }

  // This is the byte array we will write out to the TBMP API.
  public byte[] persist() {
    JSONObject retVal = new JSONObject();

    try {
      retVal.put("data", data);
      retVal.put("turnCounter", turnCounter);
      retVal.put("cPlayer", cPlayer);
      if(p_1 != null) {
        retVal.put("p_1", p_1);
      }else{
        p_1=new JSONArray();
        retVal.put("p_1", p_1);
      }
      if(p_2 != null) {
        retVal.put("p_2", p_2);
      }else{
        p_2=new JSONArray();
        retVal.put("p_2", p_2);
      }

    } catch (JSONException e) {
      Log.e("Turn", "There was an issue writing JSON!", e);
    }

    String st = retVal.toString();

    Log.d(TAG, "==== PERSISTING\n" + st);

    return st.getBytes(Charset.forName("UTF-8"));
  }

  // Creates a new instance of SkeletonTurn.
  static public JSONObject unpersist(byte[] byteArray) {
    JSONObject obj=null;
    if (byteArray == null) {
      Log.d(TAG, "Empty array---possible bug.");
      return new JSONObject();
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
       obj = new JSONObject(st);

      if (obj.has("data")) {
        retVal.data = obj.getJSONObject("data");
      }
      if (obj.has("turnCounter")) {
        retVal.turnCounter = obj.getInt("turnCounter");
      }
      if (obj.has("cPlayer")) {
        retVal.cPlayer = obj.getString("cPlayer");
      }
      if (obj.has("turnCounter")) {
        retVal.p_1 = obj.getJSONArray("p_1");
      }
      if (obj.has("turnCounter")) {
        retVal.p_2= obj.getJSONArray("p_2");
      }

    } catch (JSONException e) {
      Log.e("SkeletonTurn", "There was an issue parsing JSON!", e);
    }

    return obj;
  }
}
