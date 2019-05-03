
package com.skein.cordova.plugins;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaActivity;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.*;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.berriart.cordova.plugins.GameHelper.GameHelperListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.games.Game;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.multiplayer.*;
import com.google.android.gms.games.leaderboard.*;
import com.google.android.gms.games.achievement.*;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.games.GamesCallbackStatusCodes;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.GamesClientStatusCodes;
import com.google.android.gms.games.InvitationsClient;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.TurnBasedMultiplayerClient;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

public class PlayGamesServices extends CordovaPlugin implements GameHelperListener {

    private static final String LOGTAG = "SkeinGamesServices";

    private static final String ACTION_SIGN_IN = "signIn";
    private static final String ACTION_SIGN_OUT = "signOut";
    private static final String ACTION_IS_SIGNEDIN = "isSignedIn";
    private static final String ACTION_AUTO_MATCH = "autoMatch";
    private static final String ACTION_SHOW_ACHIEVEMENTS = "showAchievements";
    private static final String ACTION_SHOW_PLAYER = "showPlayer";
    private TurnBasedMultiplayerClient mTurnBasedMultiplayerClient = null;
    private GameHelper gameHelper;
    private static final int RC_SIGN_IN = 9001;
    final static int RC_SELECT_PLAYERS = 10000;
    private CallbackContext authCallbackContext;
    private int googlePlayServicesReturnCode;
    private GoogleSignInClient mGoogleSignInClient = null;
    private GoogleSignInAccount mGoogleSignInAccount=null;

  @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Activity cordovaActivity = cordova.getActivity();

        googlePlayServicesReturnCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(cordovaActivity);

        if (googlePlayServicesReturnCode == ConnectionResult.SUCCESS) {
            gameHelper = new GameHelper(cordovaActivity, BaseGameActivity.CLIENT_GAMES);
            //gameHelper.setup(this);
           mGoogleSignInClient = GoogleSignIn.getClient(cordovaActivity, GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);



        } else {
            Log.w(LOGTAG, String.format("GooglePlayServices not available. Error: '" +
                    GoogleApiAvailability.getInstance().getErrorString(googlePlayServicesReturnCode) +
                    "'. Error Code: " + googlePlayServicesReturnCode));
        }

