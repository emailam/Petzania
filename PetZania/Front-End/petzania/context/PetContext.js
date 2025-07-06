import React, { createContext, useState } from 'react';

export const PetContext = createContext();

export const PetProvider = ({ children }) => {
    const initialPet = {
        name: "",
        species: "",
        breed: "",
        gender: "",
        age: "",
        dateOfBirth : "",
        description: "",
        myVaccinesURLs: [],
        myPicturesURLs: [],
    };

    const [pet, setPet] = useState(initialPet);
    const [pets, setPets] = useState([]);

    const createNewPet = () => {
        setPet(initialPet);
    };

    return (
        <PetContext.Provider value={{ pet, setPet, pets, setPets, createNewPet }}>
            {children}
        </PetContext.Provider>
    );
};
