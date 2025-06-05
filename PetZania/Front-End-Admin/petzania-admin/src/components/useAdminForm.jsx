import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import useSchema from './FormSchema';
import { memo } from 'react';
const FormField = memo(({ label, error, register, id, type = "text", placeholder }) => {
  return (
    <div className="mb-4">
      <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor={id}>
        {label}
      </label>
      <input
        type={type}
        id={id}
        className={`w-full px-3 py-2 border rounded-md ${error ? 'border-red-500' : 'border-gray-300'}`}
        placeholder={placeholder}
        {...register}
      />
      {error && (
        <p className="text-red-500 text-xs mt-1">{error.message}</p>
      )}
    </div>
  );
});
function useAdminForm () {
    const schema = useSchema();
    return useForm({
        resolver: yupResolver(schema),
        mode: "onBlur", // Validate on blur instead of onChange for better performance
        defaultValues: {
        username: "",
        password: "",
        },
        shouldUnregister: false, // Keep fields registered for better UX
    });
}
export { useAdminForm, FormField };