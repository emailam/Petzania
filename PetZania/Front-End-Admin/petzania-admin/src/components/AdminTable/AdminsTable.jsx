import React from 'react';
import AdminTable from './useAdminTable';
import { useAllAdmins } from '../../services/queries';
import { Loader } from 'lucide-react';

const AdminsPage = () => {
  // Use the custom hook to fetch admin data
  const { data: admins, isLoading, error, refetch } = useAllAdmins();

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <Loader className="h-8 w-8 animate-spin text-primary" />
        <span className="ml-2">Loading admins...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-4 rounded-lg bg-red-50 text-red-700">
        <h3 className="font-medium">Error loading admins</h3>
        <p className="text-sm">{error.message || 'Unknown error occurred'}</p>
        <button 
          onClick={() => refetch()} 
          className="mt-2 px-3 py-1 bg-red-100 hover:bg-red-200 rounded-md text-sm"
        >
          Try Again
        </button>
      </div>
    );
  }

  // Pass both the refetch function and onAdminDeleted callback to AdminTable
  return (
    <div className="p-4">
      <AdminTable 
        users={admins} 
        refetch={refetch}
        onAdminDeleted={() => {
          console.log("Admin deleted, triggering refetch...");
          refetch();
        }} 
      />
    </div>
  );
};

export default AdminsPage;