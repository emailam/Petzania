import api from './api'; 
import { saveToken, clearAllTokens, getToken } from './tokenStorage';

export const adminLogin = async (username, password) => {
  try {
    const response = await api.post('/admin/login', {
      username,
      password
    });
    const { accessToken, refreshToken } = response.data.tokenDTO;
    await saveToken('accessToken', accessToken);
    await saveToken('refreshToken', refreshToken);
    return response;
  } catch (error) {
    throw error.response ? error.response.data : error.message;
  }
};

export const adminLogout = async (username) => {
  try {
    const refreshToken = await getToken('refreshToken');
    const response = await api.post('/admin/logout', {
      username,
      refreshToken
    });
    return response;
  } catch (error) {
    throw error.response ? error.response.data : error.message;
  }
};

export const getAdminById = async (adminId) => {
  try {
    const response = await api.get(`/admin/${adminId}`);
    return response;
  } catch (error) {
    throw error.response ? error.response.data : error.message;
  }
};

// NEW: Get All Admins
export const getAllAdmins = async () => {
  try {
    const response = await api.get('/admin/getAll');
    return response;
  } catch (error) {
    throw error.response ? error.response.data : error.message;
  }
};

// Create Admin (requires authentication)
export const createAdmin = async (username, password) => {
  try {
    const response = await api.post('/admin/create', {
      username,
      password,
      adminRole: "ADMIN"
    });
    return response;
  } catch (error) {
    throw error.response ? error.response.data : error.message;
  }
};

// Delete Admin (requires authentication)
export const deleteAdmin = async (adminId) => {
  try {
    const response = await api.delete(`/admin/delete/${adminId}`);
    return response;
  } catch (error) {
    throw error.response ? error.response.data : error.message;
  }
};

// Edit User Status (requires authentication)
export const editUserStatus = async (userId, newStatus) => {
  try {
    const token = await getToken('accessToken');
    const response = await api.patch(
      `/user/auth/${userId}/status`,
      { status: newStatus },
      {
        headers: {
          Authorization: `Bearer ${token}`
        }
      }
    );
    return response;
  } catch (error) {
    throw error.response ? error.response.data : error.message;
  }
};

export const getUserById = async (userId) => {
  try {
    const response = await api.get(`/user/auth/${userId}`);
    return response;
  } catch (error) {
    throw error.response ? error.response.data : error.message;
  }
};

export const getAllUsers = async () => {
  try {
    const response = await api.get('/user/auth/users');
    return response;
  } catch (error) {
    throw error.response ? error.response.data : error.message;
  }
};