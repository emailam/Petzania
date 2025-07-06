import React, { useEffect } from 'react';
import { useAllUsers } from '../../services/queries';
import UseTable from './UseTable';
import { Loader, AlertCircle } from 'lucide-react';

export default function UsersTable() {
  const { data, isLoading, isError, error, refetch } = useAllUsers();
  
  // Format or transform the data if needed
  const users = React.useMemo(() => {
    if (!data) return [];
    
    // Check if data is an array
    if (Array.isArray(data)) {
      return data;
    }
    
    // Check if data has a 'users' property that is an array
    if (data?.users && Array.isArray(data.users)) {
      return data.users;
    }
    
    // Check if data has other potential array properties
    const possibleArrayProps = Object.keys(data || {}).filter(key => 
      Array.isArray(data[key]) && data[key].length > 0
    );
    
    if (possibleArrayProps.length > 0) {
      // Use the first array property found
      return data[possibleArrayProps[0]];
    }
    
    // If we couldn't find an array, log and return empty
    console.error("Could not find users array in data:", data);
    return [];
  }, [data]);

  useEffect(() => {
    console.log("Raw API data:", data);
    console.log("Processed users:", users);
  }, [data, users]);

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center p-8 bg-white rounded-lg shadow-md">
        <Loader className="h-12 w-12 text-primary animate-spin" />
        <p className="mt-4 text-gray-600">Loading users...</p>
      </div>
    );
  }
  
  if (isError) {
    return (
      <div className="flex flex-col items-center justify-center p-8 bg-white rounded-lg shadow-md">
        <AlertCircle className="h-12 w-12 text-red-500" />
        <p className="mt-4 text-gray-800 font-medium">Error loading users</p>
        <p className="text-red-600">{error?.message || 'Unknown error occurred'}</p>
        <button 
          onClick={() => refetch()} 
          className="mt-4 px-4 py-2 bg-primary text-white rounded-md hover:bg-primary-dark transition"
        >
          Try Again
        </button>
      </div>
    );
  }
  
  if (!users || users.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center p-8 bg-white rounded-lg shadow-md">
        <p className="text-gray-600">No users found</p>
        <p className="text-gray-500 mt-2">Data structure received: {JSON.stringify(data, null, 2).substring(0, 100)}...</p>
      </div>
    );
  }

  return (
    <div className="w-full">
      <UseTable users={users} />
    </div>
  );
}