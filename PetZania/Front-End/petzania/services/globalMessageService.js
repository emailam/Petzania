import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import Constants from 'expo-constants';

class GlobalMessageService {
    constructor() {
        this.stompClient = null;
        this.subscriptions = new Map();
        this.messageHandlers = new Set();
        this.reactionHandlers = new Set();
        this.currentUserId = null;
        this.currentOpenChatId = null;
        this.isConnecting = false;
        this.retryCount = 0;
        this.maxRetries = 3;
    }

    /**
     * Connect to the global message service
     * @param {string} userId - The current user's ID
     * @param {string} token - Authentication token
     * @returns {Promise<void>}
     */
    async connect(userId, token = null) {
        if (this.isConnecting) {
            console.log('🔄 Global message service connection already in progress');
            return;
        }

        if (this.stompClient && this.stompClient.active && this.currentUserId === userId) {
            console.log('✅ Global message service already connected for user:', userId);
            return;
        }

        // Validate inputs
        if (!userId) {
            console.warn('⚠️ Cannot connect: userId is required');
            return;
        }

        if (!token) {
            console.warn('⚠️ Cannot connect: authentication token is required');
            return;
        }

        this.isConnecting = true;

        try {
            // Disconnect any existing connection
            await this.disconnect();

            // Set the user ID after disconnect to avoid it being reset to null
            this.currentUserId = userId;
            console.log('🔍 Set currentUserId to:', this.currentUserId);

            console.log('🌐 Connecting to global message service for user:', userId);

            // Create SockJS connection
            const baseUrl = Constants.expoConfig.extra.API_BASE_URL_8081.replace(/\/api$/, '');
            const wsUrl = `${baseUrl}/ws`;
            
            console.log('🔌 Attempting WebSocket connection to:', wsUrl);
            const socket = new SockJS(wsUrl);

            // Create STOMP client
            this.stompClient = new Client({
                webSocketFactory: () => socket,
                connectHeaders: {
                    userId: userId,
                    Authorization: `Bearer ${token}`
                },
                debug: (str) => {
                    // Only log important debug messages to reduce noise
                    if (str.includes('connected') || str.includes('error') || str.includes('disconnect')) {
                        console.log('Global Message STOMP Debug:', str);
                    }
                },
                reconnectDelay: 5000,
                heartbeatIncoming: 4000,
                heartbeatOutgoing: 4000,
            });

            // Set up event handlers
            this.stompClient.onConnect = async (frame) => {
                console.log('✅ Global message service connected:', frame);
                this.retryCount = 0; // Reset retry count on successful connection
                await this.subscribeToUserTopics(userId);
                this.isConnecting = false;
            };

            this.stompClient.onDisconnect = (frame) => {
                console.log('🔌 Global message service disconnected:', frame);
                this.isConnecting = false;
            };

            this.stompClient.onStompError = (frame) => {
                console.error('❌ Global message service STOMP error:', frame.headers['message']);
                console.error('❌ STOMP error details:', frame);
                this.isConnecting = false;
            };

            this.stompClient.onWebSocketError = (error) => {
                console.error('❌ Global message service WebSocket error:', error);
                this.isConnecting = false;
                
                // Don't throw error on WebSocket connection issues - just log and retry later
                if (error.type === 'error') {
                    console.log('🔄 WebSocket connection failed, this may be due to server not being ready or authentication issues');
                }
            };

            // Add connection timeout
            const connectionTimeout = setTimeout(() => {
                if (this.isConnecting) {
                    console.warn('⏰ Global message service connection timeout');
                    this.isConnecting = false;
                    this.stompClient?.deactivate();
                }
            }, 10000); // 10 second timeout

            // Clear timeout on successful connection
            this.stompClient.onConnect = async (frame) => {
                clearTimeout(connectionTimeout);
                console.log('✅ Global message service connected:', frame);
                this.retryCount = 0;
                await this.subscribeToUserTopics(userId);
                this.isConnecting = false;
            };

            // Activate the connection
            this.stompClient.activate();

        } catch (error) {
            console.error('❌ Error connecting to global message service:', error);
            this.isConnecting = false;
            throw error;
        }
    }

