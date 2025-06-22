import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  Dimensions,
  TouchableOpacity,
  Modal,
  TextInput,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import CategoryButton from './CategoryButton'; // adjust path

const { width } = Dimensions.get('window');

const PostFiltering = ({
  modalVisible,
  setModalVisible,
  tempFilters,
  setTempFilters,
  onApplyFilters,
  onResetFilters,
}) => {
  const updateTempFilter = (key, value) => {
    setTempFilters(prev => ({ ...prev, [key]: value }));
  };

  return (
    <Modal visible={modalVisible} animationType="slide" transparent>
      <View style={styles.modalOverlay}>
        <View style={styles.modalContent}>
          <View style={styles.modalHeader}>
            <Text style={styles.modalTitle}>Filters</Text>
            <TouchableOpacity onPress={onResetFilters}>
              <Text style={styles.resetText}>Reset</Text>
            </TouchableOpacity>
          </View>

          <Text style={styles.filterLabel}>Category</Text>
          <View style={styles.categoryRow}>
            {[
              { 
                value: 'All', 
                title: 'All Pets', 
                icon: <Ionicons name="heart-outline" size={24} color={tempFilters.category === 'All' ? '#9188E5' : '#666'} />
              },
              { 
                value: 'Dog', 
                title: 'Dogs', 
                icon: <Ionicons name="paw-outline" size={24} color={tempFilters.category === 'Dog' ? '#9188E5' : '#666'} />
              },
              { 
                value: 'Cat', 
                title: 'Cats', 
                icon: <Ionicons name="fish-outline" size={24} color={tempFilters.category === 'Cat' ? '#9188E5' : '#666'} />
              }
            ].map(category => (
              <CategoryButton
                key={category.value}
                icon={category.icon}
                title={category.title}
                onPress={() => updateTempFilter('category', category.value)}
                style={[
                  styles.categoryButton,
                  tempFilters.category === category.value && styles.categorySelected
                ]}
              />
            ))}
          </View>

          <Text style={styles.filterLabel}>Breed</Text>
          <TextInput
            style={styles.input}
            placeholder="e.g. Beagle"
            value={tempFilters.breedFilter}
            onChangeText={(text) => updateTempFilter('breedFilter', text)}
          />

          <Text style={styles.filterLabel}>Gender</Text>
          <View style={styles.optionsRow}>
            {['All', 'Male', 'Female'].map(opt => (
              <TouchableOpacity
                key={opt}
                style={[
                  styles.optionButton,
                  tempFilters.genderFilter === opt && styles.optionSelected
                ]}
                onPress={() => updateTempFilter('genderFilter', opt)}
              >
                <Text style={[
                  styles.optionText,
                  tempFilters.genderFilter === opt && styles.optionTextSelected
                ]}>
                  {opt}
                </Text>
              </TouchableOpacity>
            ))}
          </View>

          <Text style={styles.filterLabel}>Age Range (months)</Text>
          <View style={styles.rangeRow}>
            <TextInput
              style={[styles.input, styles.rangeInput]}
              placeholder="Min"
              keyboardType="numeric"
              value={tempFilters.ageMin}
              onChangeText={(text) => updateTempFilter('ageMin', text)}
            />
            <TextInput
              style={[styles.input, styles.rangeInput]}
              placeholder="Max"
              keyboardType="numeric"
              value={tempFilters.ageMax}
              onChangeText={(text) => updateTempFilter('ageMax', text)}
            />
          </View>

          <Text style={styles.filterLabel}>Sort by</Text>
          <View style={styles.optionsRow}>
            {[
              { value: 'date', label: 'Date' },
              { value: 'likes', label: 'Likes' }
            ].map(opt => (
              <TouchableOpacity
                key={opt.value}
                style={[
                  styles.optionButton,
                  tempFilters.sortBy === opt.value && styles.optionSelected
                ]}
                onPress={() => updateTempFilter('sortBy', opt.value)}
              >
                <Text style={[
                  styles.optionText,
                  tempFilters.sortBy === opt.value && styles.optionTextSelected
                ]}>
                  {opt.label}
                </Text>
              </TouchableOpacity>
            ))}
          </View>

          <Text style={styles.filterLabel}>Sort Order</Text>
          <View style={styles.optionsRow}>
            {[
              { 
                value: 'desc', 
                label: tempFilters.sortBy === 'date' ? 'Newest First' : 'Most Liked First' 
              },
              { 
                value: 'asc', 
                label: tempFilters.sortBy === 'date' ? 'Oldest First' : 'Least Liked First' 
              }
            ].map(opt => (
              <TouchableOpacity
                key={opt.value}
                style={[
                  styles.optionButton,
                  tempFilters.sortOrder === opt.value && styles.optionSelected
                ]}
                onPress={() => updateTempFilter('sortOrder', opt.value)}
              >
                <Text style={[
                  styles.optionText,
                  tempFilters.sortOrder === opt.value && styles.optionTextSelected
                ]}>
                  {opt.label}
                </Text>
              </TouchableOpacity>
            ))}
          </View>

          <View style={styles.modalButtons}>
            <TouchableOpacity
              style={[styles.button, styles.cancelButton]}
              onPress={() => setModalVisible(false)}
            >
              <Text style={styles.cancelButtonText}>Cancel</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.button, styles.applyButton]}
              onPress={onApplyFilters}
            >
              <Text style={styles.applyButtonText}>Apply Filters</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  modalOverlay: { 
    flex: 1, 
    backgroundColor: 'rgba(0,0,0,0.5)', 
    justifyContent: 'center', 
    alignItems: 'center' 
  },
  modalContent: { 
    width: width * 0.9, 
    maxHeight: '100%',
    backgroundColor: '#fff', 
    borderRadius: 12, 
    padding: 20 
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
    color: '#333'
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
  rangeRow: { 
    flexDirection: 'row', 
    justifyContent: 'space-between',
    marginBottom: 8
  },
  rangeInput: { 
    width: '48%' 
  },
  modalButtons: { 
    flexDirection: 'row', 
    justifyContent: 'space-between', 
    marginTop: 24,
    gap: 12
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
  categoryRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 8,
    gap: 8,
  },
  categoryButton: {
    flex: 1,
  },
  categorySelected: {
    backgroundColor: '#E8E5FF',
    borderWidth: 2,
    borderColor: '#9188E5',
  }
});

export default PostFiltering;