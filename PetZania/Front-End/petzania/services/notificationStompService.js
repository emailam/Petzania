import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class NotificationSocketService {
    constructor() {
        this.stompClient = null;
        this.subscriptions = new Map();
        this.notificationCallbacks = [];
        this.countCallbacks = [];
        this.connectionCallbacks = [];
    }

    connect(userId, token = null) {
        if (this.stompClient && this.stompClient.active) {
            return this.stompClient;
        }

        // Create SockJS connection for notification service
        const socket = new SockJS('http://192.168.1.6:8083/ws');
        // Create STOMP client
        this.stompClient = new Client({
            webSocketFactory: () => socket,
            connectHeaders: {
                userId: userId,
                Authorization: token ? `Bearer ${token}` : ''
            },
            debug: (str) => {
                console.log('Notification STOMP Debug:', str);
            },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        this.stompClient.onConnect = (frame) => {
            console.log('Notification STOMP Connected:', frame);
            
            // Auto-subscribe to notification topics when connected
            this.subscribeToNotifications(userId);
            this.subscribeToNotificationCount(userId);
            
            // Notify connection callbacks
            this.connectionCallbacks.forEach(callback => callback(true));
        };

        this.stompClient.onDisconnect = (frame) => {
            console.log('Notification STOMP Disconnected:', frame);
            
            // Notify connection callbacks
            this.connectionCallbacks.forEach(callback => callback(false));
        };

        this.stompClient.onStompError = (frame) => {
            console.error('Notification STOMP Error:', frame.headers['message']);
            console.error('Details:', frame.body);
            
            // Notify connection callbacks
            this.connectionCallbacks.forEach(callback => callback(false));
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
                let body;
                try {
                    body = JSON.parse(message.body);
                } catch (error) {
                    // Handle non-JSON messages (like count updates)
                    body = message.body;
                }
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

    // Notification specific methods
    subscribeToNotifications(userId) {
        const destination = `/user/${userId}/queue/notifications`;
        return this.subscribeTo(destination, (notification) => {
            console.log('Received notification:', notification);
            this.notificationCallbacks.forEach(callback => {
                try {
                    callback(notification);
                } catch (error) {
                    console.error('Error in notification callback:', error);
                }
            });
        });
    }

    subscribeToNotificationCount(userId) {
        const destination = `/user/${userId}/queue/notification-count`;
        return this.subscribeTo(destination, (countData) => {
            const count = typeof countData === 'number' ? countData : parseInt(countData);
            console.log('Received count update:', count);
            this.countCallbacks.forEach(callback => {
                try {
                    callback(count);
                } catch (error) {
                    console.error('Error in count callback:', error);
                }
            });
        });
    }

    // Register callback for new notifications
    onNotification(callback) {
        this.notificationCallbacks.push(callback);
        return () => {
            this.notificationCallbacks = this.notificationCallbacks.filter(cb => cb !== callback);
        };
    }

    // Register callback for count updates
    onCountUpdate(callback) {
        this.countCallbacks.push(callback);
        return () => {
            this.countCallbacks = this.countCallbacks.filter(cb => cb !== callback);
        };
    }

    // Register callback for connection status
    onConnectionChange(callback) {
        this.connectionCallbacks.push(callback);
        return () => {
            this.connectionCallbacks = this.connectionCallbacks.filter(cb => cb !== callback);
        };
    }

    // Disconnect the notification service
    disconnect() {
        if (this.stompClient) {
            // Unsubscribe from all subscriptions
            this.subscriptions.forEach((subscription, destination) => {
                subscription.unsubscribe();
            });
            this.subscriptions.clear();
            
            // Clear callbacks
            this.notificationCallbacks = [];
            this.countCallbacks = [];
            this.connectionCallbacks = [];
            
            this.stompClient.deactivate();
            this.stompClient = null;
        }
    }
}

// Create a singleton instance
const notificationSocketService = new NotificationSocketService();
export default notificationSocketService;
