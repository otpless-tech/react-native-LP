import React, { useEffect, useState } from 'react';
import { StyleSheet, Text, View, TouchableOpacity, ScrollView, TextInput, Platform } from 'react-native';
import { OtplessReactNativeModule, type CustomTabParam, type OTPlessSuccess, type OTPlessError, type IOTPlessRequest, type OTPlessAuthCallback } from 'otpless-react-native-lp';

export default function HeadlessPage() {
    const otplessModule = new OtplessReactNativeModule();
    const [result, setResult] = useState('');
    const [phoneNumber, setPhoneNumber] = useState('');

    const APP_ID = ""

    useEffect(() => {
        initializeModule();
        return () => {
            otplessModule.clearListener();
        };
    }, []);

    const initializeModule = () => {
        otplessModule.initialize(APP_ID).then((traceId) => {
        setResult(`traceId: ${traceId}`);
        });
        otplessModule.setWebViewInspectable();
        otplessModule.setResponseCallback(onHeadlessResult);
        otplessModule.setLogging(true);
        otplessModule.addEventObserver(onLoginPageEvent);
        otplessModule.initializeSession(APP_ID)
    };

    const onLoginPageEvent = (data: any) => {
        setResult(prev => `==== event ====\n${JSON.stringify(data)}\n======   ======"\n\n"${prev}`);
    }

    const onHeadlessResult = (data: OTPlessAuthCallback) => {
        var dataStr = ""
        if(data.status === "success") {
            const success: OTPlessSuccess = data 
            dataStr = "Success\n" +  JSON.stringify(success, null, 2) + "\n";
        } else {
            const error: OTPlessError = data
            dataStr = "Error\n" +  JSON.stringify(error, null, 2) + "\n";
        }
        const info = {"status": data.status}
        otplessModule.userAuthEvent("AUTH_SUCCESS", false, "OTPLESS", info)
        setResult(prev => `${dataStr}\n\n${prev}`);
    };

    const startHeadless = () => {
       
        const baseRequest: IOTPlessRequest = {};

        baseRequest.customTabParam = {
            ...(Platform.OS === 'ios' ? {
                    preferredBarTintColor: '#5B0171',
                    dismissButtonStyle: 'cancel',
                } : {
                toolbarColor: '#5B0171',
                navigationBarColor: '#5B0171',
                navigationBarDividerColor: '#FF3269',
                backgroundColor: '#5B0171',
            })
        };

        if(phoneNumber) {
           baseRequest.extraQueryParams = {
                "phone": phoneNumber,
                "countryCode": "91"
           } 
        }
        otplessModule.start(baseRequest);
    };

    const stopOperation = () => {
        otplessModule.stop()
    };

    const getActiveSession = async () => {
        let session = await otplessModule.getActiveSession();
        let str = JSON.stringify(session);
        setResult(prev => `${str}\n\n+${prev}`);
        console.log(str);
    }

    const logout = () => {
        otplessModule.logout();
        setResult(prev => "session logout\n\n" + prev);
    }

    return (
        <ScrollView contentContainerStyle={styles.container}>

            <TextInput
                style={styles.input}
                value={phoneNumber}
                onChangeText={setPhoneNumber}
                keyboardType="phone-pad"
                placeholder="Enter phone number"
            />

            <TouchableOpacity style={styles.primaryButton} onPress={startHeadless}>
                <Text style={styles.buttonText}>Show Otpless Login Page</Text>
            </TouchableOpacity>

            <TouchableOpacity style={styles.secondaryButton} onPress={stopOperation}>
                <Text style={styles.buttonText}>stop Operation</Text>
            </TouchableOpacity>
            <View style = {styles.row}>
                <TouchableOpacity style={styles.flexButtonPrimary} onPress={getActiveSession}>
                    <Text style={styles.buttonText}>Active Session</Text>
                </TouchableOpacity>

                <TouchableOpacity style={styles.flexButtonSecondary} onPress={logout}>
                    <Text style={styles.buttonText}>Logout</Text>
                </TouchableOpacity>
            </View>

            {result ? (
                <View style={styles.resultContainer}>
                    <Text style={styles.resultLabel}>Response:</Text>
                    <Text style={styles.resultText}>{result}</Text>
                </View>
            ) : null}
        </ScrollView>
    );
}

const styles = StyleSheet.create({
    container: {
        padding: 20,
        alignItems: 'center',
    },
    input: {
        width: '80%',
        height: 50,
        borderWidth: 1,
        borderColor: '#ccc',
        borderRadius: 8,
        paddingHorizontal: 15,
        marginBottom: 20,
        fontSize: 16,
    },
    primaryButton: {
        backgroundColor: "#007AFF",
        paddingVertical: 12,
        paddingHorizontal: 24,
        borderRadius: 30,
        marginVertical: 10,
        width: '80%',
        alignItems: 'center',
    },
    secondaryButton: {
        backgroundColor: "#FF3B30",
        paddingVertical: 12,
        paddingHorizontal: 24,
        borderRadius: 30,
        marginVertical: 10,
        width: '80%',
        alignItems: 'center',
    },
    flexButtonPrimary: {
        flex: 1,                   // each button gets equal weight
        marginHorizontal: 5,       // spacing between buttons
        backgroundColor: "#007AFF",
        paddingVertical: 12,
        borderRadius: 30,
        alignItems: "center",
    },
    flexButtonSecondary: {
        flex: 1,                   // each button gets equal weight
        marginHorizontal: 5,       // spacing between buttons
        backgroundColor: "#FF3B30",
        paddingVertical: 12,
        borderRadius: 30,
        alignItems: "center",
    },
    buttonText: {
        color: '#fff',
        fontSize: 16,
        fontWeight: 'bold',
    },
    resultContainer: {
        marginTop: 20,
        width: '100%',
        backgroundColor: '#f4f4f4',
        padding: 16,
        borderRadius: 8,
    },
    resultLabel: {
        fontWeight: 'bold',
        marginBottom: 8,
        fontSize: 16,
        color: '#333',
    },
    resultText: {
        fontSize: 14,
        color: '#444',
        fontFamily: 'Courier',
    },
    row: {
        flexDirection: "row",
        justifyContent: "space-between", // or "space-around" if you prefer
        marginVertical: 10,
    },
});
