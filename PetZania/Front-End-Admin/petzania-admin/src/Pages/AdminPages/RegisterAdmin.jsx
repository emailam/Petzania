import { memo , useContext } from 'react';
import {useAdminForm, FormField} from '../../components/useAdminForm.jsx';
import {AdminContext} from '../../context/AdminContext.jsx';
import { createAdmin } from '../../services/services.js';
const AdminAdminForm = () => {
  const {isLoggedIn , role} = useContext(AdminContext);
  if(!isLoggedIn) {
    return (<div className="text-center text-red-500">You are not logged in.</div>);
  }
  if(role !== 'SUPER_ADMIN') {
    return (<div className="text-center text-red-500">You do not have permission to access this page.</div>);
  }
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useAdminForm(); 

  const onSubmit = async (formData) => {
      const { username, password } = formData;
      const response = await createAdmin(username, password);
      console.log(response.data);
      console.log("ahahaha")
      if (response.status === 200) {
        alert("Admin created successfully!");

      }
    }

  return (
    <div className={`max-w-md mx-auto p-6 bg-white rounded-lg shadow-md`}>
      <h2 className="text-2xl font-bold mb-6 text-center">Add New Admin</h2>
      
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
            Register Admin
          </button>
        </div>
      </form>
    </div>
  );
};

export default memo(AdminAdminForm);