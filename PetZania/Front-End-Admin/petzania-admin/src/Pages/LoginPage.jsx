import { memo, useContext} from 'react';
import {useAdminForm, FormField} from '../components/useAdminForm.jsx';
import { adminLogin , getAdminById } from '../services/services.js';
import { AdminContext } from '../context/AdminContext.jsx';
import { useNavigate } from 'react-router-dom';
const LoginPage = () => {
  const navigate = useNavigate();
  const { setAdmin, setIsLoggedIn , setRole } = useContext(AdminContext);
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useAdminForm(); 

  const onSubmit = async (formData) => {
    const { username, password } = formData;
    const response = await adminLogin(username, password);
    if (response.status === 200) {
      const {accessToken, refreshToken} = response.data.tokenDTO;
      
      setAdmin(username);
      setIsLoggedIn(true);
      const admin = await getAdminById(response.data.userId);
      setRole(admin.data.adminRole);
      navigate('/app');
    }
  }

  return (
    <div className={`mx-auto p-8 bg-white rounded-lg shadow-xl border border-gray-300 w-full max-w-md mt-[13%]`}>
      <h2 className="text-2xl font-bold mb-6 text-center">Login Page</h2>
      
      <form onSubmit={handleSubmit(onSubmit)}>

        <FormField 
          label="Username"
          id="username"
          placeholder="Enter username"
          register={register("username")}
          error={errors.username}
        />

        <FormField 
          label="Password"
          id="password"
          type="password"
          placeholder="Enter password"
          register={register("password")}
          error={errors.password}
        />

        <div className="flex items-center justify-center">
          <button
            type="submit"
            className="bg-primary hover:opacity-100 opacity-75 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline w-full"
          >
            Login
          </button>
        </div>
      </form>
    </div>
  );
};

export default memo(LoginPage);