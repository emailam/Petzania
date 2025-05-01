import api from '@/api/axiosInstance';

export async function getAllPetsByUserId(userId) {
    try {
        const response = await api.get(`/user/${userId}/pets`);
        if (response.status !== 200) {
            throw new Error('Failed to fetch pets. Please try again later.');
        }
        return response.data;
    } catch (error) {
        console.error('Error fetching pets:', error.response?.data?.message || error.message);
        throw error;
    }
}

export async function addPetToUser(pet, userId) {
    try {
        const response = await api.post('/pet', {
            ...pet,
            userId: userId,
        });
        if (response.status !== 201) {
            throw new Error('Failed to add pet. Please try again later.');
        }
        return response.data;
    } catch (error) {
        alert('Error adding pet:', error.response?.data?.message || error.message);
    }
}

export async function getPetById(petId) {
    try {
        const response = await api.get(`/pet/${petId}`);
        if (response.status !== 200) {
            throw new Error('Failed to fetch pet. Please try again later.');
        }
        return response.data;
    } catch (error) {
        console.error('Error fetching pet:', error.response?.data?.message || error.message);
        throw error;
    }
}

export async function deletePet(petId) {
    try {
        const response = await api.delete(`/pet/${petId}`);
        if (response.status !== 204) {
            throw new Error('Failed to delete pet. Please try again later.');
        }
    } catch (error) {
        console.error('Error deleting pet:', error.response?.data?.message || error.message);
        throw error;
    }
}

export async function updatePet(petId, petData) {
    console.log('Updating pet with ID:', petId, 'and data:', petData);
    try {
        const response = await api.patch(`/pet/${petId}`, petData);
        if (response.status !== 200) {
            throw new Error('Failed to update pet. Please try again later.');
        }
        return response.data;
    } catch (error) {
        console.error('Error updating pet:', error.response?.data?.message || error.message);
        throw error;
    }
}