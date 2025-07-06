import { ScrollView, Text, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { responsive } from '@/utilities/responsive';
import RegisterForm from '@/components/RegisterForm';

export default function RegisterScreen() {
  const router = useRouter();

  return (
    <SafeAreaView style={styles.safeArea}>
      <Text style={styles.title}>Create account</Text>

      <ScrollView contentContainerStyle={styles.container}>
        <RegisterForm />

        <View style={styles.textContainer}>
          <Text style={styles.footerText}>Already have an account?{' '}</Text>
          <Text style={styles.link} onPress={() => router.replace("/RegisterModule/LoginScreen")}>Login now</Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    paddingTop: responsive.hp('10%'),
    backgroundColor: 'white',
    gap: responsive.hp('2%'),
    height: responsive.hp('100%'),
  },
  title: {
    fontSize: responsive.fonts.title,
    color: '#9188E5',
    fontWeight: 'bold',
    marginLeft: responsive.margins.screenEdge,
  },
  container: {
    flexGrow: 1,
    alignItems: 'center',
    width: responsive.wp('100%'),
    gap: responsive.hp('2%'),
  },
  link: {
    color: '#9188E5',
    fontWeight: 'bold',
  },
  footerText: {
    textAlign: 'center',
    fontSize: 14,
    color: '#666',
  },
  textContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    flexWrap: 'wrap',
  },
});