# Purchasely Unity Android SDK

## Local Testing
Use AAR files for local testing

## Deployment

```groovy
./gradlew clean :purchasely-lib:assembleRelease

//Publish to Maven Central
./gradlew :purchasely-lib:publishAndReleaseToMavenCentral --no-configuration-cache
```

This will only work if you have set up the following variables in your local.properties file :
```
signing.keyId=your_signing_key_id

signing.password=your_signing_password

signing.secretKeyRingFile=path/to/gpg/key

sonatypeStagingProfileId=1390e1929a68bb
```   

and in your ~/.gradle/gradle.properties file :
```
mavenCentralUsername=username

mavenCentralPassword=the_password
```

## Local Deployment

```groovy
./gradlew clean :purchasely-lib:assembleRelease

./gradlew :purchasely-lib:publishToMavenLocal
```