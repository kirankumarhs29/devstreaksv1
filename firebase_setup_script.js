// Firebase Firestore Setup Script
// Run this in Firebase Console > Firestore Database

// 1. Create initial collections with sample data
// Collection: users
{
  "userId": "sample_user_1",
  "name": "John Doe",
  "email": "john@example.com",
  "xp": 1250,
  "streak": 7,
  "level": 5,
  "createdAt": 1692000000000,
  "logicScore": 85
}

// Collection: user_stats (for leaderboard analytics)
{
  "userId": "sample_user_1",
  "xpGained": 50,
  "source": "challenge",
  "timestamp": 1692000000000,
  "date": "2024-08-14"
}

// Collection: friends (subcollection structure)
// Path: friends/{userId}/friends/{friendId}
{
  "addedAt": "2024-08-14T10:00:00Z"
}

// Collection: subscriptions
{
  "userId": "sample_user_1",
  "planId": "devstreak_monthly",
  "isActive": true,
  "startDate": 1692000000000,
  "endDate": 1694592000000,
  "isLifetime": false,
  "platform": "android"
}
