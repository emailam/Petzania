import { createColumnHelper } from "@tanstack/react-table";
import { Link } from "react-router-dom";
const columnHelper = createColumnHelper();

export default function CreateTableColumns() {
  return [
    columnHelper.accessor("username", {
      header: "Username",
      size: 200, // Fixed width
      cell: ({ row, getValue }) => {
        const username = getValue();
        const userId = row.original.userId || row.original.id; // Fallback to id if userId not present
        
        return (
          <Link
            to = {`/app/users/${userId}`}
            className="font-medium text-purple-600 hover:text-purple-900 focus:outline-none"
          >
            {username}
          </Link>
        );
      },
    }),
    columnHelper.accessor("email", {
      header: "Email",
      cell: (info) => info.getValue(),
    }),
   
    columnHelper.accessor("phoneNumber", {
      header: "Phone",
      size: 200, // Fixed width
      cell: (info) => info.getValue() || "N/A", // Handle missing phone numbers
    }),
    
    columnHelper.accessor("active", {  // Changed from isActive to active to match the property name in your user objects
      header: "Status",
      size: 150, // Fixed width
      // This is what the filter will compare against
      filterFn: (row, columnId, filterValue) => {
        // Convert boolean to string status first
        const status = row.getValue(columnId) === true ? "Active" : "In-Active";
        // Then compare with filter value
        return filterValue === "" || status === filterValue;
      },
      cell: ({ row }) => {
        // Handle both property names (active and isActive)
        const isActive = row.original.active ?? row.original.isActive ?? false;
        const status = isActive ? "active" : "inactive";
        const bgColor = isActive ? "bg-table-status-active" : "bg-table-status-inactive";
        
        return (
          <span className={`px-3 py-1 rounded-full text-sm font-medium ${bgColor} text-slate-800`}>
            {status}
          </span>
        );
      }
    }),
  ];
}