import { useEffect, useContext } from 'react';
import { useFriends } from '../context/FriendsContext';
import { UserContext } from '../context/UserContext';
import * as friendsService from '../services/friendsService';

// Custom hook to load initial friends data
export const useFriendsData = (userId) => {
    const {
        setFriendsCount,
        setFollowersCount,
        setFollowingCount,
        setBlockedCount,
    } = useFriends();

    useEffect(() => {
        if (userId) {
            loadAllCounts();
        }
    }, [userId]);
    const loadAllCounts = async () => {
        if (!userId) {
            console.log('No current user, skipping count loading');
            return;
        }

        try {
            const [
                friendsCountData,
                followersCountData,
                followingCountData,
                blockedCountData
            ] = await Promise.all([
                friendsService.getNumberOfFriendsByUserId(userId).catch(() => 0),
                friendsService.getNumberOfFollowersByUserId(userId).catch(() => 0),
                friendsService.getNumberOfFollowingByUserId(userId).catch(() => 0),
                friendsService.getNumberOfBlockedUsers().catch(() => 0)
            ]);

            // Update counts in context
            setFriendsCount(friendsCountData || 0);
            setFollowersCount(followersCountData || 0);
            setFollowingCount(followingCountData || 0);
            setBlockedCount(blockedCountData || 0);

        } catch (error) {
            console.error('Error loading friend counts:', error);
        }
    };

    return {
        loadAllCounts,
    };
};
