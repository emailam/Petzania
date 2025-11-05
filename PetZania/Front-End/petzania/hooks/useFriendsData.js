import { useEffect } from 'react';
import * as friendsService from '../services/friendsService';

// Custom hook to load initial friends data
export const useFriendsData = (userId) => {
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
            ] = await Promise.all([
                friendsService.getNumberOfFriendsByUserId(userId).catch(() => 0),
                friendsService.getNumberOfFollowersByUserId(userId).catch(() => 0),
                friendsService.getNumberOfFollowingByUserId(userId).catch(() => 0),
            ]);

            // Return the counts instead of setting them in global state
            return {
                friendsCount: friendsCountData || 0,
                followersCount: followersCountData || 0,
                followingCount: followingCountData || 0,
            };

        } catch (error) {
            console.error('Error loading friend counts:', error);
            // Return default values on error
            return {
                friendsCount: 0,
                followersCount: 0,
                followingCount: 0,
            };
        }
    };

    return {
        loadAllCounts,
    };
};
