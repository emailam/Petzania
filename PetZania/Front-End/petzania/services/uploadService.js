import api from '@/api/axiosInstance';

export async function uploadFile(file) {
    const formData = new FormData();
    formData.append('file', file);

    try {
        const response = await api.post('/cloud/file', formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        });

        if (response.status !== 200) {
            throw new Error('Failed to upload file. Please try again later.');
        }

        return response.data;
    } catch (error) {
        console.error('Error uploading file:', error.response?.data?.message || error.message);
        throw error;
    }
}

export async function uploadFiles(files) {
    const formData = new FormData();

    files.forEach((file) => {
        formData.append('files', {
            uri: file.uri,
            name: file.name,
            type: file.type,
        });
    });

    try {
        const response = await api.post('/cloud/files', formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        });

        if (response.status !== 200) {
            throw new Error('Failed to upload files. Please try again later.');
        }

        return response.data;
    } catch (error) {
        console.error('Error uploading files:', error.response?.data?.message || error.message);
        throw error;
    }
}