import React, { useState, useEffect } from "react";
import {
  useReactTable,
  getCoreRowModel,
  getPaginationRowModel,
  flexRender,
} from "@tanstack/react-table";
import { ChevronRight, ChevronLeft, Plus } from 'lucide-react';
import { Link } from "react-router-dom";
import CreateTableColumnAdmin from "./CreateTableColumnAdmin";
import { deleteAdmin } from "../../services/services";
import DeleteConfirmationModal from "./DeleteConfirmationModal";

const AdminTable = ({ users, onAdminDeleted, refetch }) => {
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [adminToDelete, setAdminToDelete] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);
  
  // Handle showing the delete confirmation modal
  const handleDeleteClick = (adminId, username) => {
    console.log("Delete clicked for admin:", adminId, username); // Debugging log
    setAdminToDelete({ adminId, username });
    setIsDeleteModalOpen(true);
  };
  
  // Handle the actual delete action after confirmation
  const handleDelete = async () => {
    if (!adminToDelete || isDeleting) return;
    
    try {
      setIsDeleting(true);
      console.log("Deleting admin with ID:", adminToDelete.adminId); // Debug log
      
      // Call the API to delete the admin
      const response = await deleteAdmin(adminToDelete.adminId);
      
      if (response && response.success) {
        // Close the modal first
        setIsDeleteModalOpen(false);
        setAdminToDelete(null);
        
        // Important: Call both refetch and onAdminDeleted
        if (refetch && typeof refetch === 'function') {
          await refetch(); // Refetch the data directly
          console.log("Data refetched after deletion");
        }
        
        // Also notify parent component (for backward compatibility)
        if (onAdminDeleted && typeof onAdminDeleted === 'function') {
          onAdminDeleted();
        }
      } else {
        // Handle error case
        console.error("Failed to delete admin:", response?.message || "Unknown error");
        alert("Failed to delete admin. Please try again.");
      }
    } catch (error) {
      console.error("Error deleting admin:", error);
      alert("An error occurred while deleting the admin.");
    } finally {
      setIsDeleting(false);
    }
  };

  // Create columns
  const columns = CreateTableColumnAdmin({ handleDelete: handleDeleteClick });

  const table = useReactTable({
    data: users || [], 
    columns: columns,
    getCoreRowModel: getCoreRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    autoResetPageIndex: false,
  });

  // Reset to first page when data changes
  useEffect(() => {
    if (table) {
      table.resetPageIndex(true);
    }
  }, [users?.length]);

  return (
    <div className="w-full">
      {/* Delete Confirmation Modal */}
      <DeleteConfirmationModal 
        isOpen={isDeleteModalOpen}
        onClose={() => !isDeleting && setIsDeleteModalOpen(false)}
        onConfirm={handleDelete}
        adminUsername={adminToDelete?.username}
        isDeleting={isDeleting}
      />
      
      {/* Header with title and add button */}
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-semibold text-gray-800">Admin Management</h2>
        <Link to="/app/admins/register" className="flex items-center px-4 py-2 bg-primary opacity-70 text-white rounded-md hover:opacity-100 transition-colors">
          <Plus className="h-4 w-4 mr-2" />
          Add New Admin
        </Link>
      </div>

      {/* Table */}
      <div className="overflow-hidden rounded-lg shadow-lg">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-table">
            {table.getHeaderGroups().map(headerGroup => (
              <tr key={headerGroup.id}>
                {headerGroup.headers.map(header => (
                  <th
                    key={header.id}
                    scope="col"
                    className="px-6 py-3 text-left text-sm font-medium text-gray-700"
                  >
                    {flexRender(header.column.columnDef.header, header.getContext())}
                  </th>
                ))}
              </tr>
            ))}
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {table.getRowModel().rows.map(row => (
              <tr key={row.id} className="hover:bg-gray-50">
                {row.getVisibleCells().map(cell => (
                  <td
                    key={cell.id}
                    className="px-6 py-4 whitespace-nowrap text-sm text-gray-700"
                  >
                    {flexRender(cell.column.columnDef.cell, cell.getContext())}
                  </td>
                ))}
              </tr>
            ))}
            {table.getRowModel().rows.length === 0 && (
              <tr>
                <td
                  colSpan={columns.length}
                  className="px-6 py-4 text-center text-sm text-gray-500"
                >
                  No results found
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination Footer */}
      <div className="bg-table px-4 py-3 flex items-center justify-between border-t border-gray-200 sm:px-6 rounded-b-lg">
        <div className="flex-1 flex justify-between sm:hidden">
          <button
            onClick={() => table.previousPage()}
            disabled={!table.getCanPreviousPage()}
            className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50"
          >
            Previous
          </button>
          <button
            onClick={() => table.nextPage()}
            disabled={!table.getCanNextPage()}
            className="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50"
          >
            Next
          </button>
        </div>
        <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-gray-700">
              Showing{' '}
              <span className="font-medium">
                {table.getState().pagination.pageIndex * table.getState().pagination.pageSize + 1}
              </span>{' '}-{' '}
              <span className="font-medium">
                {Math.min(
                  (table.getState().pagination.pageIndex + 1) * table.getState().pagination.pageSize,
                  table.getPreFilteredRowModel().rows.length
                )}
              </span>{' '}of{' '}
              <span className="font-medium">{table.getPreFilteredRowModel().rows.length}</span> results
            </p>
          </div>
          <div>
            <div className="flex items-center">
              <span className="mr-2 text-sm text-gray-700">Rows per page:</span>
              <select
                value={table.getState().pagination.pageSize}
                onChange={e => table.setPageSize(Number(e.target.value))}
                className="relative inline-flex items-center px-2 py-1 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50"
              >
                {[10, 20, 30, 40, 50].map(size => (
                  <option key={size} value={size}>
                    {size}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div>
            <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px" aria-label="Pagination">
              <button
                onClick={() => table.previousPage()}
                disabled={!table.getCanPreviousPage()}
                className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50"
              >
                <span className="sr-only">Previous</span>
                <ChevronLeft className="h-5 w-5" aria-hidden="true" />
              </button>
              <span className="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700">
                {table.getState().pagination.pageIndex + 1} / {table.getPageCount()}
              </span>
              <button
                onClick={() => table.nextPage()}
                disabled={!table.getCanNextPage()}
                className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50"
              >
                <span className="sr-only">Next</span>
                <ChevronRight className="h-5 w-5" aria-hidden="true" />
              </button>
            </nav>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdminTable;