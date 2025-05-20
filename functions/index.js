const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.sendMessageNotification = functions.firestore
    .document('messages/{chatId}/messages/{messageId}')
    .onCreate(async (snapshot, context) => {
        const message = snapshot.data();
        const chatId = context.params.chatId;
        
        // Get chat information
        const chatSnapshot = await admin.firestore()
            .collection('chatRooms')
            .doc(chatId)
            .get();
        
        if (!chatSnapshot.exists) {
            console.log('Chat not found');
            return null;
        }
        
        const chat = chatSnapshot.data();
        const recipientId = chat.participants.find(id => id !== message.senderId);
        
        if (!recipientId) {
            console.log('Recipient not found');
            return null;
        }
        
        // Get sender information
        const senderSnapshot = await admin.firestore()
            .collection('users')
            .doc(message.senderId)
            .get();
        
        if (!senderSnapshot.exists) {
            console.log('Sender not found');
            return null;
        }
        
        const sender = senderSnapshot.data();
        
        // Get recipient's FCM token
        const recipientSnapshot = await admin.firestore()
            .collection('users')
            .doc(recipientId)
            .get();
        
        if (!recipientSnapshot.exists || !recipientSnapshot.data().fcmToken) {
            console.log('Recipient or FCM token not found');
            return null;
        }
        
        const recipientToken = recipientSnapshot.data().fcmToken;
        
        // Create notification payload
        const payload = {
            notification: {
                title: sender.username,
                body: message.content,
                clickAction: 'OPEN_CHAT_ACTIVITY'
            },
            data: {
                chatId: chatId,
                senderId: message.senderId
            }
        };
        
        // Send notification
        try {
            await admin.messaging().sendToDevice(recipientToken, payload);
            console.log('Notification sent successfully');
            return null;
        } catch (error) {
            console.error('Error sending notification:', error);
            return null;
        }
    });