        cordova.setActivityResultCallback(this);
    }

    @Override
    public boolean execute(String action, JSONArray inputs, CallbackContext callbackContext) throws JSONException {

        JSONObject options = inputs.optJSONObject(0);

        if (gameHelper == null) {
           JSONObject googlePlayError = new JSONObject();
            googlePlayError.put("errorCode", googlePlayServicesReturnCode);
            googlePlayError.put("errorString", GoogleApiAvailability.getInstance().getErrorString(googlePlayServicesReturnCode));
             JSONObject result = new JSONObject();
            result.put("googlePlayError", googlePlayError);
            callbackContext.error(result);
            return true;
        }

        Log.i(LOGTAG, String.format("Processing action " + action + " ..."));

        if (ACTION_SIGN_IN.equals(action)) {
            executeSignIn(callbackContext);
        } else if (ACTION_SIGN_OUT.equals(action)) {
            executeSignOut(callbackContext);
        } else if (ACTION_IS_SIGNEDIN.equals(action)) {
          executeIsSignedIn(callbackContext);
        } else if (ACTION_AUTO_MATCH.equals(action)) {
          executeAutoMatch(callbackContext);
        } else if (ACTION_SHOW_ACHIEVEMENTS.equals(action)) {
            executeShowAchievements(callbackContext);
        } else if (ACTION_SHOW_PLAYER.equals(action)) {
            executeShowPlayer(callbackContext);
        } else {
            return false; // Tried to execute an unknown method
        }

        return true;
    }

    private void executeSignIn(final CallbackContext callbackContext) {
      authCallbackContext = callbackContext;
      final PlayGamesServices commandContext = this;
      Intent singingIntent = mGoogleSignInClient.getSignInIntent();
      cordova.startActivityForResult(commandContext,singingIntent,RC_SIGN_IN);
    }

    private void executeSignOut(final CallbackContext callbackContext) {
      final PlayGamesServices commandContext = this;
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              mGoogleSignInClient.signOut();
                callbackContext.success();
            }
        });
    }

    private void executeIsSignedIn(final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject result = new JSONObject();
                    boolean isSignedIn = mTurnBasedMultiplayerClient != null;
                    result.put("isSignedIn",isSignedIn);
                    callbackContext.success(result);
                } catch (JSONException e) {
                    callbackContext.error("executeIsSignedIn: unable to find user signin infomation");
                }
            }
        });
    }


    private void executeAutoMatch(final CallbackContext callbackContext) {
        Log.d(LOGTAG, "executeShowAllLeaderboards");

        final PlayGamesServices plugin = this;
      Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(1, 1, 0);

      TurnBasedMatchConfig turnBasedMatchConfig = TurnBasedMatchConfig.builder()
        .setAutoMatchCriteria(autoMatchCriteria).build();

      //  showSpinner();
      android.widget.Toast.makeText(
        cordova.getActivity(),
        "auto match start  "
        , Toast.LENGTH_SHORT)
        .show();
      // Start the match
      mTurnBasedMultiplayerClient.createMatch(turnBasedMatchConfig)
        .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
          @Override
          public void onSuccess(TurnBasedMatch turnBasedMatch) {
            // onInitiateMatch(turnBasedMatch);
            android.widget.Toast.makeText(
              cordova.getActivity(),
              "auto match started  "
              , Toast.LENGTH_SHORT)
              .show();
          }
        })
        .addOnFailureListener(createFailureListener("There was a problem creating a match!"));
    }

    private void executeShowAchievements(final CallbackContext callbackContext) {
        Log.d(LOGTAG, "executeShowAchievements");

        final PlayGamesServices plugin = this;
      mTurnBasedMultiplayerClient.getSelectOpponentsIntent(1, 1, true)
        .addOnSuccessListener(new OnSuccessListener<Intent>() {
          @Override
          public void onSuccess(Intent intent) {
            cordova.startActivityForResult(plugin,intent, RC_SELECT_PLAYERS);
          }
        })
        .addOnFailureListener(createFailureListener("fail"));
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              try {
                android.widget.Toast.makeText(
                  cordova.getActivity(),
                  "achements  "
                  , Toast.LENGTH_SHORT)
                  .show();


                if (gameHelper.isSignedIn()) {
                  TurnBasedMatch match = gameHelper.mTurnBasedMatch;
                  Game gam = match.getGame();

                  JSONObject matchJson = new JSONObject();
                  matchJson.put("mid", match.getMatchId());
                  matchJson.put("des", match.getDescription());
                  matchJson.put("rematch", match.canRematch());
                  matchJson.put("iconImageUrl", match.getStatus());

                  callbackContext.success(matchJson);
                }
              }
                catch(Exception e) {
                  Log.w(LOGTAG, "executeShowPlayer: Error providing player data", e);
                  callbackContext.error("executeShowPlayer: Error providing player data");
                }
            }
        });
    }

    private void executeShowPlayer(final CallbackContext callbackContext) {
        Log.d(LOGTAG, "executeShowPlayer");

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                try {
                    if (mGoogleSignInAccount!=null) {


                        JSONObject playerJson = new JSONObject();
                        playerJson.put("displayName", mGoogleSignInAccount.getDisplayName());
                        playerJson.put("playerId", mGoogleSignInAccount.getId());
                        playerJson.put("title", mGoogleSignInAccount.getEmail());
                        playerJson.put("iconImageUrl", mGoogleSignInAccount.getPhotoUrl());
                        playerJson.put("hiResIconImageUrl", mGoogleSignInAccount.getGrantedScopes());

                        callbackContext.success(playerJson);

                    } else {
                        Log.w(LOGTAG, "executeShowPlayer: not yet signed in");
                        callbackContext.error("executeShowPlayer: not yet signed in");
                    }
                }
                catch(Exception e) {
                    Log.w(LOGTAG, "executeShowPlayer: Error providing player data", e);
                    callbackContext.error("executeShowPlayer: Error providing player data");
                }
            }
        });
    }


    @Override
    public void onSignInFailed() {
        authCallbackContext.error("SIGN IN FAILED");
    }

    @Override
    public void onSignInSucceeded() {
        authCallbackContext.success("SIGN IN SUCCESS");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        //gameHelper.onActivityResult(requestCode, resultCode, intent);


        if (requestCode == RC_SIGN_IN) {

          Task<GoogleSignInAccount> task =
            GoogleSignIn.getSignedInAccountFromIntent(intent);

          try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
          //  onConnected(account);
            mGoogleSignInAccount = account;
            mTurnBasedMultiplayerClient = Games.getTurnBasedMultiplayerClient(cordova.getActivity(),account);
            Games.getPlayersClient(cordova.getActivity(), account)
              .getCurrentPlayer()
              .addOnSuccessListener(
                new OnSuccessListener<Player>() {
                  @Override
                  public void onSuccess(Player player) {


                    android.widget.Toast.makeText(
                      cordova.getActivity(),
                      "You are logged in as  "+player.getDisplayName()
                        , Toast.LENGTH_SHORT)
                      .show();
                  }
                }
              )
              .addOnFailureListener(createFailureListener("There was a problem getting the player!"));
          } catch (ApiException apiException) {
            String message = apiException.getMessage();
            if (message == null || message.isEmpty()) {
           //   message = getString(R.string.signin_other_error);
            }

           // onDisconnected();

            new AlertDialog.Builder(cordova.getActivity())
              .setMessage(message)
              .setNeutralButton(android.R.string.ok, null)
              .show();
          }
        }else if (requestCode == RC_SELECT_PLAYERS) {
          // Returning from 'Select players to Invite' dialog

          if (resultCode != Activity.RESULT_OK) {
            // user canceled
           // logBadActivityResult(requestCode, resultCode, "User cancelled returning from 'Select players to Invite' dialog");
            return;
          }

          // get the invitee list
          ArrayList<String> invitees = intent
            .getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);

          // get automatch criteria
          Bundle autoMatchCriteria;

          int minAutoMatchPlayers = intent.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
          int maxAutoMatchPlayers = intent.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);

          if (minAutoMatchPlayers > 0) {
            autoMatchCriteria = RoomConfig.createAutoMatchCriteria(minAutoMatchPlayers,
              maxAutoMatchPlayers, 0);
          } else {
            autoMatchCriteria = null;
          }

          TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
            .addInvitedPlayers(invitees)
            .setAutoMatchCriteria(autoMatchCriteria).build();

          // Start the match
          mTurnBasedMultiplayerClient.createMatch(tbmc)
            .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
              @Override
              public void onSuccess(TurnBasedMatch turnBasedMatch) {
              //  onInitiateMatch(turnBasedMatch);
                android.widget.Toast.makeText(
                  cordova.getActivity(),
                  "You are game  in as  "+turnBasedMatch.getDescription()
                  , Toast.LENGTH_SHORT)
                  .show();
              }
            })
            .addOnFailureListener(createFailureListener("There was a problem creating a match!"));
          //showSpinner();
        }
    }
  private OnFailureListener createFailureListener(final String string) {
    return new OnFailureListener() {
      @Override
      public void onFailure(@NonNull Exception e) {
       // handleException(e, string);
      }
    };
  }
}
/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package com.berriart.cordova.plugins;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaActivity;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.*;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.berriart.cordova.plugins.GameHelper.GameHelperListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.games.Game;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.multiplayer.*;
import com.google.android.gms.games.leaderboard.*;
import com.google.android.gms.games.achievement.*;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.games.GamesCallbackStatusCodes;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.GamesClientStatusCodes;
import com.google.android.gms.games.InvitationsClient;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.TurnBasedMultiplayerClient;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

