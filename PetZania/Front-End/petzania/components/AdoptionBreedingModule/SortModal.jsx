import React, { useState, useCallback, useMemo, memo, useEffect, useRef } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  Dimensions,
  BackHandler,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';

import {
  BottomSheetModal,
  BottomSheetScrollView,
  BottomSheetBackdrop,
} from '@gorhom/bottom-sheet';

const { width } = Dimensions.get('window');

const SortModal = memo(({ visible, onClose, onApply, initialFilters = {} }) => {
  const bottomSheetModalRef = useRef(null);

  // Initialize sort filters
  const [sortFilters, setSortFilters] = useState({
    sortBy: initialFilters.sortBy || 'SCORE',
    sortDesc: initialFilters.sortDesc !== undefined ? initialFilters.sortDesc : true,
  });

  // Handle modal visibility changes
  useEffect(() => {
    if (visible) {
      setSortFilters({
        sortBy: initialFilters.sortBy || 'SCORE',
        sortDesc: initialFilters.sortDesc !== undefined ? initialFilters.sortDesc : true,
      });
      bottomSheetModalRef.current?.present();
    } else {
      bottomSheetModalRef.current?.dismiss();
    }
  }, [visible, initialFilters]);

  // Handle Android back button
  useEffect(() => {
    const backAction = () => {
      if (visible) {
        onClose?.();
        return true;
      }
      return false;
    };

    const backHandler = BackHandler.addEventListener('hardwareBackPress', backAction);
    return () => backHandler.remove();
  }, [visible, onClose]);

  // Handle modal dismiss
  const handleSheetChanges = useCallback((index) => {
    if (index === -1) {
      onClose?.();
    }
  }, [onClose]);

  // Backdrop component
  const renderBackdrop = useCallback(
    (props) => (
      <BottomSheetBackdrop
        {...props}
        disappearsOnIndex={-1}
        appearsOnIndex={0}
        opacity={0.5}
        pressBehavior="close"
      />
    ),
    []
  );

  // Sort options with 4 combinations
  const sortOptions = useMemo(() => [
    {
      key: 'score',
      sortBy: 'SCORE',
      sortDesc: true,
      label: 'Default',
      icon: 'star-outline'
    },
    {
      key: 'newest',
      sortBy: 'CREATED_DATE',
      sortDesc: true,
      label: 'Newest First',
      icon: 'time-outline'
    },
    {
      key: 'oldest',
      sortBy: 'CREATED_DATE',
      sortDesc: false,
      label: 'Oldest First',
      icon: 'time-outline'
    },
    {
      key: 'most_liked',
      sortBy: 'REACTS',
      sortDesc: true,
      label: 'Most Liked',
      icon: 'heart-outline'
    },
    {
      key: 'least_liked',
      sortBy: 'REACTS',
      sortDesc: false,
      label: 'Least Liked',
      icon: 'heart-outline'
    }
  ], []);

  // Get current selection key
  const getCurrentSelectionKey = useCallback(() => {
    return sortOptions.find(option =>
      option.sortBy === sortFilters.sortBy && option.sortDesc === sortFilters.sortDesc
    )?.key || null;
  }, [sortFilters, sortOptions]);

  // Handle sort option selection - apply immediately
  const handleSortOptionSelect = useCallback((option) => {
    const newSortFilters = {
      sortBy: option.sortBy,
      sortDesc: option.sortDesc,
    };
    
    setSortFilters(newSortFilters);
    
    // Apply immediately
    const updatedFilters = {
      ...initialFilters,
      ...newSortFilters,
    };
    
    onApply(updatedFilters);
  }, [initialFilters, onApply]);

  // Clear all - reset to default and apply immediately
  const handleClearAll = useCallback(() => {
    const defaultSortFilters = {
      sortBy: 'SCORE',
      sortDesc: true,
    };
    
    setSortFilters(defaultSortFilters);
    
    // Apply immediately
    const updatedFilters = {
      ...initialFilters,
      ...defaultSortFilters,
    };
    
    onApply(updatedFilters);
  }, [initialFilters, onApply]);

  // Cancel and close
  const handleCancel = useCallback(() => {
    bottomSheetModalRef.current?.dismiss();
  }, []);

  // Render sort option
  const renderSortOption = useCallback((option) => {
    const isSelected = getCurrentSelectionKey() === option.key;
    
    return (
      <TouchableOpacity
        key={option.key}
        style={[styles.sortOption, isSelected && styles.sortOptionSelected]}
        onPress={() => handleSortOptionSelect(option)}
        activeOpacity={0.7}
      >
        <View style={styles.sortOptionLeft}>
          <Ionicons 
            name={option.icon} 
            size={20} 
            color={isSelected ? '#9188E5' : '#666'} 
            style={styles.sortOptionIcon}
          />
          <Text style={[styles.sortOptionText, isSelected && styles.sortOptionTextSelected]}>
            {option.label}
          </Text>
        </View>
        {isSelected && (
          <Ionicons name="checkmark" size={20} color="#9188E5" />
        )}
      </TouchableOpacity>
    );
  }, [getCurrentSelectionKey, handleSortOptionSelect]);

  const isSmallScreen = width < 360;

  return (
    <BottomSheetModal
      ref={bottomSheetModalRef}
      onChange={handleSheetChanges}
      backdropComponent={renderBackdrop}
      enablePanDownToClose={true}
      enableDismissOnClose={true}
      handleIndicatorStyle={styles.handleIndicator}
      backgroundStyle={styles.bottomSheetBackground}
      snapPoints={['50%']}
    >
      <BottomSheetScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.scrollContent}
      >
        {/* Header */}
        <View style={styles.modalHeader}>
          <Text style={styles.modalTitle}>Sort</Text>
          <TouchableOpacity onPress={handleClearAll} activeOpacity={0.7}>
            <Text style={styles.clearAllText}>Clear All</Text>
          </TouchableOpacity>
        </View>

        {/* Sort Options */}
        <View style={styles.sortOptionsContainer}>
          {sortOptions.map(renderSortOption)}
        </View>
      </BottomSheetScrollView>
    </BottomSheetModal>
  );
});