    /**
     * Subscribe to user-specific topics for messages and reactions
     * @param {string} userId - The user ID to subscribe for
     */
    async subscribeToUserTopics(userId) {
        console.log('🔍 subscribeToUserTopics called with userId:', userId);
        console.log('🔍 this.currentUserId:', this.currentUserId);
        
        if (!this.stompClient || !this.stompClient.active) {
            console.warn('⚠️ Cannot subscribe: STOMP client not connected');
            return;
        }

        try {
            // Subscribe to user messages
            const messageTopic = `/topic/${userId}/messages`;
            console.log('📩 Subscribing to:', messageTopic);
            
            const messageSubscription = this.stompClient.subscribe(messageTopic, (message) => {
                try {
                    const messageData = JSON.parse(message.body);
                    console.log('📨 Received global message:', messageData);
                    this.handleGlobalMessage(messageData);
                } catch (error) {
                    console.error('❌ Error parsing global message:', error);
                }
            });

            this.subscriptions.set('messages', messageSubscription);

            // Subscribe to user reactions
            const reactionTopic = `/topic/${userId}/reactions`;
            console.log('👍 Subscribing to:', reactionTopic);
            
            const reactionSubscription = this.stompClient.subscribe(reactionTopic, (message) => {
                try {
                    const reactionData = JSON.parse(message.body);
                    console.log('👍 Received global reaction:', reactionData);
                    this.handleGlobalReaction(reactionData);
                } catch (error) {
                    console.error('❌ Error parsing global reaction:', error);
                }
            });

            this.subscriptions.set('reactions', reactionSubscription);

            console.log('✅ Successfully subscribed to global topics for user:', userId);

        } catch (error) {
            console.error('❌ Error subscribing to user topics:', error);
        }
    }

    /**
     * Handle incoming global messages
     * @param {Object} messageEventData - The message event data with messageDTO and eventType
     */
    handleGlobalMessage(messageEventData) {
        const { messageDTO, eventType } = messageEventData;
        
        if (!messageDTO || !eventType) {
            console.warn('⚠️ Invalid message event data:', messageEventData);
            return;
        }

        console.log('📨 Processing global message event:', eventType, messageDTO);

        // Only handle relevant event types: SEND, DELETE, EDIT, UPDATE_STATUS
        const relevantEventTypes = ['SEND', 'DELETE', 'EDIT', 'UPDATE_STATUS'];
        if (!relevantEventTypes.includes(eventType)) {
            console.log('🚫 Ignoring irrelevant event type:', eventType);
            return;
        }

        // Check if this message is from the currently open chat
        const isFromCurrentChat = this.currentOpenChatId && messageDTO.chatId === this.currentOpenChatId;
        
        // For SEND events, handle unread count logic
        if (eventType === 'SEND' && !isFromCurrentChat && messageDTO.senderId !== this.currentUserId) {
            console.log('📱 New message from different chat, should increment unread count');
        }

        // Enhanced debugging for message routing
        console.log('🔍 Message routing debug:', {
            eventType,
            messageDTO: {
                chatId: messageDTO.chatId,
                senderId: messageDTO.senderId,
                content: messageDTO.content,
                messageId: messageDTO.messageId
            },
            isFromCurrentChat,
            currentOpenChatId: this.currentOpenChatId,
            currentUserId: this.currentUserId,
            shouldIncrementUnread: eventType === 'SEND' && !isFromCurrentChat && messageDTO.senderId !== this.currentUserId
        });

        // Notify all registered message handlers
        this.messageHandlers.forEach(handler => {
            try {
                handler({
                    messageDTO,
                    eventType,
                    isFromCurrentChat
                });
            } catch (error) {
                console.error('❌ Error in message handler:', error);
            }
        });
    }

