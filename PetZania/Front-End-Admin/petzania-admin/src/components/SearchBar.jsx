import { useState } from 'react';
import { Search } from 'lucide-react';

const SearchBar = ({ placeholder = "Type to Search" }) => {
  const [searchTerm, setSearchTerm] = useState('');

  // Empty search function - to be implemented later
  const handleSearch = () => {
    // Your search implementation will go here
    console.log("Searching for:", searchTerm);
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  return (
    <div className="flex w-auto max-w-lg">
      <div className="relative flex-grow">
        <div className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400">
          {searchTerm.length === 0 && (
            <Search size={18} className="text-gray-400" />
          )}
        </div>
        <input
          type="text"
          className="w-full pl-10 pr-4 py-2 border border-gray-400  rounded-md focus:outline-none focus:ring-1 "
          placeholder={placeholder}
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          onKeyDown={handleKeyDown}
        />
        
      </div>
      <button
        onClick={handleSearch}
        className="px-4 py-2 bg-table text-white rounded-lg hover:bg-primary focus:outline-none ml-2"
      >
        Search
      </button>
    </div>
  );
};

export default SearchBar;