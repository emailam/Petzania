import { useEffect } from 'react';
import { useFriends } from '../context/FriendsContext';
import * as friendsService from '../services/friendsService';

// Custom hook to load initial friends data
export const useFriendsData = () => {
    const {
        setFriendsCount,
        setFollowersCount,
        setFollowingCount,
        setBlockedCount,
    } = useFriends();

    useEffect(() => {
        loadAllCounts();
    }, []);

    const loadAllCounts = async () => {
        try {
            const [
                friendsCountData,
                followersCountData,
                followingCountData,
                blockedCountData
            ] = await Promise.all([
                friendsService.getNumberOfFriends().catch(() => 0),
                friendsService.getNumberOfFollowers().catch(() => 0),
                friendsService.getNumberOfFollowing().catch(() => 0),
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
