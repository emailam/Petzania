import { Controller } from 'react-hook-form';
import { View, Text, StyleSheet, Pressable } from 'react-native';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { Link } from 'expo-router';
import { responsive } from '@/utilities/responsive';
export default function TermsInput({ control, name, errors}) {
  return (
    <View style={styles.termsContainer}>
          <Controller
            control={control}
            name={name}
            render={({ field }) => (
              <Pressable 
                style={styles.checkboxWrapper}
                onPress={() => field.onChange(!field.value)}
                activeOpacity={1}
              >
                <FontAwesome
                  name={field.value ? "check-square" : "square-o"}
                  size={responsive.wp('6%')}
                  color="#9188E5"
                  width={responsive.wp('5%')}
                />
                <Text style={styles.termsText}>
                  I agree to the{' '}
                  <Link href="" style={styles.link}>Terms & Conditions</Link> and{' '}
                  <Link href="" style={styles.link}>Privacy Policy</Link>
                </Text>
              </Pressable>
            )}
          />
          {errors.termsAccepted && (
            <Text style={styles.errorText}>{errors.termsAccepted.message}</Text>
          )}
        </View>
  );
}

const styles = StyleSheet.create({

  termsContainer: {
    marginVertical: responsive.hp('2%'),
  },
  checkboxWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: responsive.wp('2%'),
    marginBottom: responsive.hp('2%'),
  },
  termsText: {
    fontSize: responsive.fonts.small,
    color: '#666',
    flexShrink: 1,
    width: responsive.wp('70%'),
  },
  link: {
    color: '#9188E5',
    fontWeight: 'bold',
  },
  errorText: {
    color: 'red',
    fontSize: 12,
    marginTop: 4,
  }
});