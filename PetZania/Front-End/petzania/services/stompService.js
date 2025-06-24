import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class SocketService {
    constructor() {
        this.stompClient = null;
        this.subscriptions = new Map();
    }

    connect(userId, token = null) {
        if (this.stompClient) {
            return this.stompClient;
        }

        // Create SockJS connection
        const socket = new SockJS('http://192.168.1.6:8081/ws');
        // Create STOMP client
        this.stompClient = new Client({
            webSocketFactory: () => socket,
            connectHeaders: {
                userId: userId,
                Authorization: token ? `Bearer ${token}` : ''
            },
            debug: (str) => {
                console.log('STOMP Debug:', str);
            },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        this.stompClient.onConnect = (frame) => {
            console.log('STOMP Connected:', frame);
        };

        this.stompClient.onDisconnect = (frame) => {
            console.log('STOMP Disconnected:', frame);
        };

        this.stompClient.onStompError = (frame) => {
            console.error('STOMP Error:', frame.headers['message']);
            console.error('Details:', frame.body);
        };

        this.stompClient.activate();
        return this.stompClient;
    }

    disconnect() {
        if (this.stompClient) {
            this.subscriptions.forEach((subscription) => {
                subscription.unsubscribe();
            });
            this.subscriptions.clear();
            this.stompClient.deactivate();
            this.stompClient = null;
        }
    }

    getClient() {
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

    // Send message to application destination
    sendToApp(destination, message) {
        if (this.isClientConnected()) {
            this.stompClient.publish({
                destination: `/app${destination}`,
                body: JSON.stringify(message)
            });
        }
    }    // Chat specific methods for STOMP
    subscribeToChatTopic(chatId, callback) {
        return this.subscribeTo(`/topic/chats/${chatId}`, callback);
    }

    unsubscribeFromChatTopic(chatId) {
        this.unsubscribeFrom(`/topic/chats/${chatId}`);
    }

    // Remove all subscriptions
    removeAllSubscriptions() {
        this.subscriptions.forEach((subscription) => {
            subscription.unsubscribe();
        });
        this.subscriptions.clear();
    }
}

// Create a singleton instance
const socketService = new SocketService();
export default socketService;