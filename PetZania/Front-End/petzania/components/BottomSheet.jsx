import { forwardRef, useCallback } from "react";
import { BottomSheetModal, BottomSheetView, BottomSheetBackdrop } from '@gorhom/bottom-sheet';

const BottomSheet = forwardRef(({ children, snapPoints = ['50%'], ...props }, ref) => {
    const renderBackdrop = useCallback((backdropProps) => (
        <BottomSheetBackdrop
            {...backdropProps}
            disappearsOnIndex={-1}
            appearsOnIndex={0}
            opacity={0.5}
        />
    ), []);

    return (
        <BottomSheetModal
            ref={ref}
            snapPoints={snapPoints}
            backdropComponent={renderBackdrop}
            backgroundStyle={{
                backgroundColor: '#fff',
                borderTopLeftRadius: 20,
                borderTopRightRadius: 20,
            }}
            handleIndicatorStyle={{
                backgroundColor: '#9188E5',
                width: 40,
            }}
            {...props}
        >
            <BottomSheetView style={{ flex: 1, paddingHorizontal: 20 }}>
                {children}
            </BottomSheetView>
        </BottomSheetModal>
    );
});

export default BottomSheet;