import React, { useState, useCallback } from 'react';
import {
  View,
  Text,
  Modal,
  TouchableOpacity,
  TextInput,
  ScrollView,
  Dimensions,
  Pressable,
  Alert,
  StyleSheet
} from 'react-native';
import CategoryButton from './CategoryButton';
import { RotateCcw  } from 'lucide-react-native';
import MaterialCommunityIcons from '@expo/vector-icons/MaterialCommunityIcons';
import { LinearGradient } from 'expo-linear-gradient';
const { width, height } = Dimensions.get('window');
import FontAwesome5 from '@expo/vector-icons/FontAwesome5';
import FontAwesome6 from '@expo/vector-icons/FontAwesome6';
import Entypo from '@expo/vector-icons/Entypo';

export default function FilterModal({ visible, onClose, onApply, initialFilters = {} }) {
  // Initialize filters
  const [filters, setFilters] = useState({
    species: initialFilters.species || 'ALL',
    breed: initialFilters.breed || 'ALL',
    minAge: initialFilters.minAge != null ? initialFilters.minAge : 0,
    maxAge: initialFilters.maxAge != null ? initialFilters.maxAge : 100,
    sortBy: initialFilters.sortBy || 'CREATED_DATE',
    sortDesc: initialFilters.sortDesc != null ? initialFilters.sortDesc : true,
  });

  const speciesOptions = [
    { value: 'DOG', label: 'Dog', icon: <FontAwesome5 name="dog" size={20} color="#4a4a4a" /> },
    { value: 'CAT', label: 'Cat', icon: <FontAwesome5 name="cat" size={20} color="#4a4a4a" /> },
    { value: 'BIRD', label: 'Bird', icon: <Entypo name="twitter" size={20} color="#4a4a4a" /> },
    { value: 'FISH', label: 'Fish', icon: <FontAwesome6 name="fish" size={20} color="#4a4a4a" /> },
    { value: 'RABBIT', label: 'Rabbit', icon: <MaterialCommunityIcons name="rabbit" size={24} color="#4a4a4a" /> },
  ];

  const sortOptions = [
    { value: 'CREATED_AT', label: 'Date' },
    { value: 'REACTS', label: 'Likes' }
  ];

  // Handlers
  const handleFilterChange = useCallback((key, value) => {
    setFilters(prev => ({ ...prev, [key]: value }));
  }, []);

  const handleAgeChange = useCallback((key, text) => {
    const num = parseInt(text, 10);
    if (isNaN(num) || num < 0) return;
    setFilters(prev => ({ ...prev, [key]: num }));
  }, []);

  const handleOptionSelect = useCallback((key, value) => {
    setFilters(prev => ({ ...prev, [key]: value }));
  }, []);

  const handleSortOrderToggle = useCallback(() => {
    setFilters(prev => ({ ...prev, sortDesc: !prev.sortDesc }));
  }, []);

  const handleReset = useCallback(() => {
    setFilters({
      species: 'ALL',
      breed: 'ALL',
      minAge: 0,
      maxAge: 100,
      sortBy: 'CREATED_DATE',
      sortDesc: true,
    });
  }, []);

  const handleApply = useCallback(() => {
    if (filters.minAge > filters.maxAge) {
      Alert.alert('Error', 'Minimum age cannot be greater than maximum age');
      return;
    }
    onApply(filters);
  }, [filters, onApply]);

  const isSmallScreen = width < 360;

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <Pressable style={styles.modalOverlay} onPress={onClose}>
        <Pressable style={[styles.modalContent, isSmallScreen && styles.modalContentSmall]} onPress={e => e.stopPropagation()}>
          <View style={styles.headerRow}>
            <Text style={styles.title}>Filters</Text>
            <TouchableOpacity style={styles.resetButton} onPress={handleReset}>
              <RotateCcw size={16} color="#7C3AED" />
              <Text style={styles.resetText}>Reset</Text>
            </TouchableOpacity>
          </View>

          <ScrollView showsVerticalScrollIndicator={false}>
            <Text style={styles.label}>Species</Text>
            <View style={styles.speciesGrid}>
              {speciesOptions.map(opt => (
                <View key={opt.value} style={styles.speciesButtonWrapper}>
                  <CategoryButton
                    title={opt.label}
                    onPress={() => handleOptionSelect('species', opt.value)}
                    style={[
                      styles.category,
                      filters.species === opt.value && styles.categorySelected
                    ]}
                    icon={opt.icon}
                  />
                </View>
              ))}
            </View>

            <Text style={styles.label}>Breed</Text>
            <TextInput
              style={styles.input}
              placeholder="Enter breed"
              value={filters.breed === 'ALL' ? '' : filters.breed}
              onChangeText={text => handleFilterChange('breed', text || 'ALL')}
              placeholderTextColor="#999"
            />

            <Text style={styles.label}>Age Range (months)</Text>
            <View style={styles.rangeRow}>
              <TextInput
                style={[styles.input, styles.rangeInput]}
                placeholder="Min"
                keyboardType="numeric"
                value={String(filters.minAge)}
                onChangeText={text => handleAgeChange('minAge', text)}
                placeholderTextColor="#999"
              />
              <TextInput
                style={[styles.input, styles.rangeInput]}
                placeholder="Max"
                keyboardType="numeric"
                value={String(filters.maxAge)}
                onChangeText={text => handleAgeChange('maxAge', text)}
                placeholderTextColor="#999"
              />
            </View>

            <Text style={styles.label}>Sort By</Text>
            <View style={styles.optionsRow}>
              {sortOptions.map(opt => (
                <TouchableOpacity
                  key={opt.value}
                  style={[
                    styles.option,
                    filters.sortBy === opt.value && styles.optionSelected
                  ]}
                  onPress={() => handleFilterChange('sortBy', opt.value)}
                >
                  <Text style={[
                    styles.optionText,
                    filters.sortBy === opt.value && styles.optionTextSelected
                  ]}>
                    {opt.label}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>

            <TouchableOpacity style={styles.sortOrder} onPress={handleSortOrderToggle}>
              <Text style={styles.optionText}>
                {filters.sortDesc ? 'Newest First' : 'Oldest First'}
              </Text>
            </TouchableOpacity>

            <View style={styles.buttonRow}>
              <TouchableOpacity style={[styles.button, styles.cancelBtn]} onPress={onClose}>
                <Text style={styles.cancelText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity style={[styles.button, styles.applyBtn]} onPress={handleApply}>
                <Text style={styles.applyText}>Apply</Text>
              </TouchableOpacity>
            </View>
          </ScrollView>
        </Pressable>
      </Pressable>
    </Modal>
  );
}

const styles = StyleSheet.create({
  optionSelected: { 
  backgroundColor: '#9188E5',
  borderColor: '#9188E5',
},
  modalOverlay: { 
    flex: 1, 
    backgroundColor: 'rgba(0,0,0,0.6)', 
    justifyContent: 'center', 
    alignItems: 'center' 
  },
  modalContent: { 
    width: width * 0.9, 
    maxWidth: 500,
    maxHeight: height * 0.85, 
    backgroundColor: '#ffffff', 
    borderRadius: 20, 
    padding: 24,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 10 },
    shadowOpacity: 0.15,
    shadowRadius: 20,
    elevation: 10,
  },
  modalContentSmall: { 
    padding: 16,
    width: width * 0.95,
  },
  headerRow: { 
    flexDirection: 'row', 
    justifyContent: 'space-between', 
    alignItems: 'center', 
    marginBottom: 20,
    paddingBottom: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  title: { 
    fontSize: 24, 
    fontWeight: '700', 
    color: '#1a1a1a',
    letterSpacing: -0.5,
  },
  reset: { 
    fontSize: 16, 
    color: '#9188E5',
    fontWeight: '600',
  },
  label: { 
    fontSize: 16, 
    fontWeight: '600', 
    marginTop: 16, 
    marginBottom: 10, 
    color: '#2d2d2d',
    letterSpacing: -0.2,
  },
  speciesGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginHorizontal: 0,
    marginBottom: 8,
  },
  speciesButtonWrapper: {
    width: '33.33%',
    paddingHorizontal: 4,
    marginBottom: 8,
  },
  optionsRow: { 
    flexDirection: 'row', 
    flexWrap: 'wrap', 
    marginBottom: 8,
    marginHorizontal: 2,
  },
  category: { 
    paddingHorizontal: 14, 
    paddingVertical: 10, 
    backgroundColor: '#f8f8f8',
    borderRadius: 12,
    borderWidth: 1.5,
    borderColor: '#e8e8e8',
    minHeight: 38,
    width: '100%',
  },
  categorySelected: { 
    backgroundColor: '#9188E5',
    borderColor: '#9188E5',
  },
    input: { 
    borderWidth: 1.5, 
    borderColor: '#e0e0e0', 
    borderRadius: 12, 
    padding: 14, 
    marginBottom: 8, 
    fontSize: 16, 
    backgroundColor: '#fafafa',
    color: '#333',
  },
  rangeRow: { 
    flexDirection: 'row', 
    justifyContent: 'space-between', 
    marginBottom: 8,
  },
  rangeInput: { 
    flex: 1,
    marginHorizontal: 4,
    textAlign: 'center',
  },
  option: { 
    borderWidth: 1.5, 
    borderColor: '#e0e0e0', 
    borderRadius: 12, 
    paddingHorizontal: 20, 
    paddingVertical: 12, 
    marginRight: 10, 
    marginBottom: 10,
    backgroundColor: '#fafafa',
    minWidth: 80,
    alignItems: 'center',
    overflow: 'hidden',
    position: 'relative',
  },
  optionText: { 
    fontSize: 14, 
    color: '#4a4a4a', 
    textAlign: 'center',
    fontWeight: '500',
    zIndex: 1,
  },
  optionTextSelected: { 
    color: '#ffffff', 
    fontWeight: '600' 
  },
  sortOrder: { 
    marginBottom: 16,
    backgroundColor: '#f0f0f0',
    padding: 12,
    borderRadius: 10,
    alignItems: 'center',
    marginTop: 4,
  },
  buttonRow: { 
    flexDirection: 'row', 
    justifyContent: 'space-between', 
    marginTop: 24,
    marginBottom: 8,
  },
  button: { 
    flex: 1, 
    paddingVertical: 14, 
    borderRadius: 12, 
    alignItems: 'center',
    marginHorizontal: 5,
    overflow: 'hidden',
    position: 'relative',
  },
  cancelBtn: { 
    backgroundColor: '#f5f5f5', 
    borderWidth: 1.5, 
    borderColor: '#e0e0e0' 
  },
  cancelText: { 
    fontSize: 16, 
    color: '#666',
    fontWeight: '600',
  },
  applyBtn: { 
  backgroundColor: '#9188E5',
  shadowColor: '#9188E5',
  shadowOffset: { width: 0, height: 4 },
  shadowOpacity: 0.3,
  shadowRadius: 8,
  elevation: 5,
},
  applyText: { 
    fontSize: 16, 
    color: '#fff', 
    fontWeight: '700',
    zIndex: 1,
  },
  resetButton: {
  flexDirection: 'row',
  alignItems: 'center',
  gap: 8,
  paddingHorizontal: 12,
  paddingVertical: 8,
  backgroundColor: '#EDE9FE', // purple-100
  borderRadius: 8,
},
resetText: {
  fontSize: 14,
  fontWeight: '500',
  color: '#7C3AED', // purple-600
},
});