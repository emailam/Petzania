import { ScrollView, Text, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Link } from 'expo-router';
import { responsive } from '@/utilities/responsive';
import RegisterForm from '@/components/RegisterForm';

export default function RegisterScreen() {
  return (
    <SafeAreaView style={styles.safeArea}>
      <Text style={styles.title}>Create account</Text>

      <ScrollView contentContainerStyle={styles.container}>
        <RegisterForm />

        <Text style={styles.footerText}>
          Already have an account?{' '}
          <Link href="/RegisterModule/LoginScreen" style={styles.link}>Login now</Link>
        </Text>
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
    fontSize: responsive.fonts.small,
    color: '#666',
  },
});