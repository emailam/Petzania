import React from 'react';
import { View, Text, StyleSheet, ScrollView, FlatList, TouchableOpacity, TextInput } from 'react-native';
import { useForm, Controller } from 'react-hook-form';
import { UserContext } from '@/context/UserContext';

export default function AdoptionBreedingForm() {
  const { control, handleSubmit, watch, formState: { errors } } = useForm({
    defaultValues: {
      type: 'adoption',
      pets: null,
      location: '',
      description: '',
    }
  });
  const descriptionValue = watch('description');

  const {user} = React.useContext(UserContext);
  const userPets = user?.myPets || [];
  const onSubmit = (data) => {
    console.log('Form submitted:', data);
    // Handle form submission logic here
  };
  return (
    <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
      <Text style={styles.label}>Post Type</Text>
      <Controller
        control={control}
        name="type"
        render={({ field: { value, onChange } }) => (
          <View style={styles.radioGroup}>
            {['adoption', 'breeding'].map(option => (
              <TouchableOpacity
                key={option}
                style={[
                  styles.radioOption,
                  value === option && styles.radioSelected
                ]}
                onPress={() => onChange(option)}
              >
                <Text style={[
                  styles.radioText,
                  value === option && styles.radioTextSelected
                ]}>
                  {option.charAt(0).toUpperCase() + option.slice(1)}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        )}
      />

      <Text style={styles.label}>Choose Pet</Text>
      <Controller
        control={control}
        name="pets"
        rules={{ required: 'Please select a pet.' }}
        render={({ field: { value, onChange } }) => (
          <FlatList
            data={userPets}
            horizontal
            keyExtractor={item => item.petId}
            contentContainerStyle={styles.petList}
            renderItem={({ item }) => {
              const selected = value === item.petId;
              return (
                <TouchableOpacity onPress={() => onChange(item.petId)} style={styles.petItem}>
                  <View style={[styles.petImageWrapper, selected && styles.petSelected]}>  
                    {item.myPicturesURLs && item.myPicturesURLs.length > 0
                      ? <Image source={{ uri: item.myPicturesURLs[0] }} style={styles.petImage} />
                      : <View style={styles.petPlaceholder} />
                    }
                  </View>
                  <Text style={styles.petName}>{item.name}</Text>
                </TouchableOpacity>
              );
            }}
          />
        )}
      />
      {errors.petId && <Text style={styles.error}>{errors.petId.message}</Text>}
      
      <Text style={styles.label}>Location</Text>
      <Controller
        control={control}
        name="location"
        rules={{ required: 'Location is required.' }}
        render={({ field: { value, onChange } }) => (
          <TextInput
            style={styles.textInput}
            onChangeText={onChange}
            value={value}
            placeholder="State, Country"
          />
        )}
      />
      {errors.location && <Text style={styles.error}>{errors.location.message}</Text>}

      <Text style={styles.label}>Description</Text>
      <Controller
        control={control}
        name="description"
        rules={{ maxLength: { value: 500, message: 'Max 500 characters.' } }}
        render={({ field: { value, onChange } }) => (
          <TextInput
            style={[styles.textInput, styles.textArea]}
            multiline
            maxLength={500}
            onChangeText={onChange}
            value={value}
            placeholder="Enter description"
          />
        )}
      />
      <Text style={styles.charCount}>{descriptionValue.length}/500</Text>
      {errors.description && <Text style={styles.error}>{errors.description.message}</Text>}

      <TouchableOpacity style={styles.postButton} onPress={handleSubmit(onSubmit)}>
        <Text style={styles.postButtonText}>Post</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    padding: 16,
    backgroundColor: '#fff',
  },
  label: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 8,
    color: '#333',
  },
  radioGroup: {
    flexDirection: 'row',
    marginBottom: 16,
  },
  radioOption: {
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderWidth: 1,
    borderColor: '#333',
    borderRadius: 20,
    marginRight: 12,
    backgroundColor: '#fff',
    
  },
  radioSelected: {
    backgroundColor: '#9188E5',
    borderColor: '#9188E5',
  },
  radioText: {
    fontSize: 14,
    color: '#000',
  },
  radioTextSelected: {
    color: '#fff',
  },
  petList: {
    paddingVertical: 8,
  },
  petItem: {
    alignItems: 'center',
    marginRight: 16,
  },
  petImageWrapper: {
    width: 64,
    height: 64,
    borderRadius: 32,
    borderWidth: 1,
    borderColor: '#ccc',
    overflow: 'hidden',
    justifyContent: 'center',
    alignItems: 'center',
  },
  petSelected: {
    borderColor: '#9188E5',
    borderWidth: 2,
  },
  petImage: {
    width: 64,
    height: 64,
  },
  petPlaceholder: {
    width: 64,
    height: 64,
    backgroundColor: '#ddd',
  },
  petName: {
    marginTop: 4,
    fontSize: 12,
    color: '#000',
    marginBottom: 8,
  },
  textInput: {
    borderWidth: 1,
    borderColor: '#333',
    borderRadius: 8,
    padding: 8,
    fontSize: 14,
    color: '#000',
    backgroundColor: '#fafafa',
    marginBottom: 16,
  },
  textArea: {
    height: 120,
    textAlignVertical: 'top',
  },
  charCount: {
    alignSelf: 'flex-end',
    marginTop: 4,
    fontSize: 12,
    color: '#666',
  },
  postButton: {
    marginTop: 24,
    backgroundColor: '#9188E5',
    borderColor: '#9188E5',
    borderWidth: 1,
    borderRadius: 8,
    paddingVertical: 12,
    alignItems: 'center',
  },
  postButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  error: {
    color: 'red',
    marginTop: 4,
  },
});