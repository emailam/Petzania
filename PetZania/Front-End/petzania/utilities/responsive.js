import {
    widthPercentageToDP as wp,
    heightPercentageToDP as hp,
  } from 'react-native-responsive-screen';
  
  export const responsive = {
    wp,
    hp,
    
    // Predefined sizes
    margins: {
      screenEdge: wp('5%'),
      betweenElements: hp('2%'),
    },
    
    fonts: {
      title: wp('10%'),
      body: wp('4.5%'),
      small: wp('3.5%'),
    },
    
    buttons: {
      width: {
        primary: wp('80%'),
        secondary: wp('35%'),
      }
    }
  };