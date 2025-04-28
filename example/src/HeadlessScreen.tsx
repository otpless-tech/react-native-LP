import React, { useEffect, useState } from 'react';
import { StyleSheet, Text, View, TouchableOpacity, ScrollView } from 'react-native';
import { OtplessReactNativeModule } from 'otpless-react-native-lp';

export default function HeadlessPage() {
    const otplessModule = new OtplessReactNativeModule();
    const [result, setResult] = useState('');

    useEffect(() => {
        initializeModule();
        return () => {
            otplessModule.clearListener();
        };
    }, []);

    const initializeModule = () => {
        otplessModule.initialize("H7A18MQGF2DLZY7PIJRQ");
        otplessModule.setResponseCallback(onHeadlessResult);
    };

    const onHeadlessResult = (data: any) => {
        const dataStr = JSON.stringify(data, null, 2);
        setResult(dataStr);
    };

    const startHeadless = () => {
        otplessModule.start();
    };

    const stopOperation = () => {
        otplessModule.stop()
    };

    return (
        <ScrollView contentContainerStyle={styles.container}>
            <TouchableOpacity style={styles.primaryButton} onPress={startHeadless}>
                <Text style={styles.buttonText}>Show Otpless Login Page</Text>
            </TouchableOpacity>

            <TouchableOpacity style={styles.secondaryButton} onPress={stopOperation}>
                <Text style={styles.buttonText}>stop Operation</Text>
            </TouchableOpacity>

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
});
