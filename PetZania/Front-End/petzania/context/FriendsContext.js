import React, { createContext, useState, useContext } from 'react';

export const FriendsContext = createContext();

export const FriendsProvider = ({ children }) => {
    const [friendsCount, setFriendsCount] = useState(0);
    const [followersCount, setFollowersCount] = useState(0);
    const [followingCount, setFollowingCount] = useState(0);
    const [blockedCount, setBlockedCount] = useState(0);

    const [friends, setFriends] = useState([]);
    const [followers, setFollowers] = useState([]);
    const [following, setFollowing] = useState([]);
    const [blockedUsers, setBlockedUsers] = useState([]);

    const value = {
        friendsCount,
        setFriendsCount,
        followersCount,
        setFollowersCount,
        followingCount,
        setFollowingCount,
        friends,
        setFriends,
        followers,
        setFollowers,
        following,
        setFollowing,
        blockedUsers,
        setBlockedUsers,
        blockedCount,
        setBlockedCount,
    }

    return (
        <FriendsContext.Provider value={value}>
            {children}
        </FriendsContext.Provider>
    );
};

export const useFriends = () => {
    const context = useContext(FriendsContext);
    if (!context) {
        throw new Error('useFriends must be used within a FriendsProvider');
    }
    return context;
};
