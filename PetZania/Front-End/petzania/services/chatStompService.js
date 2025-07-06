import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import Constants from 'expo-constants';

class ChatSocketService {
    constructor() {
        this.stompClient = null;
        this.subscriptions = new Map();
    }

    connect(userId, token = null) {
        return new Promise((resolve, reject) => {
            if (this.stompClient && this.stompClient.active) {
                console.log('✅ STOMP client already connected');
                resolve(this.stompClient);
                return;
            }

            // Disconnect any existing client
            if (this.stompClient) {
                this.disconnect();
            }

            try {
                // Create SockJS connection for chat service
                const baseUrl = Constants.expoConfig.extra.API_BASE_URL_8081.replace(/\/api$/, '');
                const wsUrl = `${baseUrl}/ws`;
                console.log('🔌 Chat service base URL:', baseUrl);
                console.log('🔌 Connecting to WebSocket:', wsUrl);
                
                const socket = new SockJS(wsUrl);
                
                // Add SockJS event listeners for debugging
                socket.onopen = () => {
                    console.log('✅ SockJS connection opened');
                };
                
                socket.onclose = (event) => {
                    console.log('🔌 SockJS connection closed:', event);
                };
                
                socket.onerror = (error) => {
                    console.error('❌ SockJS error:', error);
                };
                
                // Create STOMP client
                this.stompClient = new Client({
                    webSocketFactory: () => socket,
                    connectHeaders: {
                        userId: userId,
                        Authorization: token ? `Bearer ${token}` : ''
                    },
                    debug: (str) => {
                        console.log('Chat STOMP Debug:', str);
                    },
                    reconnectDelay: 5000,
                    heartbeatIncoming: 4000,
                    heartbeatOutgoing: 4000,
                });

                this.stompClient.onConnect = (frame) => {
                    console.log('✅ Chat STOMP Connected:', frame);
                    resolve(this.stompClient);
                };

                this.stompClient.onDisconnect = (frame) => {
                    console.log('🔌 Chat STOMP Disconnected:', frame);
                };

                this.stompClient.onStompError = (frame) => {
                    console.error('❌ Chat STOMP Error:', frame.headers['message']);
                    console.error('Details:', frame.body);
                    reject(new Error(`STOMP Error: ${frame.headers['message'] || 'Unknown error'}`));
                };

                this.stompClient.onWebSocketError = (error) => {
                    console.error('❌ WebSocket Error:', error);
                    reject(new Error(`WebSocket Error: ${error.message || 'Connection failed'}`));
                };

                console.log('🔄 Activating STOMP client...');
                this.stompClient.activate();
                
                // Add timeout for connection
                setTimeout(() => {
                    if (!this.stompClient || !this.stompClient.active) {
                        reject(new Error('STOMP connection timeout'));
                    }
                }, 10000); // 10 second timeout

            } catch (error) {
                console.error('❌ Error creating STOMP client:', error);
                reject(error);
            }
        });
    }

    isClientConnected() {
        return this.stompClient && this.stompClient.active;
    }

    // STOMP subscription methods
    subscribeTo(destination, callback) {
        return new Promise((resolve, reject) => {
            if (!this.isClientConnected()) {
                reject(new Error('STOMP client not connected'));
                return;
            }

            try {
                const subscription = this.stompClient.subscribe(destination, (message) => {
                    try {
                        const body = JSON.parse(message.body);
                        callback(body);
                    } catch (error) {
                        console.error('❌ Error parsing STOMP message:', error);
                    }
                });
                
                this.subscriptions.set(destination, subscription);
                console.log('✅ Subscribed to:', destination);
                resolve(subscription);
            } catch (error) {
                console.error('❌ Error subscribing to:', destination, error);
                reject(error);
            }
        });
    }

    unsubscribeFrom(destination) {
        if (this.subscriptions.has(destination)) {
            const subscription = this.subscriptions.get(destination);
            subscription.unsubscribe();
            this.subscriptions.delete(destination);
        }
    }

    // Chat specific methods for STOMP
    subscribeToChatTopic(chatId, callback) {
        return this.subscribeTo(`/topic/chats/${chatId}`, callback);
    }

    unsubscribeFromChatTopic(chatId) {
        this.unsubscribeFrom(`/topic/chats/${chatId}`);
    }

    // Subscribe to user-specific message updates (as per backend implementation)
    subscribeToUserMessages(userId, callback) {
        return this.subscribeTo(`/topic/${userId}/messages`, callback);
    }

    unsubscribeFromUserMessages(userId) {
        this.unsubscribeFrom(`/topic/${userId}/messages`);
    }

    // Subscribe to user-specific reaction updates
    subscribeToUserReactions(userId, callback) {
        return this.subscribeTo(`/topic/${userId}/reactions`, callback);
    }

    unsubscribeFromUserReactions(userId) {
        this.unsubscribeFrom(`/topic/${userId}/reactions`);
    }

    // Disconnect the chat service
    disconnect() {
        if (this.stompClient) {
            // Unsubscribe from all subscriptions
            this.subscriptions.forEach((subscription, destination) => {
                subscription.unsubscribe();
            });
            this.subscriptions.clear();
            
            this.stompClient.deactivate();
            this.stompClient = null;
        }
    }
}

// Create a singleton instance
const chatSocketService = new ChatSocketService();
export default chatSocketService;
