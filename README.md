---

# Otpless React Native Pre Built UI

Leverage our Pre-Built UI for rapid integration and customization of authentication flows in your application. This setup allows you to adjust appearance and functionality through the OTPLESS dashboard with minimal coding.

---

## Step 1: Install OTPLESS SDK Dependency

Install the OTPLESS SDK dependency by running the following command in your terminal at the root of your React Native project:

```bash
npm i otpless-react-native-lp
```

---

## Step 2: Platform-specific Integrations

### Android

1. Add intent filter inside your `android/app/src/main/AndroidManifest.xml` file into your Main activity code block:

```xml
<intent-filter>
  <action android:name="android.intent.action.VIEW" />
  <category android:name="android.intent.category.DEFAULT" />
  <category android:name="android.intent.category.BROWSABLE" />
  <data
      android:host="otpless"
      android:scheme="otpless.YOUR_APP_ID_LOWERCASE"/>
</intent-filter>
```

2. Add Network Security Config inside your `android/app/src/main/AndroidManifest.xml` file into your `<application>` code block (Only required if you are using the SNA feature):

```xml
android:networkSecurityConfig="@xml/otpless_network_security_config"
```

3. Change your activity `launchMode` to `singleTop` and `exported=true` for your Main Activity:

```xml
android:launchMode="singleTop"
android:exported="true"
```

---

### iOS

1. Add the following block to your `info.plist` file:

```xml
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>otpless.YOUR_APP_ID_LOWERCASE</string>
        </array>
        <key>CFBundleTypeRole</key>
        <string>Editor</string>
        <key>CFBundleURLName</key>
        <string>otpless</string>
    </dict>
</array>
<key>LSApplicationQueriesSchemes</key>
<array>
    <string>whatsapp</string>
    <string>otpless</string>
    <string>gootpless</string>
    <string>com.otpless.ios.app.otpless</string>
    <string>googlegmail</string>
</array>
```

2. Add the following block to your `info.plist` file (Only required if you are using the SNA feature):

```xml
<dict>
	<key>NSAllowsArbitraryLoads</key>
	<true/>
	<key>NSExceptionDomains</key>
	<dict>
		<key>80.in.safr.sekuramobile.com</key>
		<dict>
			<key>NSIncludesSubdomains</key>
			<true/>
			<key>NSTemporaryExceptionAllowsInsecureHTTPLoads</key>
			<true/>
			<key>NSTemporaryExceptionMinimumTLSVersion</key>
			<string>TLSv1.1</string>
		</dict>
		<key>partnerapi.jio.com</key>
		<dict>
			<key>NSIncludesSubdomains</key>
			<true/>
			<key>NSTemporaryExceptionAllowsInsecureHTTPLoads</key>
			<true/>
			<key>NSTemporaryExceptionMinimumTLSVersion</key>
			<string>TLSv1.1</string>
		</dict>
	</dict>
</dict>
```

3. Import the OTPLESS SDK in your respective `AppDelegate.swift` file to handle redirection:

```swift
import OtplessSwiftLP

override func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
    if OtplessSwiftLP.shared.isOtplessDeeplink(url: url){
        OtplessSwiftLP.shared.processOtplessDeeplink(url: url)
        return true
    }
	super.application(app, open: url, options: options)
	return true
}
```

---

## Step 3: Configure Sign up/Sign in

1. Import the OTPLESS package on your login page:

```typescript
import { OtplessReactNativeModule } from 'otpless-react-native-lp';
```

2. Add OTPLESS instance and initialize the SDK:

```typescript
const otplessModule = new OtplessReactNativeModule();

useEffect(() => {
    initializeModule();
    return () => {
        otplessModule.clearListener();
        otplessModule.cease();
    };
}, []);

const initializeModule = () => {
    otplessModule.initialize("APPID");
    otplessModule.setResponseCallback(onResponse);
};
```

3. Add the following code to initiate OTPLESS Login Page:

```typescript
otplessModule.start();
```

4. Add the following code to handle response callback:

```typescript
const onResponse = (data: any) => {
    let token = data.token;
    if (token !== null) {
        // Send the token to your backend and verify the token to get user details
        console.log("Token: ", token);
    } else {
        // Handle error
        console.log("Error: ", data.errorMessage);
    }
};
```

5. When user successfully logs in, stop OTPLESS:

```typescript
otplessModule.stop();
```

---

Let me know if you’d like to include sample project links or markdown for images as well!