# Firebase Firestore Database Structure for DevStreak

## Collections Overview

### 1. users
```
users/{userId}
├── email: string
├── username: string
├── avatarUrl: string (optional)
├── xp: number (default: 0)
├── level: number (default: 1)
├── dailyStreak: number (default: 0)
├── streakStartDate: timestamp
├── createdAt: timestamp
├── lastActivity: timestamp
├── isPremium: boolean (default: false)
├── subscriptionPlan: string (optional)
├── subscriptionStart: timestamp (optional)
├── subscriptionEnd: timestamp (optional)
└── preferences: map
    ├── notifications: boolean
    ├── darkMode: boolean
    └── language: string
```

### 2. user_stats (for analytics and leaderboards)
```
user_stats/{statId}
├── userId: string
├── xpGained: number
├── source: string ("challenge", "bonus", "streak")
├── timestamp: timestamp
├── date: string (YYYY-MM-DD)
├── challengeId: string (optional)
└── metadata: map (optional)
```

### 3. subscriptions
```
subscriptions/{subscriptionId}
├── userId: string
├── planId: string ("devstreak_monthly", "devstreak_lifetime")
├── isActive: boolean
├── startDate: timestamp
├── endDate: timestamp (null for lifetime)
├── isLifetime: boolean
├── transactionId: string
├── platform: string ("android", "ios")
├── paymentMethod: string (optional)
├── createdAt: timestamp
└── cancelledAt: timestamp (optional)
```

### 4. friends
```
friends/{userId}/friends/{friendId}
├── addedAt: timestamp
├── status: string ("active", "pending", "blocked")
└── mutualFriends: number (calculated)
```

### 5. challenges (for tracking completed challenges)
```
challenges/{challengeId}
├── userId: string
├── pathId: string
├── day: number
├── completedAt: timestamp
├── xpEarned: number
├── score: number (optional)
├── timeSpent: number (milliseconds)
└── feedback: map (optional)
    ├── aiScore: number
    ├── strengths: array
    └── improvements: array
```

### 6. leaderboards (cached leaderboard data)
```
leaderboards/{type} (global, weekly, monthly, streak)
├── lastUpdated: timestamp
├── entries: array of objects
│   ├── userId: string
│   ├── username: string
│   ├── avatarUrl: string
│   ├── score: number (xp or streak)
│   └── rank: number
└── metadata: map
    ├── totalUsers: number
    └── updateFrequency: string
```

## Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Users can read/write their own profile
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      allow read: if request.auth != null; // Allow reading other users for leaderboards
    }
    
    // User stats are readable by owner and for leaderboard calculations
    match /user_stats/{statId} {
      allow read, write: if request.auth != null && 
        (request.auth.uid == resource.data.userId || 
         request.auth.uid == request.resource.data.userId);
    }
    
    // Subscriptions are private to the user
    match /subscriptions/{subscriptionId} {
      allow read, write: if request.auth != null && 
        (request.auth.uid == resource.data.userId || 
         request.auth.uid == request.resource.data.userId);
    }
    
    // Friends collections
    match /friends/{userId}/friends/{friendId} {
      allow read, write: if request.auth != null && 
        (request.auth.uid == userId || request.auth.uid == friendId);
    }
    
    // Challenges are private to the user
    match /challenges/{challengeId} {
      allow read, write: if request.auth != null && 
        (request.auth.uid == resource.data.userId || 
         request.auth.uid == request.resource.data.userId);
    }
    
    // Leaderboards are read-only for authenticated users
    match /leaderboards/{type} {
      allow read: if request.auth != null;
      allow write: if false; // Only server can update leaderboards
    }
  }
}
```

## Cloud Functions for Server-Side Logic

### Function 1: Update Leaderboards (Scheduled)
```javascript
// functions/updateLeaderboards.js
const functions = require('firebase-functions');
const admin = require('firebase-admin');

exports.updateLeaderboards = functions.pubsub
  .schedule('every 5 minutes')
  .onRun(async (context) => {
    const db = admin.firestore();
    
    // Update global leaderboard
    const globalUsers = await db.collection('users')
      .orderBy('xp', 'desc')
      .limit(100)
      .get();
    
    const globalEntries = globalUsers.docs.map((doc, index) => ({
      userId: doc.id,
      username: doc.data().username,
      avatarUrl: doc.data().avatarUrl,
      score: doc.data().xp,
      rank: index + 1
    }));
    
    await db.collection('leaderboards').doc('global').set({
      lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
      entries: globalEntries,
      metadata: {
        totalUsers: globalUsers.size,
        updateFrequency: '5 minutes'
      }
    });
    
    // Similar logic for weekly, monthly, and streak leaderboards
    // ...
  });
```

### Function 2: Handle Subscription Changes
```javascript
// functions/subscriptionWebhook.js
exports.handleSubscriptionWebhook = functions.https.onRequest(async (req, res) => {
  const { transactionId, userId, planId, status } = req.body;
  
  const db = admin.firestore();
  
  if (status === 'purchased') {
    // Verify purchase with platform store
    const isValid = await verifyPurchaseWithStore(transactionId, planId);
    
    if (isValid) {
      // Update user's subscription status
      await db.collection('users').doc(userId).update({
        isPremium: true,
        subscriptionPlan: planId,
        subscriptionStart: admin.firestore.FieldValue.serverTimestamp()
      });
      
      // Record subscription
      await db.collection('subscriptions').add({
        userId,
        planId,
        transactionId,
        isActive: true,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      });
    }
  }
  
  res.status(200).send({ success: true });
});
```

## Data Migration Script

```kotlin
// For migrating existing local data to Firebase
class FirebaseDataMigration {
    suspend fun migrateLocalDataToFirebase() {
        val localUsers = getLocalUsers() // From local database
        
        localUsers.forEach { user ->
            // Upload user data to Firebase
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.userId)
                .set(user.toFirebaseMap())
                .await()
        }
    }
}
```
