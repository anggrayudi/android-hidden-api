# Append suffix -SNAPSHOT
if [[ ! ($(grep "VERSION_NAME=" gradle.properties) == *"-SNAPSHOT") ]]; then
  sed -ie "s/VERSION_NAME.*$/&-SNAPSHOT/g" gradle.properties
  rm -f gradle.propertiese
fi

mkdir "$HOME/.android"
keytool -genkey -v -keystore "$HOME/.android/debug.keystore" -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "C=US, O=Android, CN=Android Debug"

{
    printf "\nkeyAlias=androiddebugkey"
    printf "\nkeyPassword=android"
    printf "\nstorePassword=android"
  } >>local.properties
