import { Controller } from 'react-hook-form';
import { View, TextInput, Text, StyleSheet } from 'react-native';

export default function FormInput({ control, name, errors, icon, ...props }) {
  return (
    <View style={styles.container}>
      <Controller
        control={control}
        name={name}
        render={({ field }) => (
          <View style={[styles.inputContainer, errors[name] && styles.errorBorder]}>
            {icon && <View style={styles.iconContainer}>{icon}</View>}
            <TextInput
              {...props}
              placeholderTextColor="#989898"
              style={styles.input}
              onChangeText={field.onChange}
              onBlur={() => {
                // Trim whitespace for email fields when user finishes typing
                if (name === 'email' && field.value) {
                  field.onChange(field.value.trim());
                }
                field.onBlur();
              }}
              value={field.value}
            />
          </View>
        )}
      />
      {errors[name] && <Text style={styles.errorText}>{errors[name].message}</Text>}
    </View>
  );
}

const styles = StyleSheet.create({
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: 'gray',
    borderRadius: 10,
    paddingHorizontal: 12,
  },
  iconContainer: {
    marginRight: 8,
  },
  input: {
    flex: 1,
    height: 50,
    fontSize: 16,
    color: 'black',
  },
  errorBorder: {
    borderColor: 'red',
  },
  errorText: {
    color: 'red',
    fontSize: 12,
    marginTop: 4,
  }
});