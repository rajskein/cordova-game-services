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
