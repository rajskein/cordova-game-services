
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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.games.AchievementsClient;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchUpdateCallback;
import com.skein.cordova.plugins.GameHelper.GameHelperListener;
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

    private static final String LOGTAG = "GamesServices";
    private static final String ACTION_SIGN_IN = "signIn";
    private static final String ACTION_SIGN_OUT = "signOut";
    private static final String ACTION_IS_SIGNEDIN = "isSignedIn";
    private static final String ACTION_AUTO_MATCH = "autoMatch";
    private static final String ACTION_START_MATCH = "startMatch";
    private static final String ACTION_PLAY_MATCH = "playMatch";
    private static final String ACTION_CHECK_MATCH = "checkMatch";
    private static final String ACTION_RE_MATCH = "reMatch";
    private static final String ACTION_CANCEL_MATCH = "cancelMatch";
    private static final String ACTION_LEAVE_MATCH = "leaveMatch";
    private static final String ACTION_FINISH_MATCH = "finishMatch";
    private static final String ACTION_SHOW_ACHIEVEMENTS = "showAchievements";
    private static final String ACTION_LEADER_BOARD = "showLeaderBoard";
    private static final String ACTION_SUBMIT_SCORE = "submitScore";

    private static final int RC_SIGN_IN = 9001;
    final static int RC_SELECT_PLAYERS = 10000;
    final static int RC_LOOK_AT_MATCHES = 10001;
  private static final int RC_UNUSED = 5001;


    private TurnBasedMultiplayerClient mTurnBasedMultiplayerClient = null;
    private GoogleSignInClient mGoogleSignInClient = null;
    private GoogleSignInAccount mGoogleSignInAccount=null;
    private InvitationsClient mInvitationsClient = null;
  private AchievementsClient mAchievementsClient;
  private LeaderboardsClient mLeaderboardsClient;
     private String mDisplayName;
     private String mPlayerId;

     private GameHelper gameHelper;
  JSONObject dataObj=null;

     public TurnBasedMatch mMatch;
     public GameTurn mTurnData;
     private AlertDialog mAlertDialog;
     private int googlePlayServicesReturnCode;

  private CallbackContext callback = null;
  private CallbackContext authCallbackContext;

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
      Log.d(LOGTAG, "==== options\n" + inputs.toString());

      JSONObject options = inputs.optJSONObject(0);
      callback=callbackContext;
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
          executeIsSignedIn(options,callbackContext);
        } else if (ACTION_AUTO_MATCH.equals(action)) {
          executeAutoMatch(callbackContext);
        }else if (ACTION_START_MATCH.equals(action)) {
          executeStartMatch(options,callbackContext);
        }else if (ACTION_PLAY_MATCH.equals(action)) {
          executePlayMatch(options,callbackContext);
        }else if (ACTION_CHECK_MATCH.equals(action)) {
          executeCheckMatch(callbackContext);
        }else if (ACTION_RE_MATCH.equals(action)) {
          executeReMatch(callbackContext);
        } else if (ACTION_CANCEL_MATCH.equals(action)) {
          executeCancelMatch(callbackContext);
        }else if (ACTION_FINISH_MATCH.equals(action)) {
          executeFinishMatch(callbackContext);
        }else if (ACTION_LEAVE_MATCH.equals(action)) {
          executeLeaveMatch(callbackContext);
        }else if (ACTION_SHOW_ACHIEVEMENTS.equals(action)) {
            executeShowAchievements(callbackContext);
        } else if (ACTION_LEADER_BOARD.equals(action)) {
            executeshowLeaderBoard(callbackContext);
        }else if (ACTION_SUBMIT_SCORE.equals(action)) {
          executeSubmitScore(options,callbackContext);
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

    private void executeIsSignedIn(final JSONObject options,final CallbackContext callbackContext) {


      boolean isSignedIn = mTurnBasedMultiplayerClient != null;
      PluginResult result = new PluginResult(PluginResult.Status.OK,isSignedIn );
      callback.sendPluginResult(result);
    }
private void executePlayMatch(final JSONObject options,final CallbackContext callbackContext) {

      cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              mTurnData = new GameTurn();
              // Some basic turn data

              try {
                mTurnData.data = options.getJSONObject("data") ;
              } catch (JSONException e) {
                e.printStackTrace();
              }
              mTurnData.turnCounter += 1;


              String nextParticipantId = getNextParticipantId();


              mTurnBasedMultiplayerClient.takeTurn(mMatch.getMatchId(),
                mTurnData.persist(), nextParticipantId)
                .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
                  @Override
                  public void onSuccess(TurnBasedMatch turnBasedMatch) {
                   // onUpdateMatch(turnBasedMatch);

                   boolean isDoingTurn = (turnBasedMatch.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN);

                    if (isDoingTurn) {
                      updateMatch(turnBasedMatch);
                      return;
                    }
                  }
                })
                .addOnFailureListener(createFailureListener("There was a problem taking a turn!"));

              mTurnData = null;


            }
        });
    }



  private void executeStartMatch(final JSONObject options,final CallbackContext callbackContext) {

  final PlayGamesServices plugin = this;
      
                mTurnBasedMultiplayerClient.getSelectOpponentsIntent(1, 1, true)
                  .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                      cordova.startActivityForResult(plugin,intent, RC_SELECT_PLAYERS);
                    }
                  })
                  .addOnFailureListener(createFailureListener("fail"));
  }
    private void executeAutoMatch(final CallbackContext callbackContext) {
        Log.d(LOGTAG, "executeShowAllLeaderboards");

        final PlayGamesServices plugin = this;
      Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(1, 1, 0);

      TurnBasedMatchConfig turnBasedMatchConfig = TurnBasedMatchConfig.builder()
        .setAutoMatchCriteria(autoMatchCriteria).build();

      // Start the match
      mTurnBasedMultiplayerClient.createMatch(turnBasedMatchConfig)
        .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
          @Override
          public void onSuccess(TurnBasedMatch turnBasedMatch) {
             onInitiateMatch(turnBasedMatch);
          }
        })
        .addOnFailureListener(createFailureListener("There was a problem creating a match!"));
    }


  private void executeReMatch(final CallbackContext callbackContext) {
    Log.d(LOGTAG, "executeShowAllLeaderboards");
    Log.d(LOGTAG,"Log rematch"+mMatch.toString());

    mTurnBasedMultiplayerClient.rematch(mMatch.getMatchId())
      .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
        @Override
        public void onSuccess(TurnBasedMatch turnBasedMatch) {
          onInitiateMatch(turnBasedMatch);
          Log.d(LOGTAG,"rematch Done"+turnBasedMatch);

        }
      })
      .addOnFailureListener(createFailureListener("There was a problem starting a rematch!"));
    mMatch = null;
  }


    private void executeShowAchievements(final CallbackContext callbackContext) {
        final PlayGamesServices plugin = this;

      mAchievementsClient.getAchievementsIntent()
        .addOnSuccessListener(new OnSuccessListener<Intent>() {
          @Override
          public void onSuccess(Intent intent) {
            cordova.startActivityForResult(plugin,intent, RC_UNUSED);
          }
        })
        .addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {
           // handleException(e, getString(R.string.leaderboards_exception));
          }
        });

    }


    private void executeshowLeaderBoard(final CallbackContext callbackContext) {
        Log.d(LOGTAG, "executeShowPlayer");
      final PlayGamesServices plugin = this;
      mLeaderboardsClient.getAllLeaderboardsIntent()
        .addOnSuccessListener(new OnSuccessListener<Intent>() {
          @Override
          public void onSuccess(Intent intent) {
            cordova.startActivityForResult(plugin,intent, RC_UNUSED);
          }
        })
        .addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {
            //handleException(e, getString(R.string.leaderboards_exception));
          }
        });
    }


  private void executeCheckMatch(final CallbackContext callbackContext) {
    Log.d(LOGTAG, "executeShowPlayer");
    final PlayGamesServices plugin = this;
       /* cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {*/
    mTurnBasedMultiplayerClient.getInboxIntent()
      .addOnSuccessListener(new OnSuccessListener<Intent>() {
        @Override
        public void onSuccess(Intent intent) {
          cordova.startActivityForResult(plugin,intent, RC_LOOK_AT_MATCHES);
        }
      })
      .addOnFailureListener(createFailureListener("fail"));
               /* }
                catch(Exception e) {
                    Log.w(LOGTAG, "executeShowPlayer: Error providing player data", e);
                    callbackContext.error("executeShowPlayer: Error providing player data");
                }
            }
        });*/
  }

  
  private void onInitiateMatch(TurnBasedMatch match) {
   // dismissSpinner();

    if (match.getData() != null) {
      // This is a game that has already started, so I'll just start
      updateMatch(match);
      return;
    }

    startMatch(match);
  }


  public void startMatch(TurnBasedMatch match) {

    mTurnData = new GameTurn();
    // Some basic turn data
JSONObject obj= new JSONObject();

      mTurnData.data = obj;
    mTurnData.turnCounter = 0;
    mMatch = match;

    String myParticipantId = mMatch.getParticipantId(mPlayerId);

    mTurnBasedMultiplayerClient.takeTurn(match.getMatchId(),
      mTurnData.persist(), myParticipantId)
      .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
        @Override
        public void onSuccess(TurnBasedMatch turnBasedMatch) {


          updateMatch(turnBasedMatch);
        }
      })
      .addOnFailureListener(createFailureListener("There was a problem taking a turn!"));
  }


  // Cancel the game. Should possibly wait until the game is canceled before
  // giving up on the view.
  public void executeCancelMatch(final CallbackContext callbackContext) {

    mTurnBasedMultiplayerClient.cancelMatch(mMatch.getMatchId())
      .addOnSuccessListener(new OnSuccessListener<String>() {
        @Override
        public void onSuccess(String matchId) {
Log.d(LOGTAG,"matchId Cacnel"+matchId);
        }
      })
      .addOnFailureListener(createFailureListener("There was a problem cancelling the match!"));

  }

  // Leave the game during your turn. Note that there is a separate
  // mTurnBasedMultiplayerClient.leaveMatch() if you want to leave NOT on your turn.
  public void executeLeaveMatch(final CallbackContext callbackContext) {

    String nextParticipantId = getNextParticipantId();

    mTurnBasedMultiplayerClient.leaveMatchDuringTurn(mMatch.getMatchId(), nextParticipantId)
      .addOnSuccessListener(new OnSuccessListener<Void>() {
        @Override
        public void onSuccess(Void aVoid) {
          Log.d(LOGTAG,"matchId Leave");
        }
      })
      .addOnFailureListener(createFailureListener("There was a problem leaving the match!"));


  }

  // Finish the game. Sometimes, this is your only choice.
  public void executeFinishMatch(final CallbackContext callbackContext) {
    ;
    mTurnBasedMultiplayerClient.finishMatch(mMatch.getMatchId())
      .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
        @Override
        public void onSuccess(TurnBasedMatch turnBasedMatch) {
          updateMatch(turnBasedMatch);
          Log.d(LOGTAG,"matchId Finish");

        }
      })
      .addOnFailureListener(createFailureListener("There was a problem finishing the match!"));

  }
  public void executeSubmitScore(final JSONObject options,final CallbackContext callbackContext) {

int finalscore=0;
    try {
      finalscore = options.getInt("data") ;
    } catch (JSONException e) {
      e.printStackTrace();
    }
    mLeaderboardsClient.submitScore("CgkIut_irrQWEAIQAw", finalscore);
  }


  public String getNextParticipantId() {

    String myParticipantId = mMatch.getParticipantId(mPlayerId);

    ArrayList<String> participantIds = mMatch.getParticipantIds();

    int desiredIndex = -1;

    for (int i = 0; i < participantIds.size(); i++) {
      if (participantIds.get(i).equals(myParticipantId)) {
        desiredIndex = i + 1;
      }
    }

    if (desiredIndex < participantIds.size()) {
      return participantIds.get(desiredIndex);
    }

    if (mMatch.getAvailableAutoMatchSlots() <= 0) {
      // You've run out of automatch slots, so we start over.
      return participantIds.get(0);
    } else {
      // You have not yet fully automatched, so null will find a new
      // person to play against.
      return null;
    }
  }

  public void onUpdateMatch(TurnBasedMatch match) {
  //  dismissSpinner();

    if (match.canRematch()) {
     // askForRematch();
    }


   // setViewVisibility();
  }
  public void rematch() {
    mTurnBasedMultiplayerClient.rematch(mMatch.getMatchId())
      .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
        @Override
        public void onSuccess(TurnBasedMatch turnBasedMatch) {
          onInitiateMatch(turnBasedMatch);
        }
      })
      .addOnFailureListener(createFailureListener("There was a problem starting a rematch!"));
    mMatch = null;
  }
  public void updateMatch(TurnBasedMatch match) {
    mMatch = match;

    Log.d(LOGTAG, "==== options turnMatch 1231\n" + mMatch.toString());

    int status = match.getStatus();
    int turnStatus = match.getTurnStatus();
    Log.d(LOGTAG, "==== options turnMatch\n" + status + "trun" + turnStatus);


    android.widget.Toast.makeText(
      cordova.getActivity(),
      "2 You are game in as  "+match.getData().toString()
      , Toast.LENGTH_SHORT)
      .show();
    Log.d(LOGTAG, "==== options match getdata\n" + match.getData().toString());

    switch (status) {
      case TurnBasedMatch.MATCH_STATUS_CANCELED:
        android.widget.Toast.makeText(
          cordova.getActivity(),
          "This game was canceled!  "
          , Toast.LENGTH_SHORT)
          .show();
        showWarning("Canceled!", "This game was canceled!");
        return;
      case TurnBasedMatch.MATCH_STATUS_EXPIRED:
        android.widget.Toast.makeText(
          cordova.getActivity(),
          "This game is expired.  So sad! "
          , Toast.LENGTH_SHORT)
          .show();
        showWarning("Expired!", "This game is expired.  So sad!");
        return;
      case TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING:
        showWarning("Waiting for auto-match...",
          "We're still waiting for an automatch partner.");
        android.widget.Toast.makeText(
          cordova.getActivity(),
          "We're still waiting for an automatch partner."
          , Toast.LENGTH_SHORT)
          .show();
        return;
      case TurnBasedMatch.MATCH_STATUS_COMPLETE:
        if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE) {
          showWarning("Complete!",
            "This game is over; someone finished it, and so did you!  " +
              "There is nothing to be done.");
          android.widget.Toast.makeText(
            cordova.getActivity(),
            "This game is over; someone finished it, and so did you!  \" +\n" +
              "              \"There is nothing to be done."
            , Toast.LENGTH_SHORT)
            .show();
          break;
        }

        // Note that in this state, you must still call "Finish" yourself,
        // so we allow this to continue.
        showWarning("Complete!",
          "This game is over; someone finished it!  You can only finish it now.");
        android.widget.Toast.makeText(
          cordova.getActivity(),
          "This game is over; someone finished it!  You can only finish it now."
          , Toast.LENGTH_SHORT)
          .show();
    }

    // OK, it's active. Check on turn status.
    switch (turnStatus) {
      case TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN:
        JSONObject obj = GameTurn.unpersist(mMatch.getData());
        //setGameplayUI();


        PluginResult result = new PluginResult(PluginResult.Status.OK,obj );
        result.setKeepCallback(true);
        callback.sendPluginResult(result);
       // String nextParticipantId = getNextParticipantId();
        return;
      case TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN:
        // Should return results.
       // showWarning("Alas...", "It's not your turn.");
        PluginResult result1 = new PluginResult(PluginResult.Status.OK, "your opponant turn hura");
        result1.setKeepCallback(true);
        callback.sendPluginResult(result1);
        break;
      case TurnBasedMatch.MATCH_TURN_STATUS_INVITED:
        android.widget.Toast.makeText(
          cordova.getActivity(),
          "Still waiting for i"
          , Toast.LENGTH_SHORT)
          .show();
        //showWarning("Good inititative!",
          //"Still waiting for invitations.\n\nBe patient!");
    }
    mTurnData = null;
   // setViewVisibility();
  }





  public void showWarning(String title, String message) {
    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(cordova.getActivity());

    // set title
    alertDialogBuilder.setTitle(title).setMessage(message);

    // set dialog message
    alertDialogBuilder.setCancelable(false).setPositiveButton("OK",
      new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int id) {
          // if this button is clicked, close
          // current activity
        }
      });

    // create alert dialog
    mAlertDialog = alertDialogBuilder.create();
    mAlertDialog.show();
  }
    @Override
    public void onSignInFailed() {
        authCallbackContext.error("SIGN IN FAILED");
    }

    @Override
    public void onSignInSucceeded() {
        authCallbackContext.success("SIGN IN SUCCESS");
    }

  private void onConnected(GoogleSignInAccount googleSignInAccount) {

    mTurnBasedMultiplayerClient = Games.getTurnBasedMultiplayerClient(cordova.getActivity(), googleSignInAccount);
    mInvitationsClient = Games.getInvitationsClient(cordova.getActivity(), googleSignInAccount);
    mLeaderboardsClient = Games.getLeaderboardsClient(cordova.getActivity(), googleSignInAccount);
    mAchievementsClient = Games.getAchievementsClient(cordova.getActivity(), googleSignInAccount);

    Games.getPlayersClient(cordova.getActivity(), googleSignInAccount)
      .getCurrentPlayer()
      .addOnSuccessListener(
        new OnSuccessListener<Player>() {
          @Override
          public void onSuccess(Player player) {
            mDisplayName = player.getDisplayName();
            mPlayerId = player.getPlayerId();
            player.getHiResImageUri();
            JSONObject obj = new JSONObject();
            try {
              obj.put("playerName",mDisplayName);
              obj.put("playerId",player.getPlayerId());
              obj.put("playerUrl",player.getIconImageUri());
            } catch (JSONException e) {
              e.printStackTrace();
            }
            PluginResult result = new PluginResult(PluginResult.Status.OK,obj );
            callback.sendPluginResult(result);
          }
        }
      )
      .addOnFailureListener(createFailureListener("There was a problem getting the player!"));

    // Retrieve the TurnBasedMatch from the connectionHint
    GamesClient gamesClient = Games.getGamesClient(cordova.getActivity(), googleSignInAccount);
    gamesClient.getActivationHint()
      .addOnSuccessListener(new OnSuccessListener<Bundle>() {
        @Override
        public void onSuccess(Bundle hint) {
          if (hint != null) {
            TurnBasedMatch match = hint.getParcelable(Multiplayer.EXTRA_TURN_BASED_MATCH);
            if (match != null) {
              updateMatch(match);
            }
          }
        }
      })
      .addOnFailureListener(createFailureListener(
        "There was a problem getting the activation hint!"));

    mInvitationsClient.registerInvitationCallback(mInvitationCallback);
    mTurnBasedMultiplayerClient.registerTurnBasedMatchUpdateCallback(mMatchUpdateCallback);
  }

  private InvitationCallback mInvitationCallback = new InvitationCallback() {


    // Handle notification events.
    @Override
    public void onInvitationReceived(@NonNull Invitation invitation) {
      android.widget.Toast.makeText(
        cordova.getActivity(),
        "Invitation arrive from"+ invitation.getInviter().getDisplayName()
        , Toast.LENGTH_SHORT)
        .show();
    }

    @Override
    public void onInvitationRemoved(@NonNull String invitationId) {
      android.widget.Toast.makeText(
        cordova.getActivity(),
        "Invitation removed"
        , Toast.LENGTH_SHORT)
        .show();
    }
  };

  private TurnBasedMatchUpdateCallback mMatchUpdateCallback = new TurnBasedMatchUpdateCallback() {
    @Override
    public void onTurnBasedMatchReceived(@NonNull TurnBasedMatch turnBasedMatch) {
      android.widget.Toast.makeText(
        cordova.getActivity(),
        "A match was updated 1123."
        , Toast.LENGTH_SHORT)
        .show();
      updateMatch(turnBasedMatch);
    }

    @Override
    public void onTurnBasedMatchRemoved(@NonNull String matchId) {
      android.widget.Toast.makeText(
        cordova.getActivity(),
        "A match was removed"
        , Toast.LENGTH_SHORT)
        .show();
    }
  };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
           if (requestCode == RC_SIGN_IN) {

          Task<GoogleSignInAccount> task =
            GoogleSignIn.getSignedInAccountFromIntent(intent);

          try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            onConnected(account);

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
        }else if (requestCode == RC_LOOK_AT_MATCHES) {
          // Returning from the 'Select Match' dialog

          if (resultCode != Activity.RESULT_OK) {
            //logBadActivityResult(requestCode, resultCode,
            //  "User cancelled returning from the 'Select Match' dialog.");
            return;
          }

          TurnBasedMatch match = intent
            .getParcelableExtra(Multiplayer.EXTRA_TURN_BASED_MATCH);

          if (match != null) {
            updateMatch(match);
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
             Log.d(LOGTAG, "==== options invitees\n" + invitees.toString());

          TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
            .addInvitedPlayers(invitees)
            .setAutoMatchCriteria(autoMatchCriteria).build();
             Log.d(LOGTAG, "==== options\n" + tbmc.toString());

             // Start the match
          mTurnBasedMultiplayerClient.createMatch(tbmc)
            .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
              @Override
              public void onSuccess(TurnBasedMatch turnBasedMatch) {

                onInitiateMatch(turnBasedMatch);
                android.widget.Toast.makeText(
                  cordova.getActivity(),
                  "pending participant id"+turnBasedMatch.getPendingParticipantId()
                  , Toast.LENGTH_SHORT)
                  .show();
                Log.d(LOGTAG, "==== options\n" + turnBasedMatch.toString());

              }
            })
            .addOnFailureListener(createFailureListener("There was a problem creating a match!"));
          //showSpinner();

          mInvitationsClient.registerInvitationCallback(mInvitationCallback);
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
