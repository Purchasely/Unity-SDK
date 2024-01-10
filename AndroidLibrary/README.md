# Purchasely Unity Android SDK

## Local Testing
Use AAR files for local testing

## Deployment

```groovy
./gradlew clean :purchasely-lib:assembleRelease
//Publish to MavenCentral
./gradlew :purchasely-lib:publishReleasePublicationToSnapshotRepository
//Close and Release MavenCentral
./gradlew closeAndReleaseRepository
```

This will only work if you have set up the following variables in your local.properties file :

signing.keyId

signing.password

signing.secretKeyRingFile

ossrhUsername

ossrhPassword

sonatypeStagingProfileId=1390e1929a68bb

## Local Deployment

```groovy
./gradlew clean :purchasely-lib:assembleRelease

./gradlew :purchasely-lib:publishToMavenLocal
```