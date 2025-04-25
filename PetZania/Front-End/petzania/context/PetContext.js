import React, { createContext, useState } from 'react';

export const PetContext = createContext();

export const PetProvider = ({ children }) => {
    const initialPet = {
        name: "",
        type: "",
        breed: "",
        gender: "",
        age: "",
        color: "",
        description: "",
        healthCondition: "",
        vaccines: [],
        image: null,
    };

    const [pet, setPet] = useState(initialPet);

    const [pets, setPets] = useState([]);

    const addPet = (newPet) => {
        if(newPet.name === "") {
            return;
        }
        setPets((prevPets) => {
            const petIndex = prevPets.findIndex(p => p.name === newPet.name);

            if (petIndex !== -1) {
                const updatedPets = [...prevPets];
                updatedPets[petIndex] = newPet;
                return updatedPets;
            } else {
                return [...prevPets, newPet];
            }
        });
    };

    const deletePet = (petName) => {
        setPets((prevPets) => {
            return prevPets.filter(p => p.name !== petName);
        });
    }

    const createNewPet = () => {
        setPet(initialPet);
    };

    return (
        <PetContext.Provider value={{ pet, setPet, pets, setPets, addPet, createNewPet, deletePet }}>
            {children}
        </PetContext.Provider>
    );
};
