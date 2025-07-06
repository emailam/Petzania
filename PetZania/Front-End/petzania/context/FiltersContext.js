// src/contexts/FiltersContext.jsx
import { createContext, useState, useContext } from 'react';

const FiltersContext = createContext();
export const useFilters = () => useContext(FiltersContext);

export function FiltersProvider({ children }) {
  const [filters, setFilters] = useState({
    category: 'All',
    breedFilter: '',
    genderFilter: 'All',
    ageMin: '',
    ageMax: '',
    sortBy: 'date',
    sortOrder: 'desc',
  });

  const resetFilters = () => {
    setFilters({
      category: 'All',
      breedFilter: '',
      genderFilter: 'All',
      ageMin: '',
      ageMax: '',
      sortBy: 'date',
      sortOrder: 'desc',
    });
  };

  return (
    <FiltersContext.Provider value={{ filters, setFilters, resetFilters }}>
      {children}
    </FiltersContext.Provider>
  );
}
