// src/hooks/useScrollThreshold.js
import { useRef, useState } from 'react';
import { Animated, Dimensions } from 'react-native';

export default function useScrollThreshold(thresholdPx) {
  const scrollY = useRef(new Animated.Value(0)).current;
  const [passed, setPassed] = useState(false);

  const onScroll = Animated.event(
    [{ nativeEvent: { contentOffset: { y: scrollY } } }],
    {
      useNativeDriver: false,
      listener: e => {
        setPassed(e.nativeEvent.contentOffset.y > thresholdPx);
      },
    }
  );

  return { scrollY, passed, onScroll };
}
