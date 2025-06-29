import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class ChatSocketService {
    constructor() {
        this.stompClient = null;
        this.subscriptions = new Map();
    }

    connect(userId, token = null) {
        if (this.stompClient) {
            return this.stompClient;
        }

        // Create SockJS connection for chat service
        const socket = new SockJS('http://192.168.1.6:8081/ws');
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
            console.log('Chat STOMP Connected:', frame);
        };

        this.stompClient.onDisconnect = (frame) => {
            console.log('Chat STOMP Disconnected:', frame);
        };

        this.stompClient.onStompError = (frame) => {
            console.error('Chat STOMP Error:', frame.headers['message']);
            console.error('Details:', frame.body);
        };

        this.stompClient.activate();
        return this.stompClient;
    }

    isClientConnected() {
        return this.stompClient && this.stompClient.active;
    }

    // STOMP subscription methods
    subscribeTo(destination, callback) {
        if (this.isClientConnected()) {
            const subscription = this.stompClient.subscribe(destination, (message) => {
                const body = JSON.parse(message.body);
                callback(body);
            });
            this.subscriptions.set(destination, subscription);
            return subscription;
        }
        return null;
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
