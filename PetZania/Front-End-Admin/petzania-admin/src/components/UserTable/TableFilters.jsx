import { useState, useRef, useEffect } from 'react';
import { Filter } from 'lucide-react';

const TableFilters = ({
  statusOptions,
  statusFilter,
  onStatusChange,
  onReset,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef(null);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleStatusChange = (status) => {
    onStatusChange(status);
    setIsOpen(false);
  };

  const handleReset = () => {
    onReset();
    setIsOpen(false);
  };

  return (
    <div ref={dropdownRef} className="relative ml-auto">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className={`flex items-center space-x-1 p-2 rounded-md ${
          statusFilter ? 'bg-indigo-100 text-primary' : 'bg-white hover:bg-gray-100'
        } border border-gray-200 shadow-sm`}
        aria-expanded={isOpen}
        aria-haspopup="true"
      >
        <Filter size={20} />
        <span className="ml-1">
          {statusFilter ? statusFilter : 'Filter'}
        </span>
      </button>
      {isOpen && (
        <div className="absolute right-0 mt-2 bg-white border border-gray-200 rounded-md shadow-lg z-10 w-40">
          <div className="py-2">
            <button
              onClick={() => handleStatusChange('')}
              className={`block w-full text-left px-4 py-2 text-sm ${
                statusFilter === ''
                  ? 'bg-indigo-100 text-primary'
                  : 'text-gray-700 hover:bg-gray-100'
              }`}
            >
              All Statuses
            </button>
            {statusOptions.map((status) => (
              <button
                key={status}
                onClick={() => handleStatusChange(status)}
                className={`block w-full text-left px-4 py-2 text-sm ${
                  statusFilter === status
                    ? 'bg-indigo-100 text-primary'
                    : 'text-gray-700 hover:bg-gray-100'
                }`}
              >
                {status}
              </button>
            ))}
          </div>
          {statusFilter && (
            <div className="border-t border-gray-200">
              <button
                onClick={handleReset}
                className="block w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-gray-100"
              >
                Clear Filters
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default TableFilters;