import React, { useState, useCallback, useMemo, memo, useEffect, useRef } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  TextInput,
  Dimensions,
  BackHandler,
  FlatList,
} from 'react-native';

import {
  BottomSheetModal,
  BottomSheetScrollView,
  BottomSheetBackdrop,
} from '@gorhom/bottom-sheet';

import { Ionicons } from '@expo/vector-icons';

const { width } = Dimensions.get('window');

const FilterModal = memo(({ visible, onClose, onApply, initialFilters = {} }) => {
  const bottomSheetModalRef = useRef(null);

  // Initialize filters with default values
  const [filters, setFilters] = useState({
    species: initialFilters.species || 'ALL',
    breed: initialFilters.breed || 'ALL',
    minAge: initialFilters.minAge || 0,
    maxAge: initialFilters.maxAge || 1000,
    sortBy: initialFilters.sortBy || 'CREATED_AT',
    sortDesc: initialFilters.sortDesc !== undefined ? initialFilters.sortDesc : true,
  });

  // Handle modal visibility changes
  useEffect(() => {
    if (visible) {
      setFilters({
        species: initialFilters.species || 'ALL',
        breed: initialFilters.breed || 'ALL',
        minAge: initialFilters.minAge || 0,
        maxAge: initialFilters.maxAge || 1000,
        sortBy: initialFilters.sortBy || 'CREATED_AT',
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

  // Filter options
  const species = useMemo(() => [
    { value: 'DOG', label: 'Dog', icon: 'paw-outline' },
    { value: 'CAT', label: 'Cat', icon: 'paw-outline' },
    { value: 'BIRD', label: 'Bird', icon: 'airplane-outline' },
    { value: 'FISH', label: 'Fish', icon: 'fish-outline' },
    { value: 'HAMSTER', label: 'Hamster', icon: 'ellipse-outline' },
    { value: 'RABBIT', label: 'Rabbit', icon: 'ear-outline' },
    { value: 'LIZARD', label: 'Lizard', icon: 'bug-outline' },
    { value: 'TURTLE', label: 'Turtle', icon: 'shield-outline' }
  ], []);

  const sortOptions = useMemo(() => [
    { value: 'CREATED_AT', label: 'Date' },
    { value: 'REACTS', label: 'Likes' }
  ], []);

  // Handle filter changes
  const handleFilterChange = useCallback((key, value) => {
    setFilters(prev => ({ ...prev, [key]: value }));
  }, []);

  // Handle option selection - toggle between value and 'ALL'
  const handleOptionSelect = useCallback((key, value) => {
    setFilters(prev => ({
      ...prev,
      [key]: prev[key] === value ? 'ALL' : value
    }));
  }, []);

  // Handle sort order toggle
  const handleSortOrderToggle = useCallback(() => {
    setFilters(prev => ({ ...prev, sortDesc: !prev.sortDesc }));
  }, []);

  // Handle age input change with validation
  const handleAgeChange = useCallback((key, value) => {
    // Allow empty string or valid numbers
    if (value === '') {
      setFilters(prev => ({ ...prev, [key]: null }));
    } else if (/^\d+$/.test(value)) {
      setFilters(prev => ({ ...prev, [key]: parseInt(value, 10) }));
    }
  }, []);

  // Reset all filters
  const handleReset = useCallback(() => {
    setFilters({
      species: 'ALL',
      breed: 'ALL',
      minAge: 0,
      maxAge: 1000,
      sortBy: 'SCORE',
      sortDesc: true,
    });
  }, []);

  // Cancel and close
  const handleCancel = useCallback(() => {
    bottomSheetModalRef.current?.dismiss();
  }, []);

  // Apply filters
  const handleApply = useCallback(() => {
    // Validate age range
    if (filters.minAge !== null && filters.maxAge !== null && filters.minAge > filters.maxAge) {
      alert('Minimum age cannot be greater than maximum age');
      return;
    }
    
    onApply(filters);
    bottomSheetModalRef.current?.dismiss();
  }, [filters, onApply]);

  // Render option button
  const renderOptionButton = useCallback((option, isSelected, onPress, style = {}) => (
    <TouchableOpacity
      key={option.value}
      style={[styles.optionButton, isSelected && styles.optionSelected, style]}
      onPress={onPress}
      activeOpacity={0.7}
    >
      <View style={styles.optionContent}>
        <Text 
          style={[styles.optionText, isSelected && styles.optionTextSelected]}
          numberOfLines={1}
        >
          {option.label}
        </Text>
      </View>
    </TouchableOpacity>
  ), []);

  // Render species item for FlatList
  const renderSpeciesItem = useCallback(({ item }) => {
    const isSelected = filters.species === item.value;
    return renderOptionButton(
      item,
      isSelected,
      () => handleOptionSelect('species', item.value),
      styles.speciesItemButton
    );
  }, [filters.species, handleOptionSelect, renderOptionButton]);

  // Responsive layout for species
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
    >
      <BottomSheetScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.scrollContent}
      >
        {/* Header */}
        <View style={styles.modalHeader}>
          <Text style={styles.modalTitle}>Filters</Text>
          <TouchableOpacity onPress={handleReset} activeOpacity={0.7}>
            <Text style={styles.resetText}>Reset</Text>
          </TouchableOpacity>
        </View>

          {/* Species Filter */}
          <Text style={styles.filterLabel}>Species</Text>
          <FlatList
            data={species}
            renderItem={renderSpeciesItem}
            keyExtractor={(item) => item.value}
            numColumns={3}
            scrollEnabled={false}
            contentContainerStyle={styles.speciesFlatList}
            columnWrapperStyle={styles.speciesRow}
          />

          {/* Breed */}
          <Text style={styles.filterLabel}>Breed</Text>
          <TextInput
            style={[styles.input, isSmallScreen && styles.inputSmall]}
            placeholder="Enter breed or leave empty for all"
            value={filters.breed === 'ALL' ? '' : filters.breed}
            onChangeText={(text) => handleFilterChange('breed', text || 'ALL')}
            placeholderTextColor="#999"
          />

          {/* Age Range */}
          <Text style={styles.filterLabel}>Age Range (years)</Text>
          <View style={styles.rangeRow}>
            <TextInput
              style={[styles.input, styles.rangeInput, isSmallScreen && styles.inputSmall]}
              placeholder="Min"
              value={filters.minAge !== null ? String(filters.minAge) : ''}
              onChangeText={(text) => handleAgeChange('minAge', text)}
              keyboardType="numeric"
              placeholderTextColor="#999"
            />
            <TextInput
              style={[styles.input, styles.rangeInput, isSmallScreen && styles.inputSmall]}
              placeholder="Max"
              value={filters.maxAge !== null ? String(filters.maxAge) : ''}
              onChangeText={(text) => handleAgeChange('maxAge', text)}
              keyboardType="numeric"
              placeholderTextColor="#999"
            />
          </View>

          {/* Sort Options */}
          <Text style={styles.filterLabel}>Sort By</Text>
          <View style={styles.optionsRow}>
            {sortOptions.map(sort =>
              renderOptionButton(
                sort,
                filters.sortBy === sort.value,
                () => handleFilterChange('sortBy', sort.value)
              )
            )}
          </View>

          {/* Sort Order */}
          <TouchableOpacity 
            style={styles.sortOrderContainer}
            onPress={handleSortOrderToggle}
            activeOpacity={0.7}
          >
            <View style={[styles.optionButton, filters.sortDesc && styles.optionSelected]}>
              <Text style={[styles.optionText, filters.sortDesc && styles.optionTextSelected]}>
                {filters.sortDesc ? 'Newest/Most First' : 'Oldest/Least First'}
              </Text>
            </View>
          </TouchableOpacity>

          {/* Action Buttons */}
          <View style={[styles.modalButtons, isSmallScreen && styles.modalButtonsSmall]}>
            <TouchableOpacity
              style={[styles.button, styles.cancelButton]}
              onPress={handleCancel}
              activeOpacity={0.8}
            >
              <Text style={styles.cancelButtonText}>Cancel</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.button, styles.applyButton]}
              onPress={handleApply}
              activeOpacity={0.8}
            >
              <Text style={styles.applyButtonText}>Apply Filters</Text>
            </TouchableOpacity>
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
  bottomSheetIndicator: {
    backgroundColor: '#ddd',
  },
  bottomSheetContent: {
    flex: 1,
    paddingHorizontal: 20,
  },
  scrollContent: {
    paddingBottom: 20,
    paddingHorizontal: 20,
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 20,
    paddingTop: 10,
  },
  modalTitle: {
    fontSize: 26,
    fontWeight: '600',
    color: '#333'
  },
  resetText: {
    fontSize: 16,
    color: '#9188E5',
    fontWeight: '500'
  },
  filterLabel: {
    fontSize: 16,
    fontWeight: '500',
    color: '#333',
    marginTop: 16,
    marginBottom: 8
  },
  optionsRow: { 
    flexDirection: 'row', 
    marginBottom: 8,
    flexWrap: 'wrap'
  },
  speciesRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 8,
    gap: 8,
  },
  optionButton: { 
    borderWidth: 1, 
    borderColor: '#ddd', 
    borderRadius: 20, 
    paddingVertical: 8, 
    paddingHorizontal: 16, 
    marginRight: 8,
    marginBottom: 8,
    backgroundColor: '#fff'
  },
  optionSelected: { 
    backgroundColor: '#9188E5',
    borderColor: '#9188E5'
  },
  optionContent: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  optionIcon: {
    marginRight: 6,
  },
  optionText: {
    fontSize: 14,
    color: '#333',
    textAlign: 'center'
  },
  optionTextSelected: {
    color: '#fff',
    fontWeight: '500'
  },
  speciesFlatList: {
    marginBottom: 8,
  },
  speciesItemButton: {
    flex: 1,
    marginHorizontal: 4,
  },
  input: { 
    borderWidth: 1, 
    borderColor: '#ddd', 
    borderRadius: 8, 
    padding: 12, 
    marginBottom: 8,
    backgroundColor: '#fff',
    fontSize: 16
  },
  inputSmall: {
    padding: 10,
    fontSize: 14,
  },
  rangeRow: { 
    flexDirection: 'row', 
    justifyContent: 'space-between',
    marginBottom: 8,
    gap: 8
  },
  rangeInput: { 
    flex: 1
  },
  modalButtons: { 
    flexDirection: 'row', 
    justifyContent: 'space-between', 
    marginTop: 24,
    gap: 12
  },
  modalButtonsSmall: {
    marginTop: 20,
    gap: 8,
  },
  button: {
    flex: 1,
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center'
  },
  cancelButton: {
    backgroundColor: '#f0f0f0',
    borderWidth: 1,
    borderColor: '#ddd'
  },
  cancelButtonText: {
    fontSize: 16,
    color: '#333',
    fontWeight: '500'
  },
  applyButton: {
    backgroundColor: '#9188E5'
  },
  applyButtonText: {
    fontSize: 16,
    color: '#fff',
    fontWeight: '600'
  },
  sortOrderContainer: {
    marginTop: 8,
  }
};

export default FilterModal;