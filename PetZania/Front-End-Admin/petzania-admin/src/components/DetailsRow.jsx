import React from "react";

const DetailsRow = ({ icon, label, value, className }) => {
  // Helper function to render values based on their type
  const renderValue = (label, value) => {
    if (label === "Active Status") {
      return (
        <span className={`px-2 py-1 rounded-full text-xs font-medium ${value ? "bg-green-100 text-green-800" : "bg-red-100 text-red-800"}`}>
          {value ? "Active" : "Inactive"}
        </span>
      );
    } else if (label === "Roles" && Array.isArray(value)) {
      return (
        <div className="flex gap-1 flex-wrap">
          {value.length > 0 ? value.map((role, index) => (
            <span key={index} className="px-2 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
              {role}
            </span>
          )) : <span>No roles assigned</span>}
        </div>
      );
    } else if (label === "User ID") {
      return <span className="text-gray-600 font-mono text-sm">{value}</span>;
    } else if (label === "Bio") {
      return <p className="text-gray-700 italic">{value}</p>;
    } else if (["Followers", "Following", "Friends", "Login Times"].includes(label)) {
      return <span className="font-semibold">{value.toLocaleString()}</span>;
    }
    return <span className="text-gray-900">{value}</span>;
  };

  return (
    <div className={`flex items-start p-3 border rounded-lg hover:bg-gray-50 transition-colors w-full ${className || ""}`}>
      <div className="flex-shrink-0 mr-3 mt-1">
        {icon}
      </div>
      <div className="flex-1">
        <p className="text-sm font-medium text-gray-500">{label}</p>
        <div className="mt-1 text-left">
          {renderValue(label, value)}
        </div>
      </div>
    </div>
  );
};

export default DetailsRow;