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

  start(request: any ) {
    OtplessReactNativeLP.start(request ?? null);
  }

  stop() {
    OtplessReactNativeLP.stop();
  }

  setLogging(status: boolean) {
    OtplessReactNativeLP.setLogging(status)
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

export interface SafariCustomizationOptions  {
  preferredBarTintColor?: string;
  preferredControlTintColor?: string;
  dismissButtonStyle?: 'done' | 'cancel' | 'close';
  modalPresentationStyle?: 'automatic' | 'pageSheet' | 'formSheet' | 'overFullScreen';
};

export interface IOTPlessRequest {
  waitTime?: number;
  customTabParam?: CustomTabParam | SafariCustomizationOptions;
  extraQueryParams?: Record<string, string>;
  loadingUrl?: string;
}

export interface CustomTabParam {
  toolbarColor?: string;
  secondaryToolbarColor?: string;
  navigationBarColor?: string;
  navigationBarDividerColor?: string;
  backgroundColor?: string | null;
}

export interface LoginPageParams {
  /** milliseconds to wait (default 2000) */
  waitTime?: number;
  /** extra query params (default {}) */
  extraQueryParams?: Record<string, string>;
  /** custom tab options (default new CustomTabParam()) */
  customTabParam?: CustomTabParam;

  safariCustomizationOption?: SafariCustomizationOptions;
}

export type IOTPlessAuthCallbackErrorType =  'INITIATE' | 'VERIFY' | 'NETWORK';

export interface ICallbackSuccess {
  token: string;
  traceId: string
}

export interface ICallbackError {
  errorType: IOTPlessAuthCallbackErrorType;
  errorCode: number
  errorMessage: string
  traceId: string
}

export interface OTPlessSuccess extends ICallbackSuccess {
  status: "success";
}
export interface OTPlessError extends ICallbackError {
  status: "error";
}
export type OTPlessAuthCallback = OTPlessSuccess | OTPlessError;

export { OtplessReactNativeModule };
