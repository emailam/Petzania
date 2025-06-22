import { useEffect, useContext } from 'react';
import { useFriends } from '../context/FriendsContext';
import { UserContext } from '../context/UserContext';
import * as friendsService from '../services/friendsService';

// Custom hook to load initial friends data
export const useFriendsData = () => {
    const {
        setFriendsCount,
        setFollowersCount,
        setFollowingCount,
        setBlockedCount,
    } = useFriends();

    const { user: currentUser } = useContext(UserContext);

    useEffect(() => {
        if (currentUser?.userId) {
            loadAllCounts();
        }
    }, [currentUser?.userId]);
    const loadAllCounts = async () => {
        if (!currentUser?.userId) {
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
                friendsService.getNumberOfFriendsByUserId(currentUser.userId).catch(() => 0),
                friendsService.getNumberOfFollowersByUserId(currentUser.userId).catch(() => 0),
                friendsService.getNumberOfFollowingByUserId(currentUser.userId).catch(() => 0),
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
