import React, { useState, useCallback, useMemo, memo, useEffect } from 'react';
import {
  View,
  Text,
  Modal,
  TouchableOpacity,
  TextInput,
  ScrollView,
  Dimensions,
  Pressable
} from 'react-native';

const { width } = Dimensions.get('window');

const FilterModal = memo(({ visible, onClose, onApply, initialFilters = {} }) => {
  // Initialize filters with default values
  const [filters, setFilters] = useState({
    species: initialFilters.species || 'ALL',
    breed: initialFilters.breed || 'ALL',
    minAge: initialFilters.minAge || 0,
    maxAge: initialFilters.maxAge || 1000,
    sortBy: initialFilters.sortBy || 'CREATED_AT',
    sortDesc: initialFilters.sortDesc !== undefined ? initialFilters.sortDesc : true,
  });

  // Update filters when modal opens with new initialFilters
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
    }
  }, [visible, initialFilters]);

  // Filter options
  const species = useMemo(() => [
    { value: 'DOG', label: 'Dog' },
    { value: 'CAT', label: 'Cat' },
    { value: 'BIRD', label: 'Bird' },
    { value: 'FISH', label: 'Fish' },
    { value: 'HAMSTER', label: 'Hamster' },
    { value: 'RABBIT', label: 'Rabbit' },
    { value: 'GUINEA_PIG', label: 'Guinea Pig' },
    { value: 'TURTLE', label: 'Turtle' }
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
      sortBy: 'CREATED_AT',
      sortDesc: true,
    });
  }, []);

  // Apply filters
  const handleApply = useCallback(() => {
    // Validate age range
    if (filters.minAge !== null && filters.maxAge !== null && filters.minAge > filters.maxAge) {
      alert('Minimum age cannot be greater than maximum age');
      return;
    }
    
    onApply(filters);
  }, [filters, onApply]);

  // Cancel and close
  const handleCancel = useCallback(() => {
    onClose();
  }, [onClose]);

  // Render option button
  const renderOptionButton = useCallback((option, isSelected, onPress, style = {}) => (
    <TouchableOpacity
      key={option.value}
      style={[styles.optionButton, isSelected && styles.optionSelected, style]}
      onPress={onPress}
      activeOpacity={0.7}
    >
      <Text 
        style={[styles.optionText, isSelected && styles.optionTextSelected]}
        numberOfLines={1}
      >
        {option.label}
      </Text>
    </TouchableOpacity>
  ), []);

  // Responsive layout for species
  const isSmallScreen = width < 360;
  const speciesPerRow = isSmallScreen ? 2 : 3;

  return (
    <Modal
      visible={visible}
      transparent
      animationType="slide"
      onRequestClose={handleCancel}
    >
      <Pressable style={styles.modalOverlay} onPress={handleCancel}>
        <Pressable 
          style={[
            styles.modalContent, 
            isSmallScreen && styles.modalContentSmall
          ]} 
          onPress={(e) => e.stopPropagation()}
        >
          <ScrollView showsVerticalScrollIndicator={false} bounces={false}>
            {/* Header */}
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>Filters</Text>
              <TouchableOpacity onPress={handleReset} activeOpacity={0.7}>
                <Text style={styles.resetText}>Reset</Text>
              </TouchableOpacity>
            </View>

            {/* Species Filter */}
            <Text style={styles.filterLabel}>Species</Text>
            {/* First row - responsive */}
            <View style={[styles.speciesRow, isSmallScreen && styles.speciesRowSmall]}>
              {species.slice(0, speciesPerRow).map(sp =>
                renderOptionButton(
                  sp,
                  filters.species === sp.value,
                  () => handleOptionSelect('species', sp.value),
                  styles.speciesButton
                )
              )}
            </View>
            {/* Second row - responsive */}
            <View style={[styles.speciesRow, isSmallScreen && styles.speciesRowSmall]}>
              {species.slice(speciesPerRow, speciesPerRow * 2).map(sp =>
                renderOptionButton(
                  sp,
                  filters.species === sp.value,
                  () => handleOptionSelect('species', sp.value),
                  styles.speciesButton
                )
              )}
            </View>
            {/* Remaining species */}
            {species.length > speciesPerRow * 2 && (
              <View style={styles.optionsRow}>
                {species.slice(speciesPerRow * 2).map(sp =>
                  renderOptionButton(
                    sp,
                    filters.species === sp.value,
                    () => handleOptionSelect('species', sp.value)
                  )
                )}
              </View>
            )}

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
          </ScrollView>
        </Pressable>
      </Pressable>
    </Modal>
  );
});

const styles = {
  modalOverlay: { 
    flex: 1, 
    backgroundColor: 'rgba(0,0,0,0.5)', 
    justifyContent: 'center', 
    alignItems: 'center' 
  },
  modalContent: { 
    width: width * 0.9, 
    maxWidth: 500,
    maxHeight: '80%',
    backgroundColor: '#fff', 
    borderRadius: 12, 
    padding: 20 
  },
  modalContentSmall: {
    padding: 16,
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 20
  },
  modalTitle: { 
    fontSize: 20, 
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
  speciesRowSmall: {
    gap: 6,
  },
  speciesButton: {
    flex: 1,
    paddingHorizontal: 8,
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
  optionText: {
    fontSize: 14,
    color: '#333',
    textAlign: 'center'
  },
  optionTextSelected: {
    color: '#fff',
    fontWeight: '500'
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