    /**
     * Handle incoming global reactions
     * @param {Object} reactionEventData - The reaction event data with messageReactionDTO and eventType
     */
    handleGlobalReaction(reactionEventData) {
        const { messageReactionDTO, eventType } = reactionEventData;
        
        if (!messageReactionDTO || !eventType) {
            console.warn('⚠️ Invalid reaction event data:', reactionEventData);
            return;
        }

        console.log('👍 Processing global reaction event:', eventType, messageReactionDTO);

        // Only handle relevant event types: REACT, REMOVE_REACT
        const relevantEventTypes = ['REACT', 'REMOVE_REACT'];
        if (!relevantEventTypes.includes(eventType)) {
            console.log('🚫 Ignoring irrelevant reaction event type:', eventType);
            return;
        }

        // Notify all registered reaction handlers
        this.reactionHandlers.forEach(handler => {
            try {
                handler({
                    messageReactionDTO,
                    eventType
                });
            } catch (error) {
                console.error('❌ Error in reaction handler:', error);
            }
        });
    }

    /**
     * Set the currently open chat ID to prevent unread count increments
     * @param {string|null} chatId - The chat ID that is currently open
     */
    setCurrentOpenChat(chatId) {
        this.currentOpenChatId = chatId;
        console.log('💬 Current open chat set to:', chatId);
    }

    /**
     * Add a message handler
     * @param {Function} handler - Function to handle messages (messageData, isFromCurrentChat) => void
     */
    addMessageHandler(handler) {
        this.messageHandlers.add(handler);
        console.log('📝 Message handler added. Total handlers:', this.messageHandlers.size);
    }

    /**
     * Remove a message handler
     * @param {Function} handler - The handler to remove
     */
    removeMessageHandler(handler) {
        this.messageHandlers.delete(handler);
        console.log('🗑️ Message handler removed. Total handlers:', this.messageHandlers.size);
    }

    /**
     * Add a reaction handler
     * @param {Function} handler - Function to handle reactions (reactionData) => void
     */
    addReactionHandler(handler) {
        this.reactionHandlers.add(handler);
        console.log('👍 Reaction handler added. Total handlers:', this.reactionHandlers.size);
    }

    /**
     * Remove a reaction handler
     * @param {Function} handler - The handler to remove
     */
    removeReactionHandler(handler) {
        this.reactionHandlers.delete(handler);
        console.log('🗑️ Reaction handler removed. Total handlers:', this.reactionHandlers.size);
    }

    /**
     * Disconnect from the global message service
     */
    async disconnect() {
        console.log('🔌 Disconnecting from global message service');
        console.log('🔍 Stack trace for disconnect:', new Error().stack);

        // Clear subscriptions
        this.subscriptions.forEach((subscription, key) => {
            try {
                subscription.unsubscribe();
                console.log(`🔕 Unsubscribed from ${key}`);
            } catch (error) {
                console.error(`❌ Error unsubscribing from ${key}:`, error);
            }
        });
        this.subscriptions.clear();

        // Clear handlers
        this.messageHandlers.clear();
        this.reactionHandlers.clear();

        // Disconnect STOMP client
        if (this.stompClient) {
            try {
                this.stompClient.deactivate();
                console.log('🔌 STOMP client deactivated');
            } catch (error) {
                console.error('❌ Error deactivating STOMP client:', error);
            }
            this.stompClient = null;
        }

        this.currentUserId = null;
        this.currentOpenChatId = null;
        this.isConnecting = false;

        console.log('✅ Global message service disconnected');
    }

    /**
     * Check if the global message service is connected
     * @returns {boolean} True if connected, false otherwise
     */
    isConnected() {
        return this.stompClient && this.stompClient.active && !this.isConnecting;
    }

    /**
     * Get the current connection status
     * @returns {Object} Connection status information
     */
    getConnectionStatus() {
        return {
            isConnected: this.isConnected(),
            isConnecting: this.isConnecting,
            currentUserId: this.currentUserId,
            subscriptionCount: this.subscriptions.size,
            handlerCount: this.messageHandlers.size + this.reactionHandlers.size
        };
    }

    /**
     * Get the current user ID
     * @returns {string|null}
     */
    getCurrentUserId() {
        return this.currentUserId;
    }
}

// Export a singleton instance
export default new GlobalMessageService();
