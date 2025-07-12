
#!/bin/bash

echo "Cleaning..."
cd iosApp
pod deintegrate
rm -rf Pods Podfile.lock
cd ..

echo "Installing pods..."
cd iosApp
pod install
cd ..

echo "Syncing Kotlin framework..."
./gradlew :composeApp:syncFramework

echo "✅ iOS setup complete. You can now open iosApp.xcworkspace"
