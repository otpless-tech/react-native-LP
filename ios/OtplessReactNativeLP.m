#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface RCT_EXTERN_MODULE(OtplessReactNativeLP, RCTEventEmitter<RCTBridgeModule>)

RCT_EXTERN_METHOD(initialize:(NSString *)appId)

RCT_EXTERN_METHOD(setResponseCallback)

RCT_EXTERN_METHOD(start:(NSDictionary *)request)

RCT_EXTERN_METHOD(stop)

RCT_EXTERN_METHOD(setLogging:(BOOL) status)

RCT_EXTERN_METHOD(userAuthEvent:(NSString *)event
                  fallback:(BOOL)fallback
                  type:(NSString *)type
                  providerInfo:(NSDictionary<NSString *, NSString *> *)providerInfo)

@end

