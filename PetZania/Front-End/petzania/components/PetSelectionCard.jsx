import React from 'react';
import { StyleSheet, Text, View, TouchableOpacity } from 'react-native';
import { Image } from 'expo-image';

const PetSelectionCard = ({ 
    item,
    isSelected,
    onPress,
    imageStyle,
    cardStyle,
    textStyle
}) => {
    return (
        <TouchableOpacity
            onPress={() => onPress(item.value || item.name)}
            activeOpacity={0.95}
            style={styles.cardWrapper}
        >
            <View style={[
                styles.card,
                isSelected && styles.selectedCard,
                cardStyle
            ]}>
                <Image
                    source={item.image}
                    style={[styles.image, imageStyle]}
                    resizeMode="contain"
                />
                <Text style={[
                    styles.text, 
                    isSelected && styles.selectedText,
                    textStyle
                ]}>
                    {item.name}
                </Text>
            </View>
        </TouchableOpacity>
    );
};

const styles = StyleSheet.create({
    cardWrapper: {
        flex: 1,
        marginHorizontal: 5,
    },
    card: {
        borderRadius: 16,
        alignItems: 'center',
        paddingVertical: 15,
        backgroundColor: '#fff',
        borderWidth: 0.2,
        borderColor: '#e0e0e0',
        // iOS shadow
        shadowColor: '#000',
        shadowOffset: {
            width: 0,
            height: 2,
        },
        shadowOpacity: 0.1,
        shadowRadius: 3.84,
        // Android shadow
        elevation: 2,
    },
    selectedCard: {
        borderWidth: 2,
        borderColor: '#9188E5',
    },
    image: {
        width: 140,
        height: 140,
        marginBottom: 10,
    },
    text: {
        fontSize: 16,
        fontWeight: '600',
        color: '#333',
        textAlign: 'center',
    },
    selectedText: {
        color: '#9188E5',
    },
});

export default PetSelectionCard;