public class PlayGamesServices extends CordovaPlugin implements GameHelperListener {

    private static final String LOGTAG = "SkeinGamesServices";

    private static final String ACTION_SIGN_IN = "signIn";
    private static final String ACTION_SIGN_OUT = "signOut";
    private static final String ACTION_IS_SIGNEDIN = "isSignedIn";
    private static final String ACTION_AUTO_MATCH = "autoMatch";
    private static final String ACTION_SHOW_ACHIEVEMENTS = "showAchievements";
    private static final String ACTION_SHOW_PLAYER = "showPlayer";
    private TurnBasedMultiplayerClient mTurnBasedMultiplayerClient = null;
    private GameHelper gameHelper;
    private static final int RC_SIGN_IN = 9001;
    final static int RC_SELECT_PLAYERS = 10000;
    private CallbackContext authCallbackContext;
    private int googlePlayServicesReturnCode;
    private GoogleSignInClient mGoogleSignInClient = null;
    private GoogleSignInAccount mGoogleSignInAccount=null;

  @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Activity cordovaActivity = cordova.getActivity();

        googlePlayServicesReturnCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(cordovaActivity);

        if (googlePlayServicesReturnCode == ConnectionResult.SUCCESS) {
            gameHelper = new GameHelper(cordovaActivity, BaseGameActivity.CLIENT_GAMES);
            //gameHelper.setup(this);
           mGoogleSignInClient = GoogleSignIn.getClient(cordovaActivity, GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);



        } else {
            Log.w(LOGTAG, String.format("GooglePlayServices not available. Error: '" +
                    GoogleApiAvailability.getInstance().getErrorString(googlePlayServicesReturnCode) +
                    "'. Error Code: " + googlePlayServicesReturnCode));
        }

