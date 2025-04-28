import { NativeModules, Platform, NativeEventEmitter } from 'react-native';


const LINKING_ERROR =
  `The package 'otpless-react-native-lp' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const OtplessReactNativeLP = NativeModules.OtplessReactNativeLP
  ? NativeModules.OtplessReactNativeLP
  : new Proxy(
    {},
    {
      get() {
        throw new Error(LINKING_ERROR);
      },
    }
  );

interface OtplessResultCallback {
  (result: any): void;
}

class OtplessReactNativeModule {
  private eventEmitter: NativeEventEmitter | null = null;

  constructor() {
    this.eventEmitter = null;
  }

  clearListener() {
    this.eventEmitter?.removeAllListeners('OTPlessEventResult');
  }

  initialize(appId: String) {
    if (this.eventEmitter == null) {
      this.eventEmitter = new NativeEventEmitter(OtplessReactNativeLP);
    }
    // call the native method
    OtplessReactNativeLP.initialize(appId);
  }

  setResponseCallback(callback: OtplessResultCallback) {
    this.eventEmitter?.addListener('OTPlessEventResult', callback);
    // call the native method
    OtplessReactNativeLP.setResponseCallback()
  }

  start() {
    OtplessReactNativeLP.start();
  }

  stop() {
    OtplessReactNativeLP.stop();
  }

  // Checks if whatsapp is installed on android device
  isWhatsappInstalled(callback: (hasWhatsapp: boolean) => void) {
    if (Platform.OS === 'android') {
      OtplessReactNativeLP.isWhatsappInstalled((result: any) => {
        const hasWhatsapp = result.hasWhatsapp === true;
        callback(hasWhatsapp);
      });
      return
    }
  }
}

export { OtplessReactNativeModule };
