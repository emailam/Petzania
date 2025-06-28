import React, { useState } from 'react';
import ImageViewing from 'react-native-image-viewing';
import { useRouter, useLocalSearchParams } from 'expo-router';

export default function ImageViewerScreen() {
    const router = useRouter();
    const { imageuri } = useLocalSearchParams();
    const [visible, setVisible] = useState(true);

    // Decode the URI to handle special characters
    const decodedImageUri = imageuri ? decodeURIComponent(imageuri) : '';

    const handleClose = () => {
        setVisible(false);
        router.back();
    };

    if (!decodedImageUri) {
        router.back();
        return null;
    }

    return (
        <ImageViewing
            images={[{ uri: decodedImageUri }]}
            imageIndex={0}
            visible={visible}
            onRequestClose={handleClose}
            backgroundColor="#000000"
        />
    );
}