        cordova.setActivityResultCallback(this);
    }

    @Override
    public boolean execute(String action, JSONArray inputs, CallbackContext callbackContext) throws JSONException {

        JSONObject options = inputs.optJSONObject(0);

        if (gameHelper == null) {
           JSONObject googlePlayError = new JSONObject();
            googlePlayError.put("errorCode", googlePlayServicesReturnCode);
            googlePlayError.put("errorString", GoogleApiAvailability.getInstance().getErrorString(googlePlayServicesReturnCode));
             JSONObject result = new JSONObject();
            result.put("googlePlayError", googlePlayError);
            callbackContext.error(result);
            return true;
        }

        Log.i(LOGTAG, String.format("Processing action " + action + " ..."));

        if (ACTION_SIGN_IN.equals(action)) {
            executeSignIn(callbackContext);
        } else if (ACTION_SIGN_OUT.equals(action)) {
            executeSignOut(callbackContext);
        } else if (ACTION_IS_SIGNEDIN.equals(action)) {
          executeIsSignedIn(callbackContext);
        } else if (ACTION_AUTO_MATCH.equals(action)) {
          executeAutoMatch(callbackContext);
        } else if (ACTION_SHOW_ACHIEVEMENTS.equals(action)) {
            executeShowAchievements(callbackContext);
        } else if (ACTION_SHOW_PLAYER.equals(action)) {
            executeShowPlayer(callbackContext);
        } else {
            return false; // Tried to execute an unknown method
        }

        return true;
    }

    private void executeSignIn(final CallbackContext callbackContext) {
      authCallbackContext = callbackContext;
      final PlayGamesServices commandContext = this;
      Intent singingIntent = mGoogleSignInClient.getSignInIntent();
      cordova.startActivityForResult(commandContext,singingIntent,RC_SIGN_IN);
    }

    private void executeSignOut(final CallbackContext callbackContext) {
      final PlayGamesServices commandContext = this;
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              mGoogleSignInClient.signOut();
                callbackContext.success();
            }
        });
    }

    private void executeIsSignedIn(final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject result = new JSONObject();
                    boolean isSignedIn = mTurnBasedMultiplayerClient != null;
                    result.put("isSignedIn",isSignedIn);
                    callbackContext.success(result);
                } catch (JSONException e) {
                    callbackContext.error("executeIsSignedIn: unable to find user signin infomation");
                }
            }
        });
    }


    private void executeAutoMatch(final CallbackContext callbackContext) {
        Log.d(LOGTAG, "executeShowAllLeaderboards");

        final PlayGamesServices plugin = this;
      Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(1, 1, 0);

      TurnBasedMatchConfig turnBasedMatchConfig = TurnBasedMatchConfig.builder()
        .setAutoMatchCriteria(autoMatchCriteria).build();

      //  showSpinner();
      android.widget.Toast.makeText(
        cordova.getActivity(),
        "auto match start  "
        , Toast.LENGTH_SHORT)
        .show();
      // Start the match
      mTurnBasedMultiplayerClient.createMatch(turnBasedMatchConfig)
        .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
          @Override
          public void onSuccess(TurnBasedMatch turnBasedMatch) {
            // onInitiateMatch(turnBasedMatch);
            android.widget.Toast.makeText(
              cordova.getActivity(),
              "auto match started  "
              , Toast.LENGTH_SHORT)
              .show();
          }
        })
        .addOnFailureListener(createFailureListener("There was a problem creating a match!"));
    }

    private void executeShowAchievements(final CallbackContext callbackContext) {
        Log.d(LOGTAG, "executeShowAchievements");

        final PlayGamesServices plugin = this;
      mTurnBasedMultiplayerClient.getSelectOpponentsIntent(1, 1, true)
        .addOnSuccessListener(new OnSuccessListener<Intent>() {
          @Override
          public void onSuccess(Intent intent) {
            cordova.startActivityForResult(plugin,intent, RC_SELECT_PLAYERS);
          }
        })
        .addOnFailureListener(createFailureListener("fail"));
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              try {
                android.widget.Toast.makeText(
                  cordova.getActivity(),
                  "achements  "
                  , Toast.LENGTH_SHORT)
                  .show();


                if (gameHelper.isSignedIn()) {
                  TurnBasedMatch match = gameHelper.mTurnBasedMatch;
                  Game gam = match.getGame();

                  JSONObject matchJson = new JSONObject();
                  matchJson.put("mid", match.getMatchId());
                  matchJson.put("des", match.getDescription());
                  matchJson.put("rematch", match.canRematch());
                  matchJson.put("iconImageUrl", match.getStatus());

                  callbackContext.success(matchJson);
                }
              }
                catch(Exception e) {
                  Log.w(LOGTAG, "executeShowPlayer: Error providing player data", e);
                  callbackContext.error("executeShowPlayer: Error providing player data");
                }
            }
        });
    }

    private void executeShowPlayer(final CallbackContext callbackContext) {
        Log.d(LOGTAG, "executeShowPlayer");

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                try {
                    if (mGoogleSignInAccount!=null) {


                        JSONObject playerJson = new JSONObject();
                        playerJson.put("displayName", mGoogleSignInAccount.getDisplayName());
                        playerJson.put("playerId", mGoogleSignInAccount.getId());
                        playerJson.put("title", mGoogleSignInAccount.getEmail());
                        playerJson.put("iconImageUrl", mGoogleSignInAccount.getPhotoUrl());
                        playerJson.put("hiResIconImageUrl", mGoogleSignInAccount.getGrantedScopes());

                        callbackContext.success(playerJson);

                    } else {
                        Log.w(LOGTAG, "executeShowPlayer: not yet signed in");
                        callbackContext.error("executeShowPlayer: not yet signed in");
                    }
                }
                catch(Exception e) {
                    Log.w(LOGTAG, "executeShowPlayer: Error providing player data", e);
                    callbackContext.error("executeShowPlayer: Error providing player data");
                }
            }
        });
    }


    @Override
    public void onSignInFailed() {
        authCallbackContext.error("SIGN IN FAILED");
    }

    @Override
    public void onSignInSucceeded() {
        authCallbackContext.success("SIGN IN SUCCESS");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        //gameHelper.onActivityResult(requestCode, resultCode, intent);


        if (requestCode == RC_SIGN_IN) {

          Task<GoogleSignInAccount> task =
            GoogleSignIn.getSignedInAccountFromIntent(intent);

          try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
          //  onConnected(account);
            mGoogleSignInAccount = account;
            mTurnBasedMultiplayerClient = Games.getTurnBasedMultiplayerClient(cordova.getActivity(),account);
            Games.getPlayersClient(cordova.getActivity(), account)
              .getCurrentPlayer()
              .addOnSuccessListener(
                new OnSuccessListener<Player>() {
                  @Override
                  public void onSuccess(Player player) {


                    android.widget.Toast.makeText(
                      cordova.getActivity(),
                      "You are logged in as  "+player.getDisplayName()
                        , Toast.LENGTH_SHORT)
                      .show();
                  }
                }
              )
              .addOnFailureListener(createFailureListener("There was a problem getting the player!"));
          } catch (ApiException apiException) {
            String message = apiException.getMessage();
            if (message == null || message.isEmpty()) {
           //   message = getString(R.string.signin_other_error);
            }

           // onDisconnected();

            new AlertDialog.Builder(cordova.getActivity())
              .setMessage(message)
              .setNeutralButton(android.R.string.ok, null)
              .show();
          }
        }else if (requestCode == RC_SELECT_PLAYERS) {
          // Returning from 'Select players to Invite' dialog

          if (resultCode != Activity.RESULT_OK) {
            // user canceled
           // logBadActivityResult(requestCode, resultCode, "User cancelled returning from 'Select players to Invite' dialog");
            return;
          }

          // get the invitee list
          ArrayList<String> invitees = intent
            .getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);

          // get automatch criteria
          Bundle autoMatchCriteria;

          int minAutoMatchPlayers = intent.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
          int maxAutoMatchPlayers = intent.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);

          if (minAutoMatchPlayers > 0) {
            autoMatchCriteria = RoomConfig.createAutoMatchCriteria(minAutoMatchPlayers,
              maxAutoMatchPlayers, 0);
          } else {
            autoMatchCriteria = null;
          }

          TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
            .addInvitedPlayers(invitees)
            .setAutoMatchCriteria(autoMatchCriteria).build();

          // Start the match
          mTurnBasedMultiplayerClient.createMatch(tbmc)
            .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
              @Override
              public void onSuccess(TurnBasedMatch turnBasedMatch) {
              //  onInitiateMatch(turnBasedMatch);
                android.widget.Toast.makeText(
                  cordova.getActivity(),
                  "You are game  in as  "+turnBasedMatch.getDescription()
                  , Toast.LENGTH_SHORT)
                  .show();
              }
            })
            .addOnFailureListener(createFailureListener("There was a problem creating a match!"));
          //showSpinner();
        }
    }
  private OnFailureListener createFailureListener(final String string) {
    return new OnFailureListener() {
      @Override
      public void onFailure(@NonNull Exception e) {
       // handleException(e, string);
      }
    };
  }
}
