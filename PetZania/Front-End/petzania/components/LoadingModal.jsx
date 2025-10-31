import { StyleSheet, Text, View, Modal, BackHandler } from 'react-native'
import LottieView from 'lottie-react-native';
import React, { useEffect } from 'react'

export default function LoadingModal({
    isVisible,
    title = "Loading...",
    subtitle = "Please wait while we process your request",
    preventBackButton = true,
    ...props
}) {
    // Prevent back button from closing modal
    useEffect(() => {
        if (!isVisible || !preventBackButton) return;

        const backHandler = BackHandler.addEventListener(
            'hardwareBackPress',
            () => {
                return true;
            }
        );

        return () => backHandler.remove();
    }, [isVisible, preventBackButton]);

    return (
        <Modal
            visible={isVisible}
            transparent={true}
            animationType="fade"
            statusBarTranslucent={true}
            onRequestClose={() => {
                if (preventBackButton) {
                    return;
                }
            }}
            {...props}
        >
            <View style={styles.modalOverlay}>
                <View style={styles.modalContent}>
                    <LottieView
                        source={require("@/assets/lottie/cat_spinner.json")}
                        autoPlay
                        loop
                        style={styles.lottie}
                    />
                    <Text style={styles.modalTitle}>{title}</Text>
                    <Text style={styles.modalSubtitle}>
                        {subtitle}
                    </Text>
                </View>
            </View>
        </Modal>
    )
}

const styles = StyleSheet.create({
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0, 0, 0, 0.7)',
        justifyContent: 'center',
        alignItems: 'center',
    },

    modalContent: {
        backgroundColor: '#fff',
        borderRadius: 20,
        padding: 30,
        alignItems: 'center',
        minWidth: 280,
        maxWidth: 320,
        shadowColor: '#000',
        shadowOffset: {
            width: 0,
            height: 10,
        },
        shadowOpacity: 0.25,
        shadowRadius: 10,
        elevation: 10,
    },

    lottie: {
        width: 80,
        height: 80,
        marginBottom: 20,
    },

    modalTitle: {
        fontSize: 20,
        fontWeight: '600',
        color: '#333',
        marginBottom: 8,
        textAlign: 'center',
    },

    modalSubtitle: {
        fontSize: 14,
        color: '#666',
        textAlign: 'center',
        lineHeight: 20,
    }
})