import React from 'react';
import { Trash2 } from 'lucide-react';

const CreateTableColumnAdmin = ({ handleDelete }) => {
  return [
    {
      header: "Username",
      accessorKey: "username",
      cell: ({ row }) => <span className="font-medium">{row.original.username}</span>
    },
    {
      header: "Actions",
      id: "actions",
      cell: ({ row }) =>{
        console.log("Row data:", row.original); // Debugging log
        return (
       
          <div className="flex items-center space-x-2">
            <button
              onClick={() => handleDelete(row.original.adminId, row.original.username)}
              className="text-red-600 hover:text-red-800 transition-colors flex items-center"
            >
              <Trash2 className="h-4 w-4 mr-1" />
              Delete
            </button>
          </div>
        )
      } 
    }
  ];
};

export default CreateTableColumnAdmin;