const styles = {
  bottomSheetBackground: {
    backgroundColor: '#fff',
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
  },
  handleIndicator: {
    backgroundColor: '#9188E5',
    width: 40,
  },
  scrollContent: {
    paddingBottom: 20,
    paddingHorizontal: 20,
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 24,
    paddingTop: 10,
  },
  modalTitle: {
    fontSize: 26,
    fontWeight: '600',
    color: '#333'
  },
  clearAllText: {
    fontSize: 16,
    color: '#9188E5',
    fontWeight: '500'
  },
  sortOptionsContainer: {
    marginBottom: 24,
  },
  sortOption: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 16,
    paddingHorizontal: 16,
    borderRadius: 12,
    marginBottom: 8,
    backgroundColor: '#f8f9fa',
    borderWidth: 1,
    borderColor: '#e9ecef',
  },
  sortOptionSelected: {
    backgroundColor: '#f0f0ff',
    borderColor: '#9188E5',
  },
  sortOptionLeft: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  sortOptionIcon: {
    marginRight: 12,
  },
  sortOptionText: {
    fontSize: 16,
    color: '#333',
    fontWeight: '400',
  },
  sortOptionTextSelected: {
    color: '#9188E5',
    fontWeight: '500',
  },
  modalButtons: { 
    flexDirection: 'row', 
    justifyContent: 'center', 
    marginTop: 24,
  },
  modalButtonsSmall: {
    marginTop: 20,
  },
  button: {
    paddingHorizontal: 32,
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center'
  },
  closeButton: {
    backgroundColor: '#9188E5'
  },
  closeButtonText: {
    fontSize: 16,
    color: '#fff',
    fontWeight: '600'
  },
};

export default SortModal;