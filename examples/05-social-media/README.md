# Social Media Platform Example

This example creates a social media platform with users, posts, comments, and follows.

## What it generates

- **8 users** with profiles and social stats
- **20 posts** with hashtags and engagement metrics
- **35 comments** on posts
- **25 follow relationships** between users

## Key features demonstrated

- **Complex social relationships** (many-to-many follows)
- **Dynamic arrays** with variable lengths (hashtags)
- **Engagement metrics** (likes, shares, follower counts)
- **Probability-based generation** (10% verified users, 80% public posts)
- **Internet generators** for usernames and avatars

## Data model

```
Users (8) ← Posts (20) ← Comments (35)
   ↑↓           ↓
Follows      Hashtags[]
  (25)
```

## Use cases

- Social media app testing
- Content moderation system testing
- Analytics dashboard development
- Feed algorithm testing
- User engagement analysis
