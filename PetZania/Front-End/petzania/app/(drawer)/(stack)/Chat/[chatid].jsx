// import React, { useState, useCallback, useEffect } from 'react';
// import { GiftedChat } from 'react-native-gifted-chat';

// export default function ChatDetailScreen() {
//   const [messages, setMessages] = useState([]);

//   useEffect(() => {
//     setMessages([
//       {
//         _id: 1,
//         text: 'Hello! How can I help you?',
//         createdAt: new Date(),
//         user: {
//           _id: 2,
//           name: 'Support',
//           avatar: 'https://placeimg.com/140/140/any',
//         },
//       },
//     ]);
//   }, []);

//   const onSend = useCallback((newMessages = []) => {
//     setMessages((prevMessages) =>
//       GiftedChat.append(prevMessages, newMessages)
//     );
//     // You should also send the message to your backend or socket here
//   }, []);

//   return (
//     <GiftedChat
//       messages={messages}
//       onSend={(messages) => onSend(messages)}
//       user={{
//         _id: 1,
//       }}
//     />
//   );
// }