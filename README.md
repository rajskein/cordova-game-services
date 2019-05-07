  #Cordova Google Play Games Services

Install
 
  cordova plugin add https://github.com/kingemerald/cordova-games-services.git --variable GAME_APP_ID=app_id
  

Usage
Authentication
Connect
Connection/Authentication to the Google play games will happen with signIn api. This will prompt for default Game play console UI.


cordova.plugins.playGamesServices.signIn({
}, successCallback, failureCallback);
  
Disconnect
You should provde the option for users to sign out

cordova.plugins.playGamesServices.signOut({
}, successCallback, successCallback);



  implementation "com.android.support:appcompat-v7:27.+"
  implementation "com.android.support:support-v4:27.+"
  implementation "com.google.android.gms:play-services-games:16.+"
  implementation "com.google.android.gms:play-services-auth:16.+"

  signingConfigs {
    debug {
      storeFile file("C:/Users/rajam/Android_App_Certificates/TicTacToe.jks")
      storePassword "Skein@123"
      keyAlias "Skein"
      keyPassword "Skein@123"
    }
  }
  buildTypes {
    debug {
      minifyEnabled false
      //proguardFiles getDefaultProguardFile('proguard-android.txt')
      signingConfig signingConfigs.debug
    }
  }


##License

[MIT License](http://ilee.mit-license